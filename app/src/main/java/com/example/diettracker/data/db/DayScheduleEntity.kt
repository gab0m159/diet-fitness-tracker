package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 今日日程里的一项是怎么来的。 */
enum class ScheduleEntryKind(val label: String) {
    /** 整个训练日加入今天（动作清单来自该训练日）。 */
    TRAINING_DAY("训练日"),

    /** 单独从动作库加进来的一个动作，不依赖任何训练日。 */
    SINGLE_EXERCISE("单独动作");

    companion object {
        fun fromStorage(value: String): ScheduleEntryKind =
            entries.firstOrNull { it.name == value } ?: SINGLE_EXERCISE
    }
}

/**
 * 今日日程里**手动添加**的一项。
 *
 * ## 自动到期的不存在这里
 *
 * 训练日靠 `TrainingSplitEntity.nextDueDate` 到期后会自动出现在今日训练栏，那些
 * 是每次渲染实时算出来的，不落库。这张表只存用户点「+」主动加进来的东西：
 *
 *  - 加一整个训练日（即使它今天没到期）——此时 [splitName] 存名字快照，
 *    动作卡片由该训练日的 [TrainingDayExerciseEntity] 行提供；
 *  - 加一个单独的动作——动作名和目标四项直接存在这一行上，因为它不属于任何
 *    训练日，[splitId] 为 null。
 *
 * ## 为什么要有这张表
 *
 * 「今天加过什么」必须和「计划长什么样」解耦：把训练日加进今天之后，用户再改
 * 训练日，今天的日程不该被改写；「延期」也需要把今天这些行整体挪到明天。
 */
@Entity(
    tableName = "day_schedule",
    indices = [Index("date"), Index(value = ["date", "position"])]
)
data class DayScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 归属日期（ISO `yyyy-MM-dd`）。 */
    @ColumnInfo(name = "date")
    val date: String,

    /** 当天的显示顺序，0-based。 */
    @ColumnInfo(name = "position")
    val position: Int = 0,

    /** `ScheduleEntryKind.name`。 */
    @ColumnInfo(name = "kind")
    val kind: String = ScheduleEntryKind.SINGLE_EXERCISE.name,

    /** 训练日 id；[ScheduleEntryKind.SINGLE_EXERCISE] 时为 null。 */
    @ColumnInfo(name = "split_id")
    val splitId: Long? = null,

    /** 训练日名字快照，删除训练日后往期/今日仍可显示。 */
    @ColumnInfo(name = "split_name")
    val splitName: String = "",

    /** 动作名；训练日行留空（动作来自训练日自己的行）。 */
    @ColumnInfo(name = "exercise_name")
    val exerciseName: String = "",

    /** 单独动作的目标组数。 */
    @ColumnInfo(name = "target_sets")
    val targetSets: Int = TrainingDayExerciseEntity.DEFAULT_SETS,

    /** 单独动作的每组次数。 */
    @ColumnInfo(name = "target_reps")
    val targetReps: String = TrainingDayExerciseEntity.DEFAULT_REPS,

    /** 单独动作的目标重量；成功后会按步进在自己这张卡片上累加。 */
    @ColumnInfo(name = "target_weight_kg")
    val targetWeightKg: Double = 0.0,

    /** 单独动作的步进值；null 表示用动作库默认。 */
    @ColumnInfo(name = "increment_kg")
    val incrementKg: Double? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    val kindValue: ScheduleEntryKind get() = ScheduleEntryKind.fromStorage(kind)

    val isTrainingDay: Boolean get() = kindValue == ScheduleEntryKind.TRAINING_DAY
}
