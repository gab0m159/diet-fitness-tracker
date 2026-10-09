package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.ExerciseOutcome
import com.example.diettracker.data.db.ExerciseStatusEntity
import com.example.diettracker.data.repository.TrainingRepository
import com.example.diettracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 往期记录里的一天。 */
data class HistoryDay(
    val date: String,
    val entries: List<ExerciseStatusEntity>
) {
    val label: String get() = DateUtils.displayDate(date)

    val weekday: String get() = DateUtils.weekday(date)

    val successCount: Int
        get() = entries.count { it.outcomeValue == ExerciseOutcome.SUCCESS }

    val failureCount: Int
        get() = entries.count { it.outcomeValue == ExerciseOutcome.FAILURE }

    /** 「成功 3 · 失败 1」 */
    val summary: String get() = "成功 $successCount · 失败 $failureCount"
}

data class TrainingHistoryUiState(
    val days: List<HistoryDay> = emptyList(),
    val totalEntries: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val query: String = "",
    val loading: Boolean = true
) {
    val isEmpty: Boolean get() = days.isEmpty()
}

/**
 * 往期训练记录。
 *
 * 只读 `exercise_status` 里成功与失败的行——跳过的动作**永远不出现在这里**，
 * 这是产品需求里明确规定的。每条记录显示日期、动作名、目标组数/次数/重量和结果。
 */
class TrainingHistoryViewModel(
    private val repository: TrainingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TrainingHistoryUiState())
    val state: StateFlow<TrainingHistoryUiState> = _state.asStateFlow()

    /** 原始记录，过滤时不用重新查库。 */
    private var all: List<ExerciseStatusEntity> = emptyList()

    init {
        viewModelScope.launch {
            repository.observeHistory().collect { entries ->
                all = entries
                applyFilter(_state.value.query)
            }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        applyFilter(value)
    }

    private fun applyFilter(query: String) {
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            all
        } else {
            all.filter {
                it.exerciseName.contains(trimmed, ignoreCase = true) ||
                    it.sourceLabel.contains(trimmed, ignoreCase = true)
            }
        }
        val days = filtered
            .groupBy { it.date }
            .toList()
            .sortedByDescending { (date, _) -> date }
            .map { (date, entries) ->
                HistoryDay(
                    date = date,
                    entries = entries.sortedByDescending { it.id }
                )
            }
        _state.update {
            it.copy(
                days = days,
                totalEntries = filtered.size,
                successCount = filtered.count { entry ->
                    entry.outcomeValue == ExerciseOutcome.SUCCESS
                },
                failureCount = filtered.count { entry ->
                    entry.outcomeValue == ExerciseOutcome.FAILURE
                },
                loading = false
            )
        }
    }

    companion object {
        fun factory(repository: TrainingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TrainingHistoryViewModel(repository) as T
            }
    }
}
