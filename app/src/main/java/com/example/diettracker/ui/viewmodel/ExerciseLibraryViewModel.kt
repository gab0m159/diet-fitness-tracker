package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.CustomExerciseEntity
import com.example.diettracker.data.model.BodyPart
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.domain.ExerciseInfo
import com.example.diettracker.domain.ExerciseLibrary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 动作库里的一行：内置或自建。
 *
 * 没有「步进」「目标重量」这些字段——v7 里它们已经不存在了：动作就是
 * `名字 + 部位 + 肌群 + 要点`，重量 / 组数 / 次数属于当天撸铁的记录。
 */
data class ExerciseRow(
    /** 自建动作的数据库 id；内置动作为 0。 */
    val id: Long = 0L,
    val name: String,
    val bodyPart: BodyPart,
    val primaryMuscle: String,
    val cue: String,
    val isCustom: Boolean,
    /** 关联的拉伸名，便于从动作跳去看拉伸。 */
    val stretchNames: List<String> = emptyList(),
    /** 内置动作的原始信息，自建动作为 null。 */
    val info: ExerciseInfo? = null
)

data class ExerciseLibraryUiState(
    val builtInGroups: List<Pair<BodyPart, List<ExerciseRow>>> = emptyList(),
    val customRows: List<ExerciseRow> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null,
    val error: String? = null
) {
    val totalCount: Int
        get() = builtInGroups.sumOf { it.second.size } + customRows.size
}

/**
 * 「库 → 运动 → 撸铁」背后的动作库。
 *
 * 只负责动作本身的增删改查，不碰任何训练记录。
 */
class ExerciseLibraryViewModel(
    private val repository: ActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseLibraryUiState())
    val uiState: StateFlow<ExerciseLibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCustomExercises().collect { custom ->
                _uiState.update { state ->
                    state.copy(
                        builtInGroups = buildBuiltInGroups(),
                        customRows = custom.map { it.toRow() },
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
                    isCustom = false,
                    stretchNames = info.stretches.map { it.name },
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
            isCustom = true,
            stretchNames = stretchList
        )

    fun saveCustomExercise(entity: CustomExerciseEntity) {
        viewModelScope.launch {
            repository.saveCustomExercise(entity).fold(
                onSuccess = { _uiState.update { it.copy(message = "已保存动作") } },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "保存失败") }
                }
            )
        }
    }

    fun deleteCustomExercise(entity: CustomExerciseEntity) {
        viewModelScope.launch {
            repository.deleteCustomExercise(entity).fold(
                onSuccess = { _uiState.update { it.copy(message = "已删除动作") } },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "删除失败") }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    companion object {
        fun factory(repository: ActivityRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ExerciseLibraryViewModel(repository) as T
            }
    }
}
