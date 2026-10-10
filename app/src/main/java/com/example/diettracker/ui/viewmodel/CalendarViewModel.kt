package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.ui.components.DayOverview
import com.example.diettracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

/** 月历的数据状态。 */
data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    /** key 是 ISO 日期。 */
    val overviews: Map<String, DayOverview> = emptyMap(),
    val loading: Boolean = false,
    val message: String? = null
)

/**
 * 月历（日期总览）与每日命名。
 *
 * 数据来自三处：饮食条数/热量、运动项数/消耗、用户给这天起的名字。都按**整月**
 * 一次查出来，翻月时再查新的一月——避免一个月渲染 31 次查询。
 */
class CalendarViewModel(
    private val dietRepository: DietRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(CalendarUiState())
    val ui: StateFlow<CalendarUiState> = _ui.asStateFlow()

    /** 当前月的目标热量（取全局宏量目标折算，用于判断是否超标）。 */
    private var goalKcal: Double = 0.0

    init {
        refresh()
    }

    /** 切到某个月并重新拉数据。 */
    fun setMonth(month: YearMonth) {
        if (month == _ui.value.month && _ui.value.overviews.isNotEmpty()) return
        _ui.update { it.copy(month = month, loading = true) }
        refresh()
    }

    /** 翻月：上/下个月。 */
    fun shiftMonth(delta: Long) = setMonth(_ui.value.month.plusMonths(delta))

    fun refresh() {
        val month = _ui.value.month
        val from = month.atDay(1).toString()
        val to = month.atEndOfMonth().toString()
        viewModelScope.launch {
            val goals = dietRepository.getGoals()
            goalKcal = goals.carbsGrams * 4 + goals.proteinGrams * 4 + goals.fatGrams * 9

            val foodTotals = runCatching {
                dietRepository.dailyEntryTotals(from, to)
            }.getOrDefault(emptyMap())
            val sportTotals = runCatching {
                activityRepository.dailyActivityTotals(from, to)
            }.getOrDefault(emptyMap())
            val labels = runCatching {
                activityRepository.getDayLabels(from, to)
            }.getOrDefault(emptyMap())

            // 以「有饮食或有运动或有名字」的日期为准建格子
            val dates = buildSet {
                addAll(foodTotals.keys)
                addAll(sportTotals.keys)
                addAll(labels.keys)
            }
            val overviews = dates.associateWith { date ->
                val (foodCount, intake) = foodTotals[date] ?: (0 to 0.0)
                val (activityCount, burned) = sportTotals[date] ?: (0 to 0.0)
                DayOverview(
                    date = date,
                    label = labels[date].orEmpty(),
                    intakeKcal = intake,
                    goalKcal = goalKcal,
                    burnedKcal = burned,
                    activityCount = activityCount,
                    foodCount = foodCount
                )
            }
            _ui.update { it.copy(overviews = overviews, loading = false) }
        }
    }

    /** 给某一天起名字。 */
    fun setDayLabel(date: String, label: String) {
        viewModelScope.launch {
            activityRepository.setDayLabel(date, label).fold(
                onSuccess = {
                    _ui.update {
                        it.copy(message = if (label.isBlank()) "已清除名字" else "已命名为「$label」")
                    }
                    refresh()
                },
                onFailure = { e ->
                    _ui.update { it.copy(message = e.message ?: "保存失败") }
                }
            )
        }
    }

    /** 某一天的名字，今日页标题用。 */
    fun dayLabel(date: String): String =
        _ui.value.overviews[date]?.label.orEmpty()

    fun clearMessage() = _ui.update { it.copy(message = null) }

    companion object {
        fun factory(
            dietRepository: DietRepository,
            activityRepository: ActivityRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CalendarViewModel(dietRepository, activityRepository) as T
        }

        /** 今天的 ISO 串，供默认值使用。 */
        val today: String get() = DateUtils.today()
    }
}
