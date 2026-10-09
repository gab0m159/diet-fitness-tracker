package com.example.diettracker.data.repository

import com.example.diettracker.data.db.ActivityLogEntity
import com.example.diettracker.data.db.AppDatabase
import com.example.diettracker.data.db.CustomExerciseEntity
import com.example.diettracker.data.db.CustomStretchEntity
import com.example.diettracker.data.db.ExerciseLogEntity
import com.example.diettracker.data.db.UserProfileEntity
import com.example.diettracker.data.model.ActivityLevel
import com.example.diettracker.data.model.GoalMode
import com.example.diettracker.data.model.Sex
import com.example.diettracker.domain.ExerciseLibrary
import com.example.diettracker.domain.SportInfo
import com.example.diettracker.domain.SportLibrary
import com.example.diettracker.domain.StretchGuide
import com.example.diettracker.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 运动模块的唯一数据入口。
 *
 * ## v7 的模型
 *
 * 用户每天直接往当天加**运动项目**（跑步、游泳、撸铁…），填时长，系统按 MET 估算
 * 消耗：
 *
 *  - [ActivityLogEntity] 一天里的一项运动：`运动 + 时长 + 热量`；
 *  - [ExerciseLogEntity] 撸铁那一项下面挂的动作卡片：`动作 + 重量 + 组数 + 次数`。
 *
 * 没有训练日、没有频率、没有到期日、没有成功 / 失败 / 跳过、没有步进。
 *
 * 消耗**只用于显示**当天运动量，不会加回可摄入额度。读用 Flow、写返回 Result，
 * 与 [DietRepository] 形状一致。
 */
class ActivityRepository(private val db: AppDatabase) {

    private val profileDao = db.userProfileDao()
    private val activityDao = db.activityLogDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val customExerciseDao = db.customExerciseDao()
    private val customStretchDao = db.customStretchDao()

    // ------------------------------------------------------------- 身体数据

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

    /** 当前体重，MET 估算要用。 */
    suspend fun currentWeightKg(): Double = getProfile().weightKg

    // ------------------------------------------------------------- 运动库

    /** 内置运动库，按分组返回。 */
    fun sportGroups(): List<Pair<com.example.diettracker.domain.SportCategory, List<SportInfo>>> =
        SportLibrary.grouped()

    fun findSport(key: String): SportInfo? = SportLibrary.find(key)

    /** 按 MET 估算热量。 */
    fun estimateKcal(met: Double, weightKg: Double, minutes: Int): Double =
        SportLibrary.estimateKcal(met, weightKg, minutes)

    // --------------------------------------------------------- 当天运动

    /** 某一天的运动项目（按加入顺序）。 */
    fun observeActivities(date: String): Flow<List<ActivityLogEntity>> =
        activityDao.observeByDate(date)

    suspend fun getActivities(date: String): List<ActivityLogEntity> =
        activityDao.getByDate(date)

    /** 当天消耗合计，饮食页的「运动消耗」读它。 */
    fun observeBurnedKcal(date: String): Flow<Double> = activityDao.observeBurnedKcal(date)

    suspend fun getBurnedKcal(date: String): Double = activityDao.getBurnedKcal(date)

    /** 最近若干天的消耗合计（首页小结）。 */
    fun observeBurnedKcalSince(fromDate: String): Flow<Double> =
        activityDao.observeBurnedKcalSince(fromDate)

    /**
     * 把一项运动加进某一天。
     *
     * 热量按 MET 先算好存进去（体重取当前值），之后改体重不会追溯修改历史。
     */
    suspend fun addSport(
        date: String,
        sportKey: String,
        durationMinutes: Int? = null
    ): Result<Long> = runCatching {
        val sport = SportLibrary.find(sportKey) ?: error("找不到这个运动项目")
        val minutes = (durationMinutes ?: sport.defaultMinutes).coerceIn(1, 600)
        val weight = currentWeightKg()
        activityDao.insert(
            ActivityLogEntity(
                date = date,
                position = activityDao.countByDate(date),
                sportKey = sport.key,
                sportName = sport.name,
                met = sport.met,
                durationMinutes = minutes,
                burnedKcal = SportLibrary.estimateKcal(sport.met, weight, minutes),
                burnOverridden = false
            )
        )
    }

    /** 改一项运动的时长；热量跟着重算（除非用户之前手改过热量）。 */
    suspend fun updateDuration(
        activityId: Long,
        minutes: Int,
        weightKg: Double? = null
    ): Result<Unit> = runCatching {
        val activity = activityDao.getById(activityId) ?: error("运动记录不存在")
        val safeMinutes = minutes.coerceIn(1, 600)
        val weight = weightKg ?: currentWeightKg()
        activityDao.update(
            activity.copy(
                durationMinutes = safeMinutes,
                burnedKcal = if (activity.burnOverridden) {
                    activity.burnedKcal
                } else {
                    SportLibrary.estimateKcal(activity.met, weight, safeMinutes)
                }
            )
        )
    }

    /** 手动覆盖热量。 */
    suspend fun overrideBurn(activityId: Long, kcal: Double?): Result<Unit> = runCatching {
        val activity = activityDao.getById(activityId) ?: error("运动记录不存在")
        if (kcal == null) {
            // 恢复估算
            activityDao.update(
                activity.copy(
                    burnOverridden = false,
                    burnedKcal = SportLibrary.estimateKcal(
                        activity.met,
                        currentWeightKg(),
                        activity.durationMinutes
                    )
                )
            )
        } else {
            require(kcal >= 0.0) { "热量不能是负数" }
            activityDao.update(
                activity.copy(burnedKcal = kcal, burnOverridden = true)
            )
        }
    }

    /** 删掉一项运动，连同它下面的动作卡片。 */
    suspend fun deleteActivity(activityId: Long): Result<Unit> = runCatching {
        exerciseLogDao.deleteForActivity(activityId)
        activityDao.deleteById(activityId)
    }

    suspend fun deleteActivitiesOn(date: String): Result<Unit> = runCatching {
        val activities = activityDao.getByDate(date)
        activities.forEach { exerciseLogDao.deleteForActivity(it.id) }
        activityDao.deleteByDate(date)
    }

    // --------------------------------------------------------- 撸铁动作

    /** 撸铁那一项下面的动作卡片。 */
    fun observeExercises(activityId: Long): Flow<List<ExerciseLogEntity>> =
        exerciseLogDao.observeByActivity(activityId)

    suspend fun getExercises(activityId: Long): List<ExerciseLogEntity> =
        exerciseLogDao.getByActivity(activityId)

    /**
     * 当天撸铁里的全部动作，用于「某一天练了什么」。
     */
    suspend fun getExercisesOn(date: String): List<ExerciseLogEntity> =
        exerciseLogDao.getByDate(date)

    /**
     * 加一个动作卡片。
     *
     * 重量 / 组数 / 次数没填时，自动用这个动作**上一次**的记录作为默认值；
     * 从没练过就用 0kg / 3 组 / 10 次。
     */
    suspend fun addExercise(
        activityId: Long,
        date: String,
        exerciseName: String,
        weightKg: Double? = null,
        sets: Int? = null,
        reps: Int? = null
    ): Result<Long> = runCatching {
        val name = exerciseName.trim()
        require(name.isNotEmpty()) { "动作名称不能为空" }
        val last = exerciseLogDao.previousForExercise(name, date)
        exerciseLogDao.insert(
            ExerciseLogEntity(
                activityId = activityId,
                date = date,
                exerciseName = name,
                weightKg = weightKg ?: last?.weightKg ?: 0.0,
                sets = sets ?: last?.sets ?: DEFAULT_SETS,
                reps = reps ?: last?.reps ?: DEFAULT_REPS,
                position = exerciseLogDao.countForActivity(activityId)
            )
        )
    }

    /** 改一个动作卡片的重量 / 组数 / 次数。 */
    suspend fun updateExercise(
        exerciseId: Long,
        weightKg: Double,
        sets: Int,
        reps: Int
    ): Result<Unit> = runCatching {
        require(weightKg >= 0.0) { "重量不能是负数" }
        require(sets in 1..MAX_SETS) { "组数请填 1-$MAX_SETS" }
        require(reps in 1..MAX_REPS) { "次数请填 1-$MAX_REPS" }
        val target = exerciseLogDao.getById(exerciseId) ?: error("动作不存在")
        exerciseLogDao.update(
            target.copy(weightKg = weightKg, sets = sets, reps = reps)
        )
    }

    suspend fun deleteExercise(exerciseId: Long): Result<Unit> = runCatching {
        exerciseLogDao.deleteById(exerciseId)
    }

    /** 某个动作上一次的记录，用于预填。 */
    suspend fun previousExercise(exerciseName: String, beforeDate: String): ExerciseLogEntity? =
        exerciseLogDao.previousForExercise(exerciseName, beforeDate)

    // --------------------------------------------------------- 自建动作库

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

    // ------------------------------------------------------------- 拉伸库

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
        if (exerciseNames.isEmpty()) return emptyList()
        val builtIn = ExerciseLibrary.stretchesFor(exerciseNames, limit = 6)
        val linked = customExerciseDao.getAll()
            .filter { it.name in exerciseNames }
            .flatMap { it.stretchList }
        if (linked.isEmpty()) return builtIn

        val byName = getAllStretches().associateBy { it.name }
        val extra = linked.mapNotNull { byName[it] }
        return (builtIn + extra).distinctBy { it.name }.take(8)
    }

    // ------------------------------------------------------------- helpers

    suspend fun activityOf(profile: UserProfileEntity): ActivityLevel =
        ActivityLevel.fromStorage(profile.activityLevel)

    suspend fun goalModeOf(profile: UserProfileEntity): GoalMode =
        GoalMode.fromStorage(profile.goalMode)

    suspend fun sexOf(profile: UserProfileEntity): Sex = Sex.fromStorage(profile.sex)

    companion object {
        const val DEFAULT_SETS = 3
        const val DEFAULT_REPS = 10
        const val MAX_SETS = 20
        const val MAX_REPS = 100
    }
}

/** 把存储的自建拉伸转成共享领域类型。 */
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
