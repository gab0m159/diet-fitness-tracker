package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.PersonalRecordEntity
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 同一个动作的一组 PR：当前最好的那条 + 全部历史。 */
data class RecordGroup(
    val exerciseName: String,
    /** 当前最好成绩：重量优先，同重量比次数。 */
    val best: PersonalRecordEntity,
    /** 全部历史，重量从高到低。 */
    val history: List<PersonalRecordEntity>
)

data class PersonalRecordUiState(
    val groups: List<RecordGroup> = emptyList(),
    val exerciseNames: List<String> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null
) {
    val isEmpty: Boolean get() = groups.isEmpty()
}

/**
 * 「我的 PR」。
 *
 * 同一动作可以有很多条历史，列表里只显示当前最好的那条，展开看全部。
 */
class PersonalRecordViewModel(
    private val repository: ActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalRecordUiState())
    val uiState: StateFlow<PersonalRecordUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeRecords().collect { records ->
                _uiState.update { state ->
                    state.copy(groups = groupRecords(records), loading = false)
                }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(exerciseNames = repository.allExerciseNames()) }
        }
        viewModelScope.launch {
            repository.observeCustomExercises().collect {
                _uiState.update { state -> state.copy(exerciseNames = repository.allExerciseNames()) }
            }
        }
    }

    /**
     * 按动作分组：每组取重量最大的那条作为当前 PR（同重量取次数多的），
     * 组内历史按重量降序。
     */
    private fun groupRecords(records: List<PersonalRecordEntity>): List<RecordGroup> =
        records.groupBy { it.exerciseName }
            .map { (name, list) ->
                val sorted = list.sortedWith(
                    compareByDescending<PersonalRecordEntity> { it.weightKg }
                        .thenByDescending { it.reps }
                        .thenByDescending { it.date }
                )
                RecordGroup(exerciseName = name, best = sorted.first(), history = sorted)
            }
            // 列表整体按当前最佳重量降序，最重的排最上面
            .sortedByDescending { it.best.weightKg }

    fun addRecord(exerciseName: String, weightKg: Double, reps: Int, date: String) {
        viewModelScope.launch {
            repository.addRecord(exerciseName, weightKg, reps, date).fold(
                onSuccess = {
                    _uiState.update { it.copy(message = "已记录 $exerciseName") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(message = e.message ?: "保存失败") }
                }
            )
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteRecord(id).fold(
                onSuccess = { _uiState.update { it.copy(message = "已删除") } },
                onFailure = { e ->
                    _uiState.update { it.copy(message = e.message ?: "删除失败") }
                }
            )
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    companion object {
        fun factory(repository: ActivityRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PersonalRecordViewModel(repository) as T
            }
    }
}

/** 供 PR 页面默认日期使用。 */
internal val todayIso: String get() = DateUtils.today()
