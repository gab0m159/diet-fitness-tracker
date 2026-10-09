package com.example.diettracker.data.repository

import com.example.diettracker.data.db.AppDatabase
import com.example.diettracker.data.db.CustomExerciseEntity
import com.example.diettracker.data.db.CustomStretchEntity
import com.example.diettracker.data.db.DayScheduleEntity
import com.example.diettracker.data.db.ExerciseOutcome
import com.example.diettracker.data.db.ExerciseRecordEntity
import com.example.diettracker.data.db.ExerciseStatusEntity
import com.example.diettracker.data.db.ScheduleEntryKind
import com.example.diettracker.data.db.TrainingDayExerciseEntity
import com.example.diettracker.data.db.TrainingSplitEntity
import com.example.diettracker.data.db.UserProfileEntity
import com.example.diettracker.data.db.UserTrainingGoalEntity
import com.example.diettracker.data.db.WorkoutSessionEntity
import com.example.diettracker.data.model.ActivityLevel
import com.example.diettracker.data.model.GoalMode
import com.example.diettracker.data.model.SessionStatus
import com.example.diettracker.data.model.Sex
import com.example.diettracker.data.model.TrainingIntensity
import com.example.diettracker.data.model.TrainingObjective
import com.example.diettracker.domain.ExerciseLibrary
import com.example.diettracker.domain.FrequencyScheduler
import com.example.diettracker.domain.PlanTemplate
import com.example.diettracker.domain.ProgressionEngine
import com.example.diettracker.domain.StretchGuide
import com.example.diettracker.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 训练模块的唯一数据入口。
 *
 * ## v6 的模型
 *
 *  - **训练日**（[TrainingSplitEntity]）是用户自己创建的单位，带一个「几天一次」
 *    的频率和到期日，到期就自动出现在今日训练栏；
 *  - 每个训练日下有一份**动作行**（[TrainingDayExerciseEntity]），每个动作自带
 *    目标组数 / 每组次数 / 目标重量 / 步进，**只属于这个训练日**，不跨计划共享；
 *  - 今日日程里还可以**手动添加**整个训练日或单个动作（[DayScheduleEntity]）；
 *  - 卡片上的成功 / 失败 / 跳过写进 [ExerciseStatusEntity]，其中只有成功和失败
 *    会出现在往期记录里。
 *
 * 读用 Flow，写用返回 [Result] 的 suspend 函数，和 [DietRepository] 保持一致的形状。
 */
class TrainingRepository(private val db: AppDatabase) {

    private val profileDao = db.userProfileDao()
    private val goalDao = db.userTrainingGoalDao()
    private val splitDao = db.trainingSplitDao()
    private val dayExerciseDao = db.trainingDayExerciseDao()
    private val scheduleDao = db.dayScheduleDao()
    private val sessionDao = db.workoutSessionDao()
    private val recordDao = db.exerciseRecordDao()
    private val statusDao = db.exerciseStatusDao()
    private val customExerciseDao = db.customExerciseDao()
    private val customStretchDao = db.customStretchDao()

    // ------------------------------------------------------------- profile

    fun observeProfile(): Flow<UserProfileEntity> =
        profileDao.observe().map { it ?: UserProfileEntity.default() }

    suspend fun getProfile(): UserProfileEntity =
        profileDao.get() ?: UserProfileEntity.default()

    suspend fun saveProfile(profile: UserProfileEntity): Result<Unit> = runCatching {
        require(profile.heightCm in 80.0..250.0) { "身高请填 80-250 cm" }
        require(profile.weightKg in 25.0..300.0) { "体重请填 25-300 kg" }
        require(profile.age in 10..100) { "年龄请填 10-100" }
        val bf = profile.bodyFatPercent
        if (bf != null) {
            require(bf > 0.0 && bf < 70.0) { "体脂率请填 0-70 之间，留空表示不填" }
        }
        profileDao.upsert(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    /** 当前体重，MET 热量估算要用。 */
    suspend fun currentWeightKg(): Double = getProfile().weightKg

    // ------------------------------------------------------- training goal

    fun observeTrainingGoal(): Flow<UserTrainingGoalEntity> =
        goalDao.observe().map { it ?: UserTrainingGoalEntity.default() }

    suspend fun getTrainingGoal(): UserTrainingGoalEntity =
        goalDao.get() ?: UserTrainingGoalEntity.default()

    /** 只改训练目标（决定默认次数区间与组间休息提示）。 */
    suspend fun setObjective(objective: TrainingObjective): Result<Unit> = runCatching {
        goalDao.upsert(
            getTrainingGoal().copy(
                objective = objective.name,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun objective(): TrainingObjective =
        TrainingObjective.fromStorage(getTrainingGoal().objective)

    // --------------------------------------------------------- 训练日 CRUD

    /** 全部训练日，按列表顺序。 */
    fun observeSplits(): Flow<List<TrainingSplitEntity>> = splitDao.observeAll()

    suspend fun getSplits(): List<TrainingSplitEntity> = splitDao.getAll()

    suspend fun getSplit(id: Long): TrainingSplitEntity? = splitDao.getById(id)

    /** 所有训练日的动作行，按训练日 id 分组。 */
    suspend fun exerciseRowsBySplit(): Map<Long, List<TrainingDayExerciseEntity>> =
        dayExerciseDao.getAll().groupBy { it.splitId }

    suspend fun getExerciseRows(splitId: Long): List<TrainingDayExerciseEntity> =
        dayExerciseDao.getBySplit(splitId)

    fun observeExerciseRows(splitId: Long): Flow<List<TrainingDayExerciseEntity>> =
        dayExerciseDao.observeBySplit(splitId)

    /**
     * 新建一个训练日。新训练日**当天就到期**，这样用户建完可以立刻练。
     */
    suspend fun createTrainingDay(
        name: String,
        bodyPart: String,
        intervalDays: Int = TrainingSplitEntity.DEFAULT_INTERVAL_DAYS
    ): Result<Long> = runCatching {
        require(name.isNotBlank()) { "训练日名称不能为空" }
        val position = splitDao.count()
        splitDao.insert(
            TrainingSplitEntity(
                name = name.trim(),
                bodyPart = bodyPart,
                position = position,
                intervalDays = TrainingSplitEntity.sanitizeInterval(intervalDays),
                nextDueDate = DateUtils.today()
            )
        )
    }

    suspend fun deleteTrainingDay(splitId: Long): Result<Unit> = runCatching {
        splitDao.deleteById(splitId)
        renumberSplits()
    }

    suspend fun renameTrainingDay(splitId: Long, name: String): Result<Unit> = runCatching {
        require(name.isNotBlank()) { "训练日名称不能为空" }
        val split = splitDao.getById(splitId) ?: error("训练日不存在")
        splitDao.update(split.copy(name = name.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun setTrainingDayBodyPart(splitId: Long, bodyPart: String): Result<Unit> =
        runCatching {
            val split = splitDao.getById(splitId) ?: error("训练日不存在")
            splitDao.update(
                split.copy(bodyPart = bodyPart, updatedAt = System.currentTimeMillis())
            )
        }

    /** 设置「几天一次」。 */
    suspend fun setTrainingDayInterval(splitId: Long, intervalDays: Int): Result<Unit> =
        runCatching {
            val split = splitDao.getById(splitId) ?: error("训练日不存在")
            splitDao.update(
                split.copy(
                    intervalDays = TrainingSplitEntity.sanitizeInterval(intervalDays),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

    /** 重排 position，保证 0..n-1 连续。 */
    private suspend fun renumberSplits() {
        val remaining = splitDao.getAll()
        splitDao.deleteAll()
        splitDao.insertAll(
            remaining.mapIndexed { index, entity -> entity.copy(position = index) }
        )
    }

    // --------------------------------------------------- 训练日里的动作行

    /** 往训练日里加一个动作，目标值用训练目标给的默认区间。 */
    suspend fun addExerciseToDay(splitId: Long, exerciseName: String): Result<Unit> =
        runCatching {
            val name = exerciseName.trim()
            require(name.isNotEmpty()) { "动作名称不能为空" }
            val existing = dayExerciseDao.getBySplit(splitId)
            if (existing.any { it.exerciseName == name }) return@runCatching
            dayExerciseDao.insert(
                TrainingDayExerciseEntity(
                    splitId = splitId,
                    position = existing.size,
                    exerciseName = name,
                    targetSets = TrainingDayExerciseEntity.DEFAULT_SETS,
                    targetReps = ProgressionEngine.defaultRepsSpec(objective()),
                    targetWeightKg = 0.0,
                    incrementKg = null
                )
            )
        }

    suspend fun removeExerciseRow(rowId: Long): Result<Unit> = runCatching {
        val row = dayExerciseDao.getById(rowId) ?: return@runCatching
        dayExerciseDao.deleteById(rowId)
        renumberRows(row.splitId)
    }

    /** 上移 / 下移一行（[delta] = ±1）。 */
    suspend fun moveExerciseRow(rowId: Long, delta: Int): Result<Unit> = runCatching {
        val row = dayExerciseDao.getById(rowId) ?: return@runCatching
        val list = dayExerciseDao.getBySplit(row.splitId).toMutableList()
        val index = list.indexOfFirst { it.id == rowId }
        if (index < 0) return@runCatching
        val target = (index + delta).coerceIn(0, list.size - 1)
        if (target == index) return@runCatching
        val moved = list.removeAt(index)
        list.add(target, moved)
        dayExerciseDao.replaceForSplit(row.splitId, list)
    }

    private suspend fun renumberRows(splitId: Long) {
        dayExerciseDao.replaceForSplit(splitId, dayExerciseDao.getBySplit(splitId))
    }

    /** 保存一个动作在训练日里的目标组数 / 次数 / 重量 / 步进。 */
    suspend fun updateExerciseTarget(
        rowId: Long,
        targetSets: Int,
        targetReps: String,
        targetWeightKg: Double,
        incrementKg: Double?
    ): Result<Unit> = runCatching {
        require(targetSets in 1..20) { "目标组数请填 1-20" }
        require(targetWeightKg >= 0.0) { "目标重量不能是负数" }
        if (incrementKg != null) {
            require(incrementKg >= 0.0) { "步进值不能是负数" }
        }
        val row = dayExerciseDao.getById(rowId) ?: error("动作不存在")
        dayExerciseDao.update(
            row.copy(
                targetSets = targetSets,
                targetReps = targetReps.trim().ifBlank {
                    TrainingDayExerciseEntity.DEFAULT_REPS
                },
                targetWeightKg = targetWeightKg,
                incrementKg = incrementKg,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // -------------------------------------------------------- 今日日程

    /** 把整个训练日加进某一天的日程。重复添加是空操作。 */
    suspend fun addTrainingDayToDate(date: String, splitId: Long): Result<Unit> = runCatching {
        val split = splitDao.getById(splitId) ?: error("训练日不存在")
        if (scheduleDao.findDayForSplit(date, splitId) != null) return@runCatching
        scheduleDao.insert(
            DayScheduleEntity(
                date = date,
                position = scheduleDao.countByDate(date),
                kind = ScheduleEntryKind.TRAINING_DAY.name,
                splitId = split.id,
                splitName = split.name
            )
        )
    }

    /** 单独加一个动作进某一天，不依赖任何训练日。 */
    suspend fun addSingleExerciseToDate(
        date: String,
        exerciseName: String,
        targetSets: Int = TrainingDayExerciseEntity.DEFAULT_SETS,
        targetReps: String = ProgressionEngine.defaultRepsSpec(TrainingObjective.HYPERTROPHY),
        targetWeightKg: Double = 0.0,
        incrementKg: Double? = null
    ): Result<Unit> = runCatching {
        val name = exerciseName.trim()
        require(name.isNotEmpty()) { "动作名称不能为空" }
        require(targetSets in 1..20) { "组数请填 1-20" }
        scheduleDao.insert(
            DayScheduleEntity(
                date = date,
                position = scheduleDao.countByDate(date),
                kind = ScheduleEntryKind.SINGLE_EXERCISE.name,
                splitId = null,
                splitName = "",
                exerciseName = name,
                targetSets = targetSets,
                targetReps = targetReps.trim().ifBlank {
                    TrainingDayExerciseEntity.DEFAULT_REPS
                },
                targetWeightKg = targetWeightKg,
                incrementKg = incrementKg
            )
        )
    }

    /** 把一项从当天日程里移除（不写任何结果）。 */
    suspend fun removeScheduleEntry(entryId: Long): Result<Unit> = runCatching {
        scheduleDao.deleteById(entryId)
    }

    /**
     * 训练日整体跳过：其下所有动作标记为跳过，训练日从今天移除，到期日前进一个
     * 间隔，往期记录里不会出现这些动作。
     */
    suspend fun skipTrainingDay(date: String, splitId: Long): Result<Unit> = runCatching {
        val split = splitDao.getById(splitId) ?: error("训练日不存在")
        val rows = dayExerciseDao.getBySplit(splitId)
        val existing = statusDao.getByDate(date)
            .filter { it.sourceKind == ScheduleEntryKind.TRAINING_DAY.name }
            .associateBy { it.sourceId }

        rows.forEachIndexed { index, row ->
            if (existing.containsKey(row.id)) return@forEachIndexed
            val target = row.targetWeightKg
            statusDao.upsert(
                ExerciseStatusEntity(
                    sessionId = sessionDao.findForDate(date)?.id,
                    date = date,
                    exerciseName = row.exerciseName,
                    outcome = ExerciseOutcome.SKIPPED.name,
                    sourceKind = ScheduleEntryKind.TRAINING_DAY.name,
                    sourceId = row.id,
                    weightBeforeKg = target,
                    weightAfterKg = target,
                    targetSets = row.targetSets,
                    targetReps = row.targetReps,
                    targetWeightKg = target,
                    sourceLabel = split.name,
                    position = index
                )
            )
        }

        splitDao.update(FrequencyScheduler.afterHandled(split, date))
        scheduleDao.deleteForSplit(date, splitId)
    }

    /**
     * 延期：所有训练日统一往后顺延一天，今天手动加进来的日程整体挪到明天。
     *
     * 不写失败也不写跳过，目标重量和往期记录都不受影响。
     */
    suspend fun postponeAll(date: String = DateUtils.today()): Result<Unit> = runCatching {
        val splits = splitDao.getAll()
        FrequencyScheduler.postponeAll(splits, date).forEach { splitDao.update(it) }
        scheduleDao.moveDate(date, DateUtils.plusDays(date, 1))
    }

    // ------------------------------------------------------------- today

    /**
     * 解析某一天该练什么。
     *
     * 顺序：
     *  1. 用户手动加进来的训练日（按加入顺序）；
     *  2. 按频率到期、自动出现的训练日；
     *  3. 当天已经练过（有成功/失败结果）的训练日，保留在列表里让人看到结果；
     *  4. 单独添加的动作卡片。
     */
    suspend fun resolveToday(date: String = DateUtils.today()): TodayPlan {
        val objective = objective()
        val splits = splitDao.getAll()
        val rowsBySplit = exerciseRowsBySplit()
        val manual = scheduleDao.getByDate(date)
        val outcomes = statusDao.getByDate(date)
        val outcomeBySource = outcomes.associateBy { it.sourceKind to it.sourceId }
        val lastByExercise = lastRecordedByExercise(
            (rowsBySplit.values.flatten().map { it.exerciseName } +
                manual.map { it.exerciseName }).distinct()
        )

        val manualDayEntries = manual.filter { it.isTrainingDay }
        val manualSplitIds = manualDayEntries.mapNotNull { it.splitId }.toSet()
        val dueSplits = FrequencyScheduler.dueSplits(splits, date)
        val dueIds = dueSplits.map { it.id }.toSet()

        val groups = mutableListOf<TodayTrainingGroup>()

        // 1. 手动加入的训练日
        manualDayEntries.forEach { entry ->
            val split = entry.splitId?.let { id -> splits.firstOrNull { it.id == id } }
            groups += TodayTrainingGroup(
                key = "manual-${entry.id}",
                kind = TodayGroupKind.MANUAL_DAY,
                splitId = split?.id,
                title = split?.name ?: entry.splitName.ifBlank { "训练日" },
                subtitle = if (split == null) {
                    "这个训练日已被删除"
                } else {
                    FrequencyScheduler.describeInterval(split.intervalDays)
                },
                manualEntryId = entry.id,
                cards = split?.let {
                    buildCards(it, rowsBySplit[it.id].orEmpty(), outcomeBySource, lastByExercise)
                }.orEmpty()
            )
        }

        // 2. 频率到期的训练日
        dueSplits.filter { it.id !in manualSplitIds }.forEach { split ->
            groups += TodayTrainingGroup(
                key = "due-${split.id}",
                kind = TodayGroupKind.DUE_DAY,
                splitId = split.id,
                title = split.name,
                subtitle = FrequencyScheduler.describeInterval(split.intervalDays),
                cards = buildCards(
                    split,
                    rowsBySplit[split.id].orEmpty(),
                    outcomeBySource,
                    lastByExercise
                )
            )
        }

        // 3. 今天已经练过的（只保留真的练了成功/失败的，整体跳过的不再显示）
        splits.filter { split ->
            val rowIds = rowsBySplit[split.id].orEmpty().map { it.id }.toSet()
            split.lastDoneDate == date &&
                split.id !in manualSplitIds &&
                split.id !in dueIds &&
                outcomes.any { status ->
                    status.outcome in ExerciseOutcome.recordedInHistory &&
                        status.sourceKind == ScheduleEntryKind.TRAINING_DAY.name &&
                        status.sourceId in rowIds
                }
        }.forEach { split ->
            groups += TodayTrainingGroup(
                key = "done-${split.id}",
                kind = TodayGroupKind.HANDLED_DAY,
                splitId = split.id,
                title = split.name,
                subtitle = "今天已练 · 下次 " +
                    FrequencyScheduler.nextDueLabel(split, date),
                cards = buildCards(
                    split,
                    rowsBySplit[split.id].orEmpty(),
                    outcomeBySource,
                    lastByExercise
                )
            )
        }

        // 4. 单独添加的动作
        manual.filter { !it.isTrainingDay }.forEach { entry ->
            val recorded = outcomeBySource[
                ScheduleEntryKind.SINGLE_EXERCISE.name to entry.id
            ]
            groups += TodayTrainingGroup(
                key = "single-${entry.id}",
                kind = TodayGroupKind.SINGLE,
                splitId = null,
                title = "单独动作",
                subtitle = "",
                manualEntryId = entry.id,
                cards = listOf(
                    TodayExerciseCard(
                        sourceKind = ScheduleEntryKind.SINGLE_EXERCISE,
                        sourceId = entry.id,
                        splitId = null,
                        splitName = "",
                        exerciseName = entry.exerciseName,
                        targetSets = entry.targetSets,
                        targetReps = entry.targetReps,
                        targetWeightKg = recorded?.weightAfterKg?.takeIf { it > 0.0 }
                            ?: entry.targetWeightKg,
                        incrementKg = entry.incrementKg ?: 0.0,
                        outcome = recorded?.outcomeValue,
                        resultText = recorded?.changeLabel.orEmpty(),
                        lastSummary = lastByExercise[entry.exerciseName]?.let {
                            "${it.date} ${it.outcomeValue.label} " +
                                "${trim(it.weightAfterKg)}kg"
                        }
                    )
                )
            )
        }

        return TodayPlan(
            date = date,
            groups = groups,
            objective = objective,
            session = sessionDao.findForDate(date),
            hasTrainingDays = splits.isNotEmpty(),
            dueCount = dueSplits.size + manualDayEntries.size,
            isToday = date == DateUtils.today()
        )
    }

    private fun buildCards(
        split: TrainingSplitEntity,
        rows: List<TrainingDayExerciseEntity>,
        outcomeBySource: Map<Pair<String, Long>, ExerciseStatusEntity>,
        lastByExercise: Map<String, ExerciseStatusEntity>
    ): List<TodayExerciseCard> = rows.mapIndexed { index, row ->
        val recorded = outcomeBySource[ScheduleEntryKind.TRAINING_DAY.name to row.id]
        TodayExerciseCard(
            sourceKind = ScheduleEntryKind.TRAINING_DAY,
            sourceId = row.id,
            splitId = split.id,
            splitName = split.name,
            exerciseName = row.exerciseName,
            targetSets = row.targetSets,
            targetReps = row.targetReps,
            // 上一次成功/失败已经把目标重量写进行里了，所以这里直接用行上的值。
            targetWeightKg = row.targetWeightKg,
            // 步进默认 0 = 不自动加重；没设过就是 0，不再回落到动作库的按部位默认值。
            incrementKg = row.incrementKg ?: 0.0,
            outcome = recorded?.outcomeValue,
            resultText = recorded?.changeLabel.orEmpty(),
            lastSummary = lastByExercise[row.exerciseName]?.let {
                "${it.date} ${it.outcomeValue.label} ${trim(it.weightAfterKg)}kg"
            },
            position = index
        )
    }

    /** 每个动作最近一次成功/失败的成绩，批量查一次。 */
    private suspend fun lastRecordedByExercise(
        names: List<String>
    ): Map<String, ExerciseStatusEntity> {
        if (names.isEmpty()) return emptyMap()
        return statusDao.recentForNames(names)
            .groupBy { it.exerciseName }
            .mapValues { (_, list) -> list.first() }
    }

    // -------------------------------------------------------- 卡片结果

    /**
     * 记录一张卡片的结果。
     *
     *  - 成功：目标重量按**这个训练日里这个动作**的步进值上调；
     *  - 失败：目标重量不变（永不自动降重）；
     *  - 跳过：写一行跳过标记（好让卡片从待办里划掉），但不改重量、不进往期。
     *
     * 切换结果时会先回到这张卡片原先的目标重量再重新计算，所以反复点不会让重量
     * 越滚越高。
     */
    suspend fun recordOutcome(
        date: String,
        card: TodayExerciseCard,
        outcome: ExerciseOutcome
    ): Result<ProgressionEngine.Outcome> = runCatching {
        val existing = statusDao.find(date, card.sourceKind.name, card.sourceId)
        val baseWeight = existing?.weightBeforeKg ?: card.targetWeightKg

        val result = when (outcome) {
            ExerciseOutcome.SUCCESS ->
                ProgressionEngine.onSuccess(baseWeight, card.incrementKg)

            ExerciseOutcome.FAILURE -> ProgressionEngine.onFailure(baseWeight)
            ExerciseOutcome.SKIPPED -> ProgressionEngine.onSkip(baseWeight)
        }

        // 目标重量永远以本次计算出的绝对值为准，所以不需要回滚旧结果。
        if (result.weightKg > 0.0) {
            writeTargetWeight(card, result.weightKg)
        }

        statusDao.upsert(
            ExerciseStatusEntity(
                sessionId = sessionDao.findForDate(date)?.id,
                date = date,
                exerciseName = card.exerciseName,
                outcome = outcome.name,
                sourceKind = card.sourceKind.name,
                sourceId = card.sourceId,
                weightBeforeKg = baseWeight,
                weightAfterKg = result.weightKg,
                targetSets = card.targetSets,
                targetReps = card.targetReps,
                targetWeightKg = baseWeight,
                sourceLabel = card.splitName.ifBlank { "单独动作" },
                position = card.position
            )
        )

        if (card.splitId != null) {
            completeDayIfFinished(date, card.splitId)
        }
        result
    }

    /** 撤销一张卡片的结果，并把目标重量还原到按按钮之前的值。 */
    suspend fun clearOutcome(date: String, card: TodayExerciseCard): Result<Unit> =
        runCatching {
            val existing = statusDao.find(date, card.sourceKind.name, card.sourceId)
                ?: return@runCatching
            if (existing.weightBeforeKg > 0.0) {
                writeTargetWeight(card, existing.weightBeforeKg)
            }
            statusDao.deleteFor(date, card.sourceKind.name, card.sourceId)
        }

    /** 把新的目标重量写到卡片对应的来源行上。 */
    private suspend fun writeTargetWeight(card: TodayExerciseCard, weightKg: Double) {
        when (card.sourceKind) {
            ScheduleEntryKind.TRAINING_DAY -> {
                val row = dayExerciseDao.getById(card.sourceId) ?: return
                dayExerciseDao.update(
                    row.copy(targetWeightKg = weightKg, updatedAt = System.currentTimeMillis())
                )
            }

            ScheduleEntryKind.SINGLE_EXERCISE -> {
                val entry = scheduleDao.getById(card.sourceId) ?: return
                scheduleDao.update(entry.copy(targetWeightKg = weightKg))
            }
        }
    }

    /**
     * 一个训练日当天的动作全部有结果后，把到期日推进一个间隔，并移除今日手动项。
     */
    private suspend fun completeDayIfFinished(date: String, splitId: Long) {
        val rows = dayExerciseDao.getBySplit(splitId)
        if (rows.isEmpty()) return
        val markedIds = statusDao.getByDate(date)
            .filter { it.sourceKind == ScheduleEntryKind.TRAINING_DAY.name }
            .map { it.sourceId }
            .toSet()
        if (!rows.all { it.id in markedIds }) return

        val split = splitDao.getById(splitId) ?: return
        splitDao.update(FrequencyScheduler.afterHandled(split, date))
        scheduleDao.deleteForSplit(date, splitId)
    }

    // ----------------------------------------------------------- sessions

    fun observeSessionsForDate(date: String): Flow<List<WorkoutSessionEntity>> =
        sessionDao.observeByDate(date)

    fun observeRecentSessions(limit: Int = 20): Flow<List<WorkoutSessionEntity>> =
        sessionDao.observeRecentCompleted(limit)

    /** 当天消耗热量，饮食页的「运动消耗」读它。 */
    fun observeBurnedKcal(date: String): Flow<Double> = sessionDao.observeBurnedKcal(date)

    suspend fun getBurnedKcal(date: String): Double = sessionDao.getBurnedKcal(date)

    fun observeSession(id: Long): Flow<WorkoutSessionEntity?> = sessionDao.observeById(id)

    suspend fun getSession(id: Long): WorkoutSessionEntity? = sessionDao.getById(id)

    fun observeRecords(sessionId: Long): Flow<List<ExerciseRecordEntity>> =
        recordDao.observeBySession(sessionId)

    suspend fun getRecords(sessionId: Long): List<ExerciseRecordEntity> =
        recordDao.getBySession(sessionId)

    /**
     * 取当天的运动记录，没有就按 MET 估算建一条。
     *
     * 一天只有一条：时长和热量是按天累计的，和今天练了几个训练日无关。
     */
    suspend fun ensureSession(
        date: String,
        title: String = ""
    ): Result<Long> = runCatching {
        val existing = sessionDao.findForDate(date)
        if (existing != null) {
            if (title.isNotBlank() && existing.title != title) {
                sessionDao.update(
                    existing.copy(title = title, updatedAt = System.currentTimeMillis())
                )
            }
            return@runCatching existing.id
        }
        val weight = currentWeightKg()
        val intensity = defaultIntensity(objective())
        sessionDao.insert(
            WorkoutSessionEntity(
                date = date,
                title = title,
                status = SessionStatus.IN_PROGRESS.name,
                durationMinutes = DEFAULT_DURATION_MINUTES,
                intensity = intensity.name,
                burnedKcal = round1(
                    intensity.met * weight * (DEFAULT_DURATION_MINUTES / 60.0)
                )
            )
        )
    }

    private fun defaultIntensity(objective: TrainingObjective): TrainingIntensity =
        when (objective) {
            TrainingObjective.POWERLIFTING -> TrainingIntensity.HEAVY
            TrainingObjective.HYPERTROPHY -> TrainingIntensity.HEAVY
            TrainingObjective.PHYSIQUE -> TrainingIntensity.MODERATE
        }

    /** 更新时长 / 强度 / 热量（热量可手改）。 */
    suspend fun updateSessionMetrics(
        sessionId: Long,
        durationMinutes: Int,
        intensity: TrainingIntensity,
        burnedKcalOverride: Double?
    ): Result<Unit> = runCatching {
        val session = sessionDao.getById(sessionId) ?: error("训练记录不存在")
        val weight = currentWeightKg()
        val burn = burnedKcalOverride ?: session.estimateBurn(intensity.met, weight)
        sessionDao.update(
            session.copy(
                durationMinutes = durationMinutes.coerceAtLeast(0),
                intensity = intensity.name,
                burnedKcal = round1(burn),
                burnOverridden = burnedKcalOverride != null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** 保存一次训练里每个动作的每组次数明细。 */
    suspend fun saveRecords(
        sessionId: Long,
        records: List<ExerciseRecordEntity>
    ): Result<Unit> = runCatching {
        recordDao.replaceSessionRecords(
            sessionId,
            records.mapIndexed { index, record ->
                record.copy(sessionId = sessionId, position = index)
            }
        )
    }

    /** 完成当天训练（热量才会计入「运动消耗」）。 */
    suspend fun completeSession(sessionId: Long): Result<Unit> = runCatching {
        val session = sessionDao.getById(sessionId) ?: error("训练记录不存在")
        sessionDao.update(
            session.copy(
                status = SessionStatus.COMPLETED.name,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSession(sessionId: Long): Result<Unit> = runCatching {
        recordDao.deleteBySession(sessionId)
        sessionDao.deleteById(sessionId)
    }

    // ------------------------------------------------------------ 往期记录

    /** 往期训练记录：只有成功和失败，跳过的不在里面。 */
    fun observeHistory(limit: Int = 500): Flow<List<ExerciseStatusEntity>> =
        statusDao.observeHistory(limit)

    suspend fun getHistory(limit: Int = 500): List<ExerciseStatusEntity> =
        statusDao.getHistory(limit)

    /** 某个动作最近一次的成绩，用于卡片上的「上次」提示。 */
    suspend fun lastOutcomeFor(exerciseName: String): ExerciseStatusEntity? =
        statusDao.latestForExercise(exerciseName)

    /** 某个动作的历史最好重量。 */
    suspend fun personalBest(exerciseName: String): Double =
        recordDao.personalBest(exerciseName)

    /** 每个动作最近一次「每组次数」明细。 */
    suspend fun lastPerformance(exerciseName: String): ExerciseRecordEntity? =
        recordDao.latestForExercise(exerciseName)

    /** 本周（最近 7 天）消耗热量合计。 */
    fun observeBurnedKcalSince(fromDate: String): Flow<Double> =
        sessionDao.observeBurnedKcalSince(fromDate)

    // ------------------------------------------------------- 预设计划

    fun planTemplates(): List<PlanTemplate> = com.example.diettracker.domain.PlanTemplates.all()

    /**
     * 套用一个预设：整体替换现有训练日，初始到期日按顺序错开一天，
     * 这样它们不会全部挤在今天。
     */
    suspend fun adoptTemplate(
        template: PlanTemplate,
        objective: TrainingObjective
    ): Result<Unit> = runCatching {
        goalDao.upsert(
            getTrainingGoal().copy(
                splitType = template.type.name,
                objective = objective.name,
                updatedAt = System.currentTimeMillis()
            )
        )
        splitDao.deleteAll()
        val today = DateUtils.today()
        template.trainingDays.forEachIndexed { index, day ->
            val splitId = splitDao.insert(
                TrainingSplitEntity(
                    name = day.name,
                    bodyPart = day.bodyPart.name,
                    position = index,
                    intervalDays = TrainingSplitEntity.sanitizeInterval(
                        template.defaultIntervalDays
                    ),
                    nextDueDate = FrequencyScheduler.initialNextDueDate(today, index)
                )
            )
            dayExerciseDao.insertAll(
                day.exerciseNames.mapIndexed { rowIndex, exerciseName ->
                    TrainingDayExerciseEntity(
                        splitId = splitId,
                        position = rowIndex,
                        exerciseName = exerciseName,
                        targetSets = TrainingDayExerciseEntity.DEFAULT_SETS,
                        targetReps = ProgressionEngine.defaultRepsSpec(objective),
                        targetWeightKg = 0.0,
                        incrementKg = null
                    )
                }
            )
        }
    }

    // ------------------------------------------- custom exercise library

    fun observeCustomExercises(): Flow<List<CustomExerciseEntity>> =
        customExerciseDao.observeAll()

    suspend fun getCustomExercises(): List<CustomExerciseEntity> = customExerciseDao.getAll()

    suspend fun saveCustomExercise(entity: CustomExerciseEntity): Result<Unit> = runCatching {
        require(entity.name.isNotBlank()) { "动作名称不能为空" }
        val duplicate = customExerciseDao.countWithName(entity.name.trim(), entity.id) > 0
        require(!duplicate) { "已存在同名自建动作" }
        customExerciseDao.insert(entity.copy(name = entity.name.trim()))
    }

    suspend fun deleteCustomExercise(entity: CustomExerciseEntity): Result<Unit> =
        runCatching { customExerciseDao.delete(entity) }

    /** 所有可选动作名：内置 + 自建。 */
    suspend fun allExerciseNames(): List<String> =
        (ExerciseLibrary.allNames() + customExerciseDao.getAll().map { it.name })
            .distinct()
            .sorted()

    // -------------------------------------------- custom stretch library

    fun observeCustomStretches(): Flow<List<CustomStretchEntity>> =
        customStretchDao.observeAll()

    suspend fun getCustomStretches(): List<CustomStretchEntity> = customStretchDao.getAll()

    suspend fun saveCustomStretch(entity: CustomStretchEntity): Result<Unit> = runCatching {
        require(entity.name.isNotBlank()) { "拉伸名称不能为空" }
        require(entity.targetMuscle.isNotBlank()) { "请填写目标肌群" }
        val duplicate = customStretchDao.countWithName(entity.name.trim(), entity.id) > 0
        require(!duplicate) { "已存在同名自建拉伸" }
        customStretchDao.insert(entity.copy(name = entity.name.trim()))
    }

    suspend fun deleteCustomStretch(entity: CustomStretchEntity): Result<Unit> =
        runCatching { customStretchDao.delete(entity) }

    /** 全部拉伸：内置 + 自建。 */
    suspend fun getAllStretches(): List<StretchGuide> {
        val custom = customStretchDao.getAll().map { it.toGuide() }
        return ExerciseLibrary.allStretches() + custom
    }

    /** 一组动作做完之后的拉伸指导（内置 + 自建动作关联的）。 */
    suspend fun stretchesForExercises(exerciseNames: List<String>): List<StretchGuide> {
        val builtIn = ExerciseLibrary.stretchesFor(exerciseNames, limit = 6)
        val linked = customExerciseDao.getAll()
            .filter { it.name in exerciseNames }
            .flatMap { it.stretchList }
        if (linked.isEmpty()) return builtIn

        val byName = getAllStretches().associateBy { it.name }
        val extra = linked.mapNotNull { byName[it] }
        return (builtIn + extra).distinctBy { it.name }.take(8)
    }

    private fun trim(value: Double): String {
        val rounded = Math.round(value * 10.0) / 10.0
        return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
        else rounded.toString()
    }

    private fun round1(value: Double): Double = Math.round(value * 10.0) / 10.0

    // ------------------------------------------------------------- helpers

    suspend fun activityOf(profile: UserProfileEntity): ActivityLevel =
        ActivityLevel.fromStorage(profile.activityLevel)

    suspend fun goalModeOf(profile: UserProfileEntity): GoalMode =
        GoalMode.fromStorage(profile.goalMode)

    suspend fun sexOf(profile: UserProfileEntity): Sex = Sex.fromStorage(profile.sex)

    companion object {
        const val DEFAULT_DURATION_MINUTES = 60
    }
}

/** 今日训练栏里的一张动作卡片。 */
data class TodayExerciseCard(
    val sourceKind: ScheduleEntryKind,
    val sourceId: Long,
    val splitId: Long?,
    val splitName: String,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: String,
    val targetWeightKg: Double,
    val incrementKg: Double,
    val outcome: ExerciseOutcome? = null,
    val resultText: String = "",
    val lastSummary: String? = null,
    val position: Int = 0
) {
    /** LazyColumn 的稳定 key：来源类型 + 来源行 id。 */
    val key: String get() = "${sourceKind.name}-$sourceId"

    val repsLabel: String get() = ProgressionEngine.repsLabel(targetReps)

    /** 「4 组 × 8-12 次」 */
    val targetLabel: String get() = "$targetSets 组 × $repsLabel"

    /**
     * 成功后会用到的下一个重量。
     *
     * 步进为 0（默认）或目标重量还没填时返回 0，表示「没有下一个」。
     */
    val nextWeightKg: Double
        get() = if (targetWeightKg > 0.0 && incrementKg > 0.0) {
            ProgressionEngine.roundToStep(targetWeightKg + incrementKg, incrementKg)
        } else {
            0.0
        }

    /** 「本次 60kg → 下次 62.5kg」；步进为 0 或未设定时只显示本次。 */
    val weightProgressLabel: String
        get() = when {
            targetWeightKg <= 0.0 -> "目标重量未设定"
            nextWeightKg <= 0.0 -> "本次 ${trim(targetWeightKg)}kg"
            else -> "本次 ${trim(targetWeightKg)}kg → 下次 ${trim(nextWeightKg)}kg"
        }

    private fun trim(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
        else rounded.toString()
    }
}

/** 今日训练栏里的一个分组（一个训练日，或「单独动作」）。 */
enum class TodayGroupKind {
    /** 用户手动加进今天的训练日。 */
    MANUAL_DAY,

    /** 按频率到期自动出现的训练日。 */
    DUE_DAY,

    /** 今天已经练过、保留展示结果的训练日。 */
    HANDLED_DAY,

    /** 单独添加的动作卡片。 */
    SINGLE
}

data class TodayTrainingGroup(
    val key: String,
    val kind: TodayGroupKind,
    val splitId: Long?,
    val title: String,
    val subtitle: String = "",
    val manualEntryId: Long? = null,
    val cards: List<TodayExerciseCard> = emptyList()
) {
    /** 这个分组能不能整体跳过 / 延期（单独动作不行）。 */
    val canSkipWholeDay: Boolean
        get() = kind == TodayGroupKind.DUE_DAY || kind == TodayGroupKind.MANUAL_DAY

    val doneCount: Int get() = cards.count { it.outcome != null }

    val pendingCount: Int get() = cards.size - doneCount

    val allDone: Boolean get() = cards.isNotEmpty() && doneCount == cards.size

    /** 「动作 2/5」 */
    val progressLabel: String get() = "动作 $doneCount/${cards.size}"

    /** 每个动作「成功」累加起来的重量，用于分组标题上的小结。 */
    val gainedKg: Double
        get() = cards.sumOf { card ->
            val recorded = card.outcome
            if (recorded == ExerciseOutcome.SUCCESS) card.incrementKg else 0.0
        }
}

/** 「今天该练什么」的完整结果。 */
data class TodayPlan(
    val date: String,
    val groups: List<TodayTrainingGroup> = emptyList(),
    val objective: TrainingObjective = TrainingObjective.HYPERTROPHY,
    val session: WorkoutSessionEntity? = null,
    val hasTrainingDays: Boolean = false,
    val dueCount: Int = 0,
    val isToday: Boolean = true
) {
    val hasAnything: Boolean get() = groups.isNotEmpty()

    val totalCards: Int get() = groups.sumOf { it.cards.size }

    val doneCards: Int get() = groups.sumOf { it.doneCount }

    /** 「胸 + 三头、背 + 二头」 */
    val titleLine: String get() = groups.joinToString("、") { it.title }
}

/** 转成共享的领域类型，供拉伸卡片渲染。 */
fun CustomStretchEntity.toGuide(): StretchGuide = StretchGuide(
    name = name,
    targetMuscle = targetMuscle,
    holdSecondsMin = holdSecondsMin,
    holdSecondsMax = holdSecondsMax,
    sets = sets,
    howTo = howTo,
    description = description,
    source = source.ifBlank { "用户自建" },
    mediaUrl = mediaUrl.ifBlank { null }
)
