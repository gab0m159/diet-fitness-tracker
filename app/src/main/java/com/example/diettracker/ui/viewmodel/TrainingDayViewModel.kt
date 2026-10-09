package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.ScheduleEntryKind
import com.example.diettracker.data.db.TrainingDayExerciseEntity
import com.example.diettracker.data.db.TrainingSplitEntity
import com.example.diettracker.data.model.BodyPart
import com.example.diettracker.data.model.SplitType
import com.example.diettracker.data.model.TrainingObjective
import com.example.diettracker.data.repository.TrainingRepository
import com.example.diettracker.domain.ExerciseLibrary
import com.example.diettracker.domain.FrequencyScheduler
import com.example.diettracker.domain.PlanTemplate
import com.example.diettracker.domain.PlanTemplates
import com.example.diettracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 「我的 → 训练日管理」列表里的一行。 */
data class TrainingDaySummary(
    val split: TrainingSplitEntity,
    val rows: List<TrainingDayExerciseEntity>,
    val dueLabel: String
) {
    val id: Long get() = split.id
    val name: String get() = split.name
    val bodyPart: BodyPart get() = BodyPart.fromStorage(split.bodyPart)
    val exerciseCount: Int get() = rows.size
    val intervalLabel: String get() = FrequencyScheduler.describeInterval(split.intervalDays)

    /** 「练 3 休 1」之后这里显示「每 4 天一次 · 今天该练」。 */
    val statusLabel: String
        get() = "${intervalLabel} · $dueLabel"

    val exercisePreview: String
        get() = rows.take(4).joinToString("、") { it.exerciseName } +
            if (rows.size > 4) "…" else ""
}

data class TrainingDayListUiState(
    val days: List<TrainingDaySummary> = emptyList(),
    val templates: List<PlanTemplate> = emptyList(),
    val objective: TrainingObjective = TrainingObjective.HYPERTROPHY,
    val currentTemplateType: SplitType? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

/** 训练日编辑器状态：名称 + 部位 + 频率 + 动作清单（每个动作带目标四项）。 */
data class TrainingDayEditorUiState(
    val splitId: Long = 0L,
    val name: String = "",
    val bodyPart: BodyPart = BodyPart.FULL_BODY,
    val intervalDays: Int = TrainingSplitEntity.DEFAULT_INTERVAL_DAYS,
    val rows: List<TrainingDayExerciseEntity> = emptyList(),
    val allExercises: List<String> = emptyList(),
    val objective: TrainingObjective = TrainingObjective.HYPERTROPHY,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null
) {
    val isNew: Boolean get() = splitId == 0L

    /** 可选部位：休息日不是训练日，排除掉。 */
    val bodyPartChoices: List<BodyPart>
        get() = BodyPart.entries.filter { it != BodyPart.REST }
}

/**
 * 训练日的创建与管理（「我的」里那一块）。
 *
 * 训练日现在完全自由：任意数量、任意名字、每个带一个「几天一次」的频率，动作清
 * 单里每个动作都有自己的目标组数 / 每组次数 / 目标重量 / 步进。
 */
class TrainingDayViewModel(
    private val repository: TrainingRepository
) : ViewModel() {

    private val _list = MutableStateFlow(TrainingDayListUiState())
    val list: StateFlow<TrainingDayListUiState> = _list.asStateFlow()

    private val _editor = MutableStateFlow(TrainingDayEditorUiState())
    val editor: StateFlow<TrainingDayEditorUiState> = _editor.asStateFlow()

    /**
     * 可选动作名（内置 + 自建），「单个动作加进今天」的选择器用它。
     */
    val exerciseNames: StateFlow<List<String>> =
        repository.observeCustomExercises()
            .let { customFlow ->
                combine(customFlow, MutableStateFlow(Unit)) { custom, _ ->
                    (ExerciseLibrary.allNames() + custom.map { it.name })
                        .distinct()
                        .sorted()
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ExerciseLibrary.allNames()
            )

    init {
        _list.update { it.copy(templates = PlanTemplates.all()) }
        viewModelScope.launch { refreshList() }
        // 训练日、动作行或训练目标一变就刷新列表与编辑器。
        viewModelScope.launch { repository.observeSplits().collect { refreshList() } }
        viewModelScope.launch { repository.observeTrainingGoal().collect { refreshList() } }
    }

    private suspend fun refreshList() {
        val today = DateUtils.today()
        val splits = repository.getSplits()
        val rows = repository.exerciseRowsBySplit()
        val goal = repository.getTrainingGoal()
        _list.update { state ->
            state.copy(
                days = splits.map { split ->
                    TrainingDaySummary(
                        split = split,
                        rows = rows[split.id].orEmpty(),
                        dueLabel = FrequencyScheduler.nextDueLabel(split, today)
                    )
                },
                objective = TrainingObjective.fromStorage(goal.objective),
                currentTemplateType = SplitType.entries
                    .firstOrNull { it.name == goal.splitType },
                loading = false
            )
        }
    }

    fun clearMessages() = _list.update { it.copy(message = null, error = null) }

    fun clearEditorMessages() = _editor.update { it.copy(message = null, error = null) }

    // -------------------------------------------------------- 列表操作

    /** 新建一个训练日（当天即到期，可以马上练）。 */
    fun createDay(
        name: String,
        bodyPart: BodyPart = BodyPart.FULL_BODY,
        intervalDays: Int = TrainingSplitEntity.DEFAULT_INTERVAL_DAYS
    ) {
        viewModelScope.launch {
            repository.createTrainingDay(name, bodyPart.name, intervalDays).fold(
                onSuccess = {
                    _list.update { it.copy(message = "已创建「${name.trim()}」") }
                    refreshList()
                },
                onFailure = { e ->
                    _list.update { it.copy(error = e.message ?: "创建失败") }
                }
            )
        }
    }

    fun deleteDay(splitId: Long) {
        viewModelScope.launch {
            repository.deleteTrainingDay(splitId).fold(
                onSuccess = {
                    _list.update { it.copy(message = "已删除训练日") }
                    refreshList()
                },
                onFailure = { e ->
                    _list.update { it.copy(error = e.message ?: "删除失败") }
                }
            )
        }
    }

    /** 改「几天一次」。 */
    fun setInterval(splitId: Long, intervalDays: Int) {
        viewModelScope.launch {
            repository.setTrainingDayInterval(splitId, intervalDays).fold(
                onSuccess = {
                    _list.update {
                        it.copy(message = "频率已改为每 $intervalDays 天一次")
                    }
                    refreshList()
                },
                onFailure = { e ->
                    _list.update { it.copy(error = e.message ?: "保存失败") }
                }
            )
        }
    }

    /** 一键套用预设计划（会整体替换现有训练日）。 */
    fun adoptTemplate(template: PlanTemplate) {
        viewModelScope.launch {
            _list.update { it.copy(saving = true, error = null) }
            repository.adoptTemplate(template, _list.value.objective).fold(
                onSuccess = {
                    _list.update {
                        it.copy(
                            saving = false,
                            message = "已套用「${template.title}」，之后可以随意修改"
                        )
                    }
                    refreshList()
                },
                onFailure = { e ->
                    _list.update { it.copy(saving = false, error = e.message ?: "套用失败") }
                }
            )
        }
    }

    fun setObjective(objective: TrainingObjective) {
        viewModelScope.launch {
            repository.setObjective(objective).fold(
                onSuccess = {
                    _list.update { it.copy(objective = objective, message = "训练目标已更新") }
                    refreshList()
                },
                onFailure = { e ->
                    _list.update { it.copy(error = e.message ?: "保存失败") }
                }
            )
        }
    }

    // ------------------------------------------------------ 编辑器操作

    /** 载入一个训练日；[splitId] 为 0 表示新建。 */
    fun loadEditor(splitId: Long) {
        _editor.update { it.copy(splitId = splitId, loading = true, error = null) }
        viewModelScope.launch {
            val names = repository.allExerciseNames()
            val objective = repository.objective()
            if (splitId == 0L) {
                _editor.update {
                    it.copy(
                        splitId = 0L,
                        name = "",
                        bodyPart = BodyPart.FULL_BODY,
                        intervalDays = TrainingSplitEntity.DEFAULT_INTERVAL_DAYS,
                        rows = emptyList(),
                        allExercises = names,
                        objective = objective,
                        loading = false
                    )
                }
                return@launch
            }
            val split = repository.getSplit(splitId)
            if (split == null) {
                _editor.update { it.copy(loading = false, error = "训练日不存在") }
                return@launch
            }
            _editor.update {
                it.copy(
                    splitId = split.id,
                    name = split.name,
                    bodyPart = BodyPart.fromStorage(split.bodyPart),
                    intervalDays = split.intervalDays,
                    rows = repository.getExerciseRows(split.id),
                    allExercises = names,
                    objective = objective,
                    loading = false
                )
            }
        }
    }

    /** 编辑器里改名（新建时由保存流程落库）。 */
    fun onNameChange(value: String) = _editor.update { it.copy(name = value) }

    fun onBodyPartChange(value: BodyPart) = _editor.update { it.copy(bodyPart = value) }

    fun onIntervalChange(value: Int) = _editor.update {
        it.copy(intervalDays = TrainingSplitEntity.sanitizeInterval(value))
    }

    /** 新建的训练日：先建出训练日，再把动作行写进去。 */
    fun createWithEditorContent() {
        val state = _editor.value
        if (state.name.isBlank()) {
            _editor.update { it.copy(error = "训练日名称不能为空") }
            return
        }
        viewModelScope.launch {
            _editor.update { it.copy(saving = true, error = null) }
            repository.createTrainingDay(
                name = state.name,
                bodyPart = state.bodyPart.name,
                intervalDays = state.intervalDays
            ).fold(
                onSuccess = { splitId ->
                    state.rows.forEachIndexed { index, row ->
                        repository.addExerciseToDay(splitId, row.exerciseName)
                        repository.updateExerciseTarget(
                            rowId = repository.getExerciseRows(splitId)
                                .firstOrNull { it.exerciseName == row.exerciseName }
                                ?.id ?: return@forEachIndexed,
                            targetSets = row.targetSets,
                            targetReps = row.targetReps,
                            targetWeightKg = row.targetWeightKg,
                            incrementKg = row.incrementKg
                        )
                    }
                    _editor.update { it.copy(saving = false, message = "已创建训练日") }
                    refreshList()
                },
                onFailure = { e ->
                    _editor.update { it.copy(saving = false, error = e.message ?: "创建失败") }
                }
            )
        }
    }

    /** 改名 / 改部位 / 改频率（已存在的训练日立即落库）。 */
    fun saveDayBasics() {
        val state = _editor.value
        if (state.isNew) {
            createWithEditorContent()
            return
        }
        if (state.name.isBlank()) {
            _editor.update { it.copy(error = "训练日名称不能为空") }
            return
        }
        viewModelScope.launch {
            repository.renameTrainingDay(state.splitId, state.name)
            repository.setTrainingDayBodyPart(state.splitId, state.bodyPart.name)
            repository.setTrainingDayInterval(state.splitId, state.intervalDays)
            _editor.update { it.copy(message = "已保存") }
            refreshList()
        }
    }

    /** 编辑器里加一个动作（还没落库的行走内存，保存时一起写）。 */
    fun addExercise(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val state = _editor.value
        if (state.rows.any { it.exerciseName == trimmed }) {
            _editor.update { it.copy(message = "「$trimmed」已经在清单里了") }
            return
        }
        val newRow = TrainingDayExerciseEntity(
            splitId = state.splitId,
            position = state.rows.size,
            exerciseName = trimmed,
            targetSets = TrainingDayExerciseEntity.DEFAULT_SETS,
            targetReps = com.example.diettracker.domain.ProgressionEngine
                .defaultRepsSpec(state.objective),
            targetWeightKg = 0.0,
            incrementKg = null
        )
        if (state.isNew) {
            _editor.update { it.copy(rows = it.rows + newRow) }
            return
        }
        viewModelScope.launch {
            repository.addExerciseToDay(state.splitId, trimmed)
            _editor.update { it.copy(rows = repository.getExerciseRows(state.splitId)) }
            refreshList()
        }
    }

    fun removeExercise(rowId: Long) {
        val state = _editor.value
        if (state.isNew) {
            _editor.update { it.copy(rows = it.rows.filterNot { row -> row.id == rowId }) }
            return
        }
        viewModelScope.launch {
            repository.removeExerciseRow(rowId)
            _editor.update { it.copy(rows = repository.getExerciseRows(state.splitId)) }
            refreshList()
        }
    }

    /** 上移 / 下移一个动作行。 */
    fun moveExercise(rowId: Long, delta: Int) {
        val state = _editor.value
        if (state.isNew) {
            val list = state.rows.toMutableList()
            val index = list.indexOfFirst { it.id == rowId }
            if (index < 0) return
            val target = (index + delta).coerceIn(0, list.size - 1)
            if (target == index) return
            val moved = list.removeAt(index)
            list.add(target, moved)
            _editor.update { it.copy(rows = list) }
            return
        }
        viewModelScope.launch {
            repository.moveExerciseRow(rowId, delta)
            _editor.update { it.copy(rows = repository.getExerciseRows(state.splitId)) }
        }
    }

    /** 改一个动作的目标组数 / 每组次数 / 目标重量 / 步进。 */
    fun updateTarget(
        rowId: Long,
        targetSets: Int,
        targetReps: String,
        targetWeightKg: Double,
        incrementKg: Double?
    ) {
        val state = _editor.value
        if (state.isNew) {
            _editor.update {
                it.copy(
                    rows = it.rows.map { row ->
                        if (row.id == rowId) {
                            row.copy(
                                targetSets = targetSets,
                                targetReps = targetReps,
                                targetWeightKg = targetWeightKg,
                                incrementKg = incrementKg
                            )
                        } else {
                            row
                        }
                    }
                )
            }
            return
        }
        viewModelScope.launch {
            repository.updateExerciseTarget(
                rowId = rowId,
                targetSets = targetSets,
                targetReps = targetReps,
                targetWeightKg = targetWeightKg,
                incrementKg = incrementKg
            ).fold(
                onSuccess = {
                    _editor.update { it.copy(rows = repository.getExerciseRows(state.splitId)) }
                },
                onFailure = { e ->
                    _editor.update { it.copy(error = e.message ?: "保存失败") }
                }
            )
        }
    }

    /**
     * 把一个训练日加入某一天（默认今天）。
     *
     * 刻意放在这个 ViewModel 里，因为「我的」里的训练日列表也要能直接加进今天；
     * 今日页自己的加号走 [WorkoutViewModel.addTrainingDayToDate]。
     */
    fun addDayToDate(date: String, splitId: Long) {
        viewModelScope.launch {
            repository.addTrainingDayToDate(date, splitId).fold(
                onSuccess = {
                    _list.update { it.copy(message = "已加入 ${DateUtils.friendlyLabel(date)}") }
                },
                onFailure = { e ->
                    _list.update { it.copy(error = e.message ?: "添加失败") }
                }
            )
        }
    }

    companion object {
        fun factory(repository: TrainingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TrainingDayViewModel(repository) as T
            }
    }
}

/** 今日日程项类型的中文说明，供选择器与历史页使用。 */
fun scheduleKindLabel(kind: ScheduleEntryKind): String = when (kind) {
    ScheduleEntryKind.TRAINING_DAY -> "训练日"
    ScheduleEntryKind.SINGLE_EXERCISE -> "单个动作"
}
