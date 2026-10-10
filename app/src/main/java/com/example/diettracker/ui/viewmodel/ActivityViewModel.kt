package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.ActivityLogEntity
import com.example.diettracker.data.db.ExerciseLogEntity
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.domain.SportCategory
import com.example.diettracker.domain.SportInfo
import com.example.diettracker.domain.SportLibrary
import com.example.diettracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 运动库里的一项，带上「按当前体重，默认时长能消耗多少」。 */
data class SportRow(
    val info: SportInfo,
    val estimatedKcal: Double,
    val alreadyAdded: Boolean = false
) {
    val key: String get() = info.key
    val name: String get() = info.name
    val metLabel: String get() = "MET ${trim(info.met)}"
    val defaultLabel: String get() = "${info.defaultMinutes} 分钟"
    val kcalLabel: String
        get() = if (estimatedKcal > 0) "≈ ${Math.round(estimatedKcal)} kcal" else "—"

    private fun trim(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}

/** 当天的一项运动 + 它下面挂的动作（撸铁才有）。 */
data class ActivityCard(
    val activity: ActivityLogEntity,
    val exercises: List<ExerciseLogEntity> = emptyList()
) {
    val id: Long get() = activity.id
    val name: String get() = activity.sportName
    val durationLabel: String get() = "${activity.durationMinutes} 分钟"
    val kcalLabel: String get() = "${Math.round(activity.burnedKcal)} kcal"
    val isStrength: Boolean get() = activity.isStrength
    val isOverridden: Boolean get() = activity.burnOverridden

    /** 撸铁的总容量（kg × 次数），不是撸铁时为 0。 */
    val totalVolume: Double get() = exercises.sumOf { it.volume }
}

/** 今日运动页的状态。 */
data class ActivityUiState(
    val date: String = DateUtils.today(),
    val cards: List<ActivityCard> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    /** 运动库（按分组），供选择器使用。 */
    val sportGroups: List<Pair<SportCategory, List<SportRow>>> = emptyList(),
    /** 可选动作名（内置 + 自建），供撸铁加动作用。 */
    val exerciseNames: List<String> = emptyList(),
    /** 当前体重，用于展示与热量估算。 */
    val bodyWeightKg: Double = 70.0
) {
    /** 只含有氧 / 操课等项目（上段）。 */
    val sportCards: List<ActivityCard> get() = cards.filterNot { it.isStrength }

    /** 撸铁（下段）。 */
    val strengthCards: List<ActivityCard> get() = cards.filter { it.isStrength }

    val totalKcal: Double get() = cards.sumOf { it.activity.burnedKcal }

    val totalKcalLabel: String get() = "${Math.round(totalKcal)} kcal"

    val totalMinutes: Int get() = cards.sumOf { it.activity.durationMinutes }

    val isEmpty: Boolean get() = cards.isEmpty()
}

/**
 * 今日运动的 ViewModel。
 *
 * 负责把当天的运动项目与撸铁动作读出来、写回去。热量统一走 MET 估算
 * （`MET × 体重 × 小时`），允许用户手改；改过长度的会按新时长重算。
 */
class ActivityViewModel(
    private val repository: ActivityRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ActivityUiState())
    val ui: StateFlow<ActivityUiState> = _ui.asStateFlow()

    /** 当前体重，用于估算与展示。 */
    private var bodyWeightKg: Double = 70.0

    init {
        // 立刻订阅**今天**的运动。之前只在 setDate() 里订阅，而首页首次进入并不会
        // 调用 setDate，于是 loading 永远停在 true、运动区一直显示「正在读取」，
        // 连带「加动作」按钮都渲染不出来。
        observeDate(_ui.value.date)
        viewModelScope.launch {
            bodyWeightKg = repository.currentWeightKg()
            refreshSportLibrary()
            _ui.update { it.copy(bodyWeightKg = bodyWeightKg) }
        }
        viewModelScope.launch {
            repository.observeProfile().collect { profile ->
                bodyWeightKg = profile.weightKg
                refreshSportLibrary()
                _ui.update { it.copy(bodyWeightKg = bodyWeightKg) }
            }
        }
        // 自建运动变化时刷新选择器。
        viewModelScope.launch {
            repository.observeSportGroups().collect { groups: List<Pair<SportCategory, List<SportInfo>>> ->
                val rows: List<Pair<SportCategory, List<SportRow>>> = groups.map { pair ->
                    val category: SportCategory = pair.first
                    val list: List<SportInfo> = pair.second
                    category to list.map { info ->
                        SportRow(
                            info = info,
                            estimatedKcal = repository.estimateKcal(
                                info.met,
                                bodyWeightKg,
                                info.defaultMinutes
                            )
                        )
                    }
                }
                _ui.update { it.copy(sportGroups = rows) }
            }
        }
        // 动作库随时可能新增自建动作，持续订阅，保证「加动作」里立刻能看到。
        viewModelScope.launch {
            repository.observeCustomExercises().collect {
                refreshExerciseNames()
            }
        }
    }

    /** 切到某一天，重新读当天的运动。 */
    fun setDate(date: String) {
        if (_ui.value.date == date && observingDate == date) return
        _ui.update { it.copy(date = date, loading = true) }
        observeDate(date)
    }

    private var observingDate: String? = null

    private fun observeDate(date: String) {
        if (observingDate == date) return
        observingDate = date
        // 同时订阅「当天的运动」和「动作表」：撸铁下面挂的动作卡片增删改时，
        // 只订阅活动表是收不到通知的，卡片会一直停在旧内容。
        viewModelScope.launch {
            combine(
                repository.observeActivities(date),
                repository.observeExercisesOn(date)
            ) { activities, exercisesByDate -> activities to exercisesByDate }
                .collect { (activities, allExercises) ->
                    // 只有当前正在看的这一天，才允许写进 UI（避免切日期时旧数据覆盖）。
                    if (observingDate != date) return@collect
                    val byActivity = allExercises.groupBy { it.activityId }
                    val cards = activities.map { activity ->
                        ActivityCard(
                            activity = activity,
                            exercises = if (activity.isStrength) {
                                byActivity[activity.id].orEmpty()
                            } else {
                                emptyList()
                            }
                        )
                    }
                    _ui.update { it.copy(cards = cards, loading = false) }
                }
        }
    }

    private suspend fun refreshSportLibrary() {
        val groups = repository.sportGroups()
        val rows = groups.map { (category, list) ->
            category to list.map { info ->
                SportRow(
                    info = info,
                    estimatedKcal = repository.estimateKcal(
                        info.met,
                        bodyWeightKg,
                        info.defaultMinutes
                    )
                )
            }
        }
        _ui.update { it.copy(sportGroups = rows) }
    }

    // ------------------------------------------------------- 自建运动

    /** 新建一个自建运动（名称 / MET / 默认时长），成功后刷新选择器。 */
    fun addCustomSport(name: String, met: Double, defaultMinutes: Int) {
        viewModelScope.launch {
            repository.addCustomSport(name, met, defaultMinutes).fold(
                onSuccess = {
                    _ui.update { it.copy(message = "已添加运动「${name.trim()}」") }
                    refreshSportLibrary()
                },
                onFailure = { e ->
                    _ui.update { it.copy(error = e.message ?: "添加失败") }
                }
            )
        }
    }

    fun deleteCustomSport(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomSport(id).fold(
                onSuccess = {
                    _ui.update { it.copy(message = "已删除自建运动") }
                    refreshSportLibrary()
                },
                onFailure = { e ->
                    _ui.update { it.copy(error = e.message ?: "删除失败") }
                }
            )
        }
    }

    private suspend fun refreshExerciseNames() {
        _ui.update { it.copy(exerciseNames = repository.allExerciseNames()) }
    }

    fun clearMessage() = _ui.update { it.copy(message = null, error = null) }

    // ------------------------------------------------------------ 写操作

    /** 加一项运动；撸铁会自动带上「撸铁」这一项。 */
    fun addSport(sportKey: String, durationMinutes: Int? = null) {
        val date = _ui.value.date
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            repository.addSport(date, sportKey, durationMinutes).fold(
                onSuccess = { activityId ->
                    val name = repository.findSport(sportKey)?.name ?: "运动"
                    if (sportKey == SportLibrary.STRENGTH_KEY) {
                        // 撸铁：再自动加一张默认动作卡片，让用户直接改就行。
                        val first = _ui.value.exerciseNames.firstOrNull()
                        if (first != null) {
                            repository.addExercise(activityId, date, first)
                        }
                    }
                    _ui.update {
                        it.copy(
                            saving = false,
                            message = if (sportKey == SportLibrary.STRENGTH_KEY) {
                                "已加入撸铁，可以往里加动作"
                            } else {
                                "已加入「$name」"
                            }
                        )
                    }
                },
                onFailure = { e ->
                    _ui.update { it.copy(saving = false, error = e.message ?: "添加失败") }
                }
            )
        }
    }

    /** 改时长，热量按新时长重算。 */
    fun setDuration(activityId: Long, minutes: Int) {
        viewModelScope.launch {
            repository.updateDuration(activityId, minutes, bodyWeightKg).fold(
                onSuccess = { _ui.update { it.copy(message = "已改为 $minutes 分钟") } },
                onFailure = { e -> _ui.update { it.copy(error = e.message ?: "保存失败") } }
            )
        }
    }

    /** 手改热量；传 null 表示恢复估算值。 */
    fun overrideBurn(activityId: Long, kcal: Double?) {
        viewModelScope.launch {
            repository.overrideBurn(activityId, kcal).fold(
                onSuccess = {
                    _ui.update {
                        it.copy(
                            message = if (kcal == null) "已恢复估算值" else "热量已手动改为 ${Math.round(kcal)} kcal"
                        )
                    }
                },
                onFailure = { e -> _ui.update { it.copy(error = e.message ?: "保存失败") } }
            )
        }
    }

    fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            repository.deleteActivity(activityId).fold(
                onSuccess = { _ui.update { it.copy(message = "已删除") } },
                onFailure = { e -> _ui.update { it.copy(error = e.message ?: "删除失败") } }
            )
        }
    }

    /** 往撸铁里加一个动作卡片。 */
    fun addExercise(activityId: Long, exerciseName: String) {
        val date = _ui.value.date
        viewModelScope.launch {
            repository.addExercise(activityId, date, exerciseName).fold(
                onSuccess = { _ui.update { it.copy(message = "已加入「$exerciseName」") } },
                onFailure = { e -> _ui.update { it.copy(error = e.message ?: "添加失败") } }
            )
        }
    }

    /** 改动作卡片的重量 / 组数 / 次数。 */
    fun updateExercise(exerciseId: Long, weightKg: Double, sets: Int, reps: Int) {
        viewModelScope.launch {
            repository.updateExercise(exerciseId, weightKg, sets, reps).fold(
                onSuccess = { _ui.update { it.copy(message = "已保存") } },
                onFailure = { e -> _ui.update { it.copy(error = e.message ?: "保存失败") } }
            )
        }
    }

    fun deleteExercise(exerciseId: Long) {
        viewModelScope.launch {
            repository.deleteExercise(exerciseId).fold(
                onSuccess = { },
                onFailure = { e -> _ui.update { it.copy(error = e.message ?: "删除失败") } }
            )
        }
    }

    /** 清空当天运动。 */
    fun clearDay() {
        val date = _ui.value.date
        viewModelScope.launch {
            repository.deleteActivitiesOn(date).fold(
                onSuccess = { _ui.update { it.copy(message = "已清空当天运动") } },
                onFailure = { e -> _ui.update { it.copy(error = e.message ?: "清空失败") } }
            )
        }
    }

    companion object {
        fun factory(repository: ActivityRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ActivityViewModel(repository) as T
            }
    }
}
