package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.CustomStretchEntity
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.data.repository.toGuide
import com.example.diettracker.domain.ExerciseLibrary
import com.example.diettracker.domain.StretchGuide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StretchLibraryUiState(
    val groups: List<Pair<String, List<StretchGuide>>> = emptyList(),
    val customNames: Set<String> = emptySet(),
    val loading: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

/**
 * 库 → 运动 → 拉伸 的后端。
 *
 * v7 把「动作库」从库里移除了：现在动作是撸铁的一部分，只在「加动作」时按名字
 * 检索，不再有独立的动作库页面，所以这里只剩拉伸。
 */
class LibraryViewModel(
    private val repository: ActivityRepository
) : ViewModel() {

    private val _stretchState = MutableStateFlow(StretchLibraryUiState())
    val stretchState: StateFlow<StretchLibraryUiState> = _stretchState.asStateFlow()

    init {
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

    /** 内置拉伸 + 自建拉伸，按目标肌群分组。 */
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

    /** 只有自建拉伸能删；内置的给出提示而不是静默失败。 */
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
        _stretchState.update { it.copy(message = null, error = null) }
    }

    companion object {
        fun factory(repository: ActivityRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LibraryViewModel(repository) as T
            }
    }
}
