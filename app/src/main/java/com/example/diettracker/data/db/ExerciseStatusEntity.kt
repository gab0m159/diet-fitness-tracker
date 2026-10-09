package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 动作卡片上按下的三个按钮，以及它们的含义。
 *
 *  - [SUCCESS] 目标达成 → 该动作**在本训练计划内**的目标重量按步进值上调；
 *  - [FAILURE] 目标未达成 → 目标重量保持不变（v6 起永不自动降重）；
 *  - [SKIPPED] 没有练 → 不写往期、不改目标重量。
 *
 * 跳过仍然会落一行（值为 [SKIPPED]），因为跳过的动作必须从今日待办里划掉；
 * 但往期记录页只查询 [SUCCESS] / [FAILURE]，跳过永远不会出现在历史里。
 */
enum class ExerciseOutcome(val label: String) {
    SUCCESS("成功"),
    FAILURE("失败"),
    SKIPPED("跳过");

    companion object {
        fun fromStorage(value: String): ExerciseOutcome =
            entries.firstOrNull { it.name == value } ?: SKIPPED

        /** 只有这两种结果会进往期记录。 */
        val recordedInHistory = listOf(SUCCESS.name, FAILURE.name)
    }
}

/**
 * 某一天某张动作卡片的结果。
 *
 * ## 卡片身份 = (来源类型, 来源行 id)，不是动作名
 *
 * 旧模型全程拿动作名当身份（`exercise_settings` 的主键就是动作名），这是「同一
 * 动作在不同训练计划里要有不同目标重量」做不到的根因。这里改成：
 *
 *  - 来自训练日的卡片 → `source_id` = `TrainingDayExerciseEntity.id`；
 *  - 单独添加的卡片   → `source_id` = `DayScheduleEntity.id`。
 *
 * 于是同一天出现两个同名动作（例如「胸+三头」里有卧推，你又单独加了一张卧推）
 * 也能各自记一笔，不会互相覆盖。
 *
 * 目标值在这里是**快照**：往期记录要显示当时的组数 / 次数 / 重量，之后改训练日
 * 不能篡改历史。[sourceLabel] 记录来源（训练日名或「单独动作」）。
 */
@Entity(
    tableName = "exercise_status",
    indices = [
        Index("date"),
        Index("exercise_name"),
        Index(value = ["date", "source_kind", "source_id"], unique = true)
    ]
)
data class ExerciseStatusEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 关联的当天运动记录；允许为空（例如只标了卡片还没填时长）。 */
    @ColumnInfo(name = "session_id")
    val sessionId: Long? = null,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "exercise_name")
    val exerciseName: String,

    /** `ExerciseOutcome.name`。 */
    @ColumnInfo(name = "outcome")
    val outcome: String,

    /** `ScheduleEntryKind.name`：卡片来自训练日还是单独添加。 */
    @ColumnInfo(name = "source_kind")
    val sourceKind: String = ScheduleEntryKind.SINGLE_EXERCISE.name,

    /** 来源行 id：训练日动作行 id，或今日日程行 id。 */
    @ColumnInfo(name = "source_id")
    val sourceId: Long = 0L,

    /** 动作前的目标重量，用于显示「+5kg → 65kg」。 */
    @ColumnInfo(name = "weight_before_kg")
    val weightBeforeKg: Double,

    /** 动作后的目标重量。 */
    @ColumnInfo(name = "weight_after_kg")
    val weightAfterKg: Double = weightBeforeKg,

    /** 目标组数快照。 */
    @ColumnInfo(name = "target_sets")
    val targetSets: Int = TrainingDayExerciseEntity.DEFAULT_SETS,

    /** 每组次数快照，例如 "8-12" 或 "12,12,10"。 */
    @ColumnInfo(name = "target_reps")
    val targetReps: String = TrainingDayExerciseEntity.DEFAULT_REPS,

    /** 当时的目标重量快照。 */
    @ColumnInfo(name = "target_weight_kg")
    val targetWeightKg: Double = 0.0,

    /** 来源标签：训练日名字，或「单独动作」。 */
    @ColumnInfo(name = "source_label")
    val sourceLabel: String = "",

    @ColumnInfo(name = "position")
    val position: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    val outcomeValue: ExerciseOutcome
        get() = ExerciseOutcome.fromStorage(outcome)

    val sourceKindValue: ScheduleEntryKind
        get() = ScheduleEntryKind.fromStorage(sourceKind)

    /** 「4 组 × 8-12 次」。 */
    val targetLabel: String
        get() = TrainingDayExerciseEntity.label(targetSets, targetReps)

    /** 「60kg」，未设定时显示「未设定」。 */
    val targetWeightLabel: String
        get() = if (targetWeightKg > 0.0) "${trim(targetWeightKg)}kg" else "未设定"

    /** 「+5kg → 65kg」/「保持 60kg」。 */
    val changeLabel: String
        get() {
            val delta = weightAfterKg - weightBeforeKg
            return when {
                delta > 0.001 -> "+${trim(delta)}kg → ${trim(weightAfterKg)}kg"
                delta < -0.001 -> "${trim(delta)}kg → ${trim(weightAfterKg)}kg"
                weightAfterKg > 0.0 -> "保持 ${trim(weightAfterKg)}kg"
                else -> "未设定重量"
            }
        }

    private fun trim(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
        else rounded.toString()
    }
}
