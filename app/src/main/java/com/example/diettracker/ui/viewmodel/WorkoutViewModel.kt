package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.ExerciseOutcome
import com.example.diettracker.data.db.ExerciseRecordEntity
import com.example.diettracker.data.db.ScheduleEntryKind
import com.example.diettracker.data.model.SessionStatus
import com.example.diettracker.data.model.TrainingIntensity
import com.example.diettracker.data.model.TrainingObjective
import com.example.diettracker.data.repository.TodayExerciseCard
import com.example.diettracker.data.repository.TodayPlan
import com.example.diettracker.data.repository.TrainingRepository
import com.example.diettracker.domain.ProgressionEngine
import com.example.diettracker.domain.StretchGuide
import com.example.diettracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 详细记录页里的一个动作行（每组次数 / 重量 / 组数）。
 *
 * 重量、组数、次数都保留成字符串，方便用户输入到一半的状态也能留在界面上。
 */
data class ExerciseEntry(
    /** 卡片身份（来源类型 + 来源行 id），同一天两个同名动作也能区分开。 */
    val cardKey: String,
    val exerciseName: String,
    val weightText: String = "",
    val setsText: String = "3",
    val repsText: String = "",
    val repsMin: Int = 8,
    val repsMax: Int = 12,
    /** 上次的成绩摘要，例如「2026-09-20 成功 60kg」。 */
    val lastSummary: String? = null,
    val personalBest: Double = 0.0
) {
    val weightKg: Double? get() = weightText.toDoubleOrNull()?.takeIf { it >= 0 }
    val sets: Int? get() = setsText.toIntOrNull()?.takeIf { it > 0 }
    val reps: List<Int>
        get() = repsText.split(',', '，', ' ')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }

    val isComplete: Boolean
        get() = weightKg != null && sets != null && reps.isNotEmpty()

    val totalReps: Int get() = reps.sum()

    val volume: Double get() = (weightKg ?: 0.0) * totalReps
}

/** 今日训练栏的状态。 */
data class TrainingHomeUiState(
    val plan: TodayPlan? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    /** 最近 7 天累计消耗热量，用于小结。 */
    val weekBurned: Double = 0.0
)

/** 详细记录页的状态（时长 / 强度 / 热量 / 每组次数 / 拉伸）。 */
data class WorkoutLogUiState(
    val sessionId: Long? = null,
    val date: String = DateUtils.today(),
    val title: String = "",
    val objective: TrainingObjective = TrainingObjective.HYPERTROPHY,
    val entries: List<ExerciseEntry> = emptyList(),
    val durationText: String = "60",
    val intensity: TrainingIntensity = TrainingIntensity.HEAVY,
    val burnText: String = "",
    val burnOverridden: Boolean = false,
    val bodyWeightKg: Double = 70.0,
    val status: SessionStatus = SessionStatus.IN_PROGRESS,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val stretches: List<StretchGuide> = emptyList(),
    val showStretches: Boolean = false
) {
    val durationMinutes: Int? get() = durationText.toIntOrNull()?.takeIf { it >= 0 }
    val burnKcal: Double? get() = burnText.toDoubleOrNull()?.takeIf { it >= 0 }

    /** 当前 MET 估算值，作为热量输入框的提示与「用估算值」按钮的目标。 */
    val estimatedBurn: Double
        get() {
            val minutes = durationMinutes ?: 0
            return intensity.met * bodyWeightKg * (minutes / 60.0)
        }

    val totalVolume: Double get() = entries.sumOf { it.volume }
    val completedEntries: Int get() = entries.count { it.isComplete }

    val restLabel: String get() = ProgressionEngine.restLabel(objective)
}

/**
 * 今日训练栏 + 详细记录页的 ViewModel。
 *
 * 今日栏直接渲染 [TodayPlan] 里的分组和卡片：每组是一个训练日（或「单独动作」），
 * 卡片上直接按成功 / 失败 / 跳过。写完就重新解析一次，界面永远反映数据库里的真
 * 实状态（目标重量、结果、到期日都在库里）。
 */
class WorkoutViewModel(
    private val repository: TrainingRepository
) : ViewModel() {

    private val _home = MutableStateFlow(TrainingHomeUiState())
    val home: StateFlow<TrainingHomeUiState> = _home.asStateFlow()

    private val _log = MutableStateFlow(WorkoutLogUiState())
    val log: StateFlow<WorkoutLogUiState> = _log.asStateFlow()

    /** 今日栏跟随饮食页选择的日期。 */
    private val selectedDate = MutableStateFlow(DateUtils.today())

    init {
        // 训练日 / 训练目标一变就重新解析今天该练什么。
        viewModelScope.launch { repository.observeSplits().collect { refresh() } }
        viewModelScope.launch { repository.observeTrainingGoal().collect { refresh() } }
        viewModelScope.launch {
            repository.observeBurnedKcalSince(DateUtils.minusDays(DateUtils.today(), 7))
                .collect { total -> _home.update { it.copy(weekBurned = total) } }
        }
        viewModelScope.launch {
            _log.update { it.copy(bodyWeightKg = repository.currentWeightKg()) }
        }
        refresh()
    }

    /** 今日栏指向哪一天（由首页的日期导航调用）。 */
    fun setDate(date: String) {
        selectedDate.value = date
        refresh()
    }

    /** 重新解析当前日期该练什么。 */
    fun refresh() {
        val date = selectedDate.value
        viewModelScope.launch {
            val plan = repository.resolveToday(date)
            _home.update { it.copy(plan = plan, loading = false) }
        }
    }

    fun clearMessage() = _home.update { it.copy(message = null, error = null) }

    // ------------------------------------------------------ 卡片三按钮

    fun markSuccess(groupKey: String, cardKey: String) =
        applyOutcome(groupKey, cardKey, ExerciseOutcome.SUCCESS)

    fun markFailure(groupKey: String, cardKey: String) =
        applyOutcome(groupKey, cardKey, ExerciseOutcome.FAILURE)

    fun markSkip(groupKey: String, cardKey: String) =
        applyOutcome(groupKey, cardKey, ExerciseOutcome.SKIPPED)

    private fun applyOutcome(groupKey: String, cardKey: String, outcome: ExerciseOutcome) {
        val plan = _home.value.plan ?: return
        val card = findCard(plan, groupKey, cardKey) ?: return
        _home.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            repository.recordOutcome(plan.date, card, outcome).fold(
                onSuccess = { result ->
                    _home.update { it.copy(saving = false, message = result.explanation) }
                    refresh()
                },
                onFailure = { e ->
                    _home.update { it.copy(saving = false, error = "记录失败：${e.message}") }
                }
            )
        }
    }

    /** 撤销一张卡片的结果。 */
    fun resetOutcome(groupKey: String, cardKey: String) {
        val plan = _home.value.plan ?: return
        val card = findCard(plan, groupKey, cardKey) ?: return
        viewModelScope.launch {
            repository.clearOutcome(plan.date, card).fold(
                onSuccess = {
                    _home.update { it.copy(message = "已撤销「${card.exerciseName}」") }
                    refresh()
                },
                onFailure = { e ->
                    _home.update { it.copy(error = "撤销失败：${e.message}") }
                }
            )
        }
    }

    private fun findCard(plan: TodayPlan, groupKey: String, cardKey: String): TodayExerciseCard? =
        plan.groups.firstOrNull { it.key == groupKey }
            ?.cards
            ?.firstOrNull { it.key == cardKey }

    // ------------------------------------------------- 训练日整体操作

    /** 训练日整体跳过：其下动作全部标记跳过，且不进往期。 */
    fun skipDay(groupKey: String) {
        val plan = _home.value.plan ?: return
        val group = plan.groups.firstOrNull { it.key == groupKey } ?: return
        val splitId = group.splitId ?: return
        _home.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            repository.skipTrainingDay(plan.date, splitId).fold(
                onSuccess = {
                    _home.update {
                        it.copy(saving = false, message = "已跳过「${group.title}」，不计入往期")
                    }
                    refresh()
                },
                onFailure = { e ->
                    _home.update { it.copy(saving = false, error = "跳过失败：${e.message}") }
                }
            )
        }
    }

    /** 延期：所有训练日统一往后顺延一天。 */
    fun postponeAll() {
        val plan = _home.value.plan ?: return
        _home.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            repository.postponeAll(plan.date).fold(
                onSuccess = {
                    _home.update {
                        it.copy(
                            saving = false,
                            message = "已延期：今天的训练整体挪到明天，后续训练日一起顺延"
                        )
                    }
                    refresh()
                },
                onFailure = { e ->
                    _home.update { it.copy(saving = false, error = "延期失败：${e.message}") }
                }
            )
        }
    }

    /** 从当天日程里移除一项（不写任何结果）。 */
    fun removeScheduleEntry(entryId: Long) {
        viewModelScope.launch {
            repository.removeScheduleEntry(entryId).fold(
                onSuccess = {
                    _home.update { it.copy(message = "已从今天移除") }
                    refresh()
                },
                onFailure = { e ->
                    _home.update { it.copy(error = "移除失败：${e.message}") }
                }
            )
        }
    }

    // ------------------------------------------------- 添加训练到某天

    /** 把整个训练日加进今天。 */
    fun addTrainingDayToDate(date: String, splitId: Long) {
        viewModelScope.launch {
            repository.addTrainingDayToDate(date, splitId).fold(
                onSuccess = {
                    _home.update { it.copy(message = "已加入今天") }
                    refresh()
                },
                onFailure = { e ->
                    _home.update { it.copy(error = "添加失败：${e.message}") }
                }
            )
        }
    }

    /** 单独加一个动作卡片进今天。 */
    fun addSingleExerciseToDate(
        date: String,
        exerciseName: String,
        targetSets: Int,
        targetReps: String,
        targetWeightKg: Double,
        incrementKg: Double?
    ) {
        viewModelScope.launch {
            repository.addSingleExerciseToDate(
                date = date,
                exerciseName = exerciseName,
                targetSets = targetSets,
                targetReps = targetReps,
                targetWeightKg = targetWeightKg,
                incrementKg = incrementKg
            ).fold(
                onSuccess = {
                    _home.update { it.copy(message = "已加入「$exerciseName」") }
                    refresh()
                },
                onFailure = { e ->
                    _home.update { it.copy(error = "添加失败：${e.message}") }
                }
            )
        }
    }

    // ----------------------------------------------------- 详细记录页

    /** 打开详细记录页：没有当天的运动记录就建一条，并把动作行预填好。 */
    fun openLog(date: String = selectedDate.value) {
        _log.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            val plan = repository.resolveToday(date)
            repository.ensureSession(date, plan.titleLine).fold(
                onSuccess = { sessionId -> loadSessionInternal(sessionId, plan) },
                onFailure = { e ->
                    _log.update {
                        it.copy(loading = false, error = "无法开始记录：${e.message}")
                    }
                }
            )
        }
    }

    fun loadSession(sessionId: Long, date: String = selectedDate.value) {
        viewModelScope.launch {
            loadSessionInternal(sessionId, repository.resolveToday(date))
        }
    }

    private suspend fun loadSessionInternal(sessionId: Long, plan: TodayPlan) {
        val session = repository.getSession(sessionId)
        if (session == null) {
            _log.update { it.copy(loading = false, error = "训练记录不存在") }
            return
        }
        val saved = repository.getRecords(sessionId)
        // 动作行来自今天日程里的卡片，目标值直接取卡片上的。
        val cards = plan.groups.flatMap { it.cards }

        val entries = cards.map { card ->
            val existing = saved.firstOrNull { it.exerciseName == card.exerciseName }
            val range = ProgressionEngine.repRange(card.targetReps)
            ExerciseEntry(
                cardKey = card.key,
                exerciseName = card.exerciseName,
                weightText = existing?.let { trim(it.weightKg) }
                    ?: card.targetWeightKg.takeIf { it > 0.0 }?.let { trim(it) }
                    ?: "",
                setsText = (existing?.sets ?: card.targetSets).toString(),
                repsText = existing?.repsPerSet ?: "",
                repsMin = existing?.repsMin ?: range?.first ?: 8,
                repsMax = existing?.repsMax ?: range?.last ?: 12,
                lastSummary = card.lastSummary,
                personalBest = repository.personalBest(card.exerciseName)
            )
        }

        _log.update {
            it.copy(
                sessionId = sessionId,
                date = session.date,
                title = session.title,
                objective = plan.objective,
                entries = entries,
                durationText = session.durationMinutes.toString(),
                intensity = TrainingIntensity.fromStorage(session.intensity),
                burnText = trim(session.burnedKcal),
                burnOverridden = session.burnOverridden,
                status = SessionStatus.fromStorage(session.status),
                loading = false,
                error = null
            )
        }
    }

    // ---------------------------------------------------------- 输入

    fun onWeightChange(index: Int, value: String) = updateEntry(index) {
        it.copy(weightText = sanitizeDecimal(value))
    }

    fun onSetsChange(index: Int, value: String) = updateEntry(index) {
        it.copy(setsText = value.filter { ch -> ch.isDigit() }.take(2))
    }

    fun onRepsChange(index: Int, value: String) = updateEntry(index) {
        it.copy(repsText = sanitizeReps(value))
    }

    fun onDurationChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(3)
        _log.update { state ->
            val updated = state.copy(durationText = digits, error = null)
            if (!updated.burnOverridden) {
                updated.copy(burnText = trim(updated.estimatedBurn))
            } else {
                updated
            }
        }
    }

    fun onIntensityChange(value: TrainingIntensity) {
        _log.update { state ->
            val updated = state.copy(intensity = value, error = null)
            if (!updated.burnOverridden) {
                updated.copy(burnText = trim(updated.estimatedBurn))
            } else {
                updated
            }
        }
    }

    /** 手改消耗热量。 */
    fun onBurnChange(value: String) = _log.update {
        it.copy(burnText = sanitizeDecimal(value), burnOverridden = true, error = null)
    }

    /** 回到 MET 估算值。 */
    fun resetBurnToEstimate() = _log.update {
        it.copy(burnText = trim(it.estimatedBurn), burnOverridden = false)
    }

    private fun updateEntry(index: Int, transform: (ExerciseEntry) -> ExerciseEntry) {
        _log.update { state ->
            val list = state.entries.toMutableList()
            if (index in list.indices) {
                list[index] = transform(list[index])
            }
            state.copy(entries = list, error = null)
        }
    }

    // ---------------------------------------------------------- 保存

    /** 保存训练记录；[complete] 为真时同时标记整天已完成（热量才计入消耗）。 */
    fun save(complete: Boolean) {
        val state = _log.value
        val sessionId = state.sessionId
        if (sessionId == null) {
            _log.update { it.copy(error = "训练记录不存在") }
            return
        }
        val duration = state.durationMinutes
        if (duration == null || duration <= 0) {
            _log.update { it.copy(error = "请填写训练时长（分钟）") }
            return
        }
        val burn = state.burnKcal
        if (burn == null) {
            _log.update { it.copy(error = "消耗热量请填数字，或点击「用估算值」") }
            return
        }
        if (complete && state.completedEntries == 0) {
            _log.update { it.copy(error = "至少填写一个动作的重量、组数和次数") }
            return
        }

        _log.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            repository.updateSessionMetrics(
                sessionId = sessionId,
                durationMinutes = duration,
                intensity = state.intensity,
                burnedKcalOverride = if (state.burnOverridden) burn else null
            ).onFailure { e ->
                _log.update { it.copy(saving = false, error = "保存失败：${e.message}") }
                return@launch
            }

            val records = state.entries
                .filter { it.isComplete }
                .map { entry ->
                    ExerciseRecordEntity(
                        sessionId = sessionId,
                        date = state.date,
                        exerciseName = entry.exerciseName,
                        weightKg = entry.weightKg ?: 0.0,
                        sets = entry.sets ?: entry.reps.size,
                        repsPerSet = ExerciseRecordEntity.encodeReps(entry.reps),
                        repsMin = entry.repsMin,
                        repsMax = entry.repsMax
                    )
                }
            repository.saveRecords(sessionId, records).onFailure { e ->
                _log.update { it.copy(saving = false, error = "保存动作失败：${e.message}") }
                return@launch
            }

            if (complete) {
                repository.completeSession(sessionId).onFailure { e ->
                    _log.update { it.copy(saving = false, error = "完成失败：${e.message}") }
                    return@launch
                }
            }

            val stretches = repository.stretchesForExercises(records.map { it.exerciseName })
            _log.update {
                it.copy(
                    saving = false,
                    status = if (complete) SessionStatus.COMPLETED else it.status,
                    message = if (complete) "训练已完成" else "已保存",
                    stretches = stretches,
                    showStretches = complete && stretches.isNotEmpty()
                )
            }
            refresh()
        }
    }

    fun dismissStretches() = _log.update { it.copy(showStretches = false) }

    // ---------------------------------------------------------- helpers

    private fun sanitizeDecimal(raw: String): String {
        val builder = StringBuilder()
        var dot = false
        for (ch in raw) {
            when {
                ch.isDigit() -> builder.append(ch)
                (ch == '.' || ch == ',') && !dot -> {
                    builder.append('.')
                    dot = true
                }
            }
        }
        return builder.toString()
    }

    /** 允许数字和分隔符，方便输入 "12,12,10"。 */
    private fun sanitizeReps(raw: String): String {
        val builder = StringBuilder()
        for (ch in raw) {
            when {
                ch.isDigit() -> builder.append(ch)
                ch == ',' || ch == '，' || ch == ' ' -> {
                    if (builder.isNotEmpty() && !builder.endsWith(",")) {
                        builder.append(',')
                    }
                }
            }
        }
        return builder.toString()
    }

    private fun trim(value: Double): String {
        val rounded = Math.round(value * 10.0) / 10.0
        return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
        else rounded.toString()
    }

    companion object {
        fun factory(repository: TrainingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    WorkoutViewModel(repository) as T
            }
    }
}

/** 会话状态的中文标签。 */
fun statusLabelOf(status: String): String =
    SessionStatus.fromStorage(status).label

/** 卡片来源的中文标签。 */
fun cardSourceLabel(kind: ScheduleEntryKind): String = when (kind) {
    ScheduleEntryKind.TRAINING_DAY -> "训练日"
    ScheduleEntryKind.SINGLE_EXERCISE -> "单独动作"
}
