package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.CustomExerciseEntity
import com.example.diettracker.data.db.CustomStretchEntity
import com.example.diettracker.data.model.BodyPart
import com.example.diettracker.data.repository.TrainingRepository
import com.example.diettracker.data.repository.toGuide
import com.example.diettracker.domain.ExerciseInfo
import com.example.diettracker.domain.ExerciseLibrary
import com.example.diettracker.domain.StretchGuide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One row in the exercise library: built-in or user-created.
 *
 * v6 去掉了「目标重量」和「每动作步进」两个字段：步进和目标重量现在属于
 * **具体的训练日**（`TrainingDayExerciseEntity`），动作库里没有全局值可显示。
 */
data class ExerciseRow(
    /** 自建动作的数据库 id；内置动作为 0。编辑 / 删除要靠它定位到具体行。 */
    val id: Long = 0L,
    val name: String,
    val bodyPart: BodyPart,
    val primaryMuscle: String,
    val cue: String,
    val source: String,
    val stretchNames: List<String>,
    val isCustom: Boolean,
    /** Built-in info, absent for custom exercises. */
    val info: ExerciseInfo? = null
)

data class ExerciseLibraryUiState(
    val builtInGroups: List<Pair<BodyPart, List<ExerciseRow>>> = emptyList(),
    val customRows: List<ExerciseRow> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

data class StretchLibraryUiState(
    val groups: List<Pair<String, List<StretchGuide>>> = emptyList(),
    val customNames: Set<String> = emptySet(),
    val loading: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

/**
 * Backs the "库" tab: the exercise library and the stretch library.
 *
 * Both merge built-in content with whatever the user created, so the UI never has
 * to know which is which except to decide whether an entry is editable.
 */
class LibraryViewModel(
    private val repository: TrainingRepository
) : ViewModel() {

    private val _exerciseState = MutableStateFlow(ExerciseLibraryUiState())
    val exerciseState: StateFlow<ExerciseLibraryUiState> = _exerciseState.asStateFlow()

    private val _stretchState = MutableStateFlow(StretchLibraryUiState())
    val stretchState: StateFlow<StretchLibraryUiState> = _stretchState.asStateFlow()

    /** Exercise names used by the training-day editor and the add-to-today picker. */
    val allExerciseNames: StateFlow<List<String>> =
        repository.observeCustomExercises()
            .let { customFlow ->
                combine(customFlow, MutableStateFlow(Unit)) { custom, _ ->
                    (ExerciseLibrary.allNames() + custom.map { it.name }).distinct().sorted()
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ExerciseLibrary.allNames()
            )

    init {
        viewModelScope.launch {
            repository.observeCustomExercises().collect { custom ->
                _exerciseState.update { state ->
                    state.copy(
                        builtInGroups = buildBuiltInGroups(),
                        customRows = custom.map { it.toRow() },
                        loading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(
                repository.observeCustomStretches(),
                repository.observeCustomExercises()
            ) { stretches, _ -> stretches }
                .collect { custom ->
                    val customGuides = custom.map { it.toGuide() }
                    val groups = buildStretchGroups(customGuides)
                    _stretchState.update { state ->
                        state.copy(
                            groups = groups,
                            customNames = customGuides.map { it.name }.toSet(),
                            loading = false
                        )
                    }
                }
        }
    }

    private fun buildBuiltInGroups(): List<Pair<BodyPart, List<ExerciseRow>>> =
        ExerciseLibrary.groupedByBodyPart().map { (part, list) ->
            part to list.map { info ->
                ExerciseRow(
                    name = info.name,
                    bodyPart = part,
                    primaryMuscle = info.primaryMuscle,
                    cue = info.cue,
                    source = info.source,
                    stretchNames = info.stretches.map { it.name },
                    isCustom = false,
                    info = info
                )
            }
        }

    private fun CustomExerciseEntity.toRow(): ExerciseRow =
        ExerciseRow(
            id = id,
            name = name,
            bodyPart = BodyPart.fromStorage(bodyPart),
            primaryMuscle = primaryMuscle,
            cue = cue,
            source = "用户自建",
            stretchNames = stretchList,
            isCustom = true
        )

    /**
     * Merges built-in stretches with custom ones, then groups by target muscle so
     * the whole body is covered in one screen.
     */
    private fun buildStretchGroups(
        custom: List<StretchGuide>
    ): List<Pair<String, List<StretchGuide>>> {
        val builtIn = ExerciseLibrary.allStretchGroups()
        if (custom.isEmpty()) return builtIn

        val merged = LinkedHashMap<String, MutableList<StretchGuide>>()
        builtIn.forEach { (muscle, list) ->
            merged.getOrPut(muscle) { mutableListOf() }.addAll(list)
        }
        custom.forEach { guide ->
            val key = merged.keys.firstOrNull { group ->
                guide.targetMuscle.contains(group) || group.contains(guide.targetMuscle)
            } ?: guide.targetMuscle.ifBlank { "自建拉伸" }
            merged.getOrPut(key) { mutableListOf() }.add(guide)
        }
        return merged.map { (muscle, list) -> muscle to list.distinctBy { it.name } }
    }

    // ------------------------------------------------------------- writes

    fun saveCustomExercise(entity: CustomExerciseEntity) {
        viewModelScope.launch {
            repository.saveCustomExercise(entity).fold(
                onSuccess = { _exerciseState.update { it.copy(message = "已保存动作") } },
                onFailure = { e ->
                    _exerciseState.update { it.copy(error = e.message ?: "保存失败") }
                }
            )
        }
    }

    fun deleteCustomExercise(entity: CustomExerciseEntity) {
        viewModelScope.launch {
            repository.deleteCustomExercise(entity).fold(
                onSuccess = { _exerciseState.update { it.copy(message = "已删除动作") } },
                onFailure = { e ->
                    _exerciseState.update { it.copy(error = e.message ?: "删除失败") }
                }
            )
        }
    }

    fun saveStretch(entity: CustomStretchEntity) {
        viewModelScope.launch {
            repository.saveCustomStretch(entity).fold(
                onSuccess = { _stretchState.update { it.copy(message = "已保存拉伸") } },
                onFailure = { e ->
                    _stretchState.update { it.copy(error = e.message ?: "保存失败") }
                }
            )
        }
    }

    /**
     * Deletes a stretch. Only custom entries can be removed; deleting a built-in
     * one is a no-op with an explanatory message.
     */
    fun deleteStretch(name: String) {
        viewModelScope.launch {
            val custom = repository.getCustomStretches().firstOrNull { it.name == name }
            if (custom == null) {
                _stretchState.update { it.copy(message = "内置拉伸无法删除") }
                return@launch
            }
            repository.deleteCustomStretch(custom).fold(
                onSuccess = { _stretchState.update { it.copy(message = "已删除拉伸") } },
                onFailure = { e ->
                    _stretchState.update { it.copy(error = e.message ?: "删除失败") }
                }
            )
        }
    }

    fun clearMessages() {
        _exerciseState.update { it.copy(message = null, error = null) }
        _stretchState.update { it.copy(message = null, error = null) }
    }

    /** Stretches linked to one exercise, for the "拉伸 ↗" jump. */
    fun stretchNamesFor(exerciseName: String): List<String> =
        ExerciseLibrary.find(exerciseName)?.stretches?.map { it.name }.orEmpty()

    companion object {
        fun factory(repository: TrainingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LibraryViewModel(repository) as T
            }
    }
}
