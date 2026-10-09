package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 训练日里的一个动作，连同它**只属于这个训练日**的目标设定。
 *
 * 这是 v6 新增的核心表，取代了旧模型里两处「按动作名全局共享」的做法：
 *
 *  - 旧 `TrainingSplitEntity.exerciseNames`（逗号串，无法承载每个动作的目标值）；
 *  - 旧 `ExerciseSettingEntity`（按动作名存步进和上次重量，跨计划共享）。
 *
 * 因为目标重量会被「成功」按钮高频改写，而且必须按「训练日 + 动作」隔离，所以
 * 动作在这里是**行**而不是逗号串里的一个下标：行 id 就是动作在计划内的身份，
 * 增删 / 排序不会让目标值错位到别的动作上。
 */
@Entity(
    tableName = "training_day_exercises",
    foreignKeys = [
        ForeignKey(
            entity = TrainingSplitEntity::class,
            parentColumns = ["id"],
            childColumns = ["split_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("split_id"), Index(value = ["split_id", "position"])]
)
data class TrainingDayExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 所属训练日。 */
    @ColumnInfo(name = "split_id")
    val splitId: Long,

    /** 在训练日内的顺序，0-based。 */
    @ColumnInfo(name = "position")
    val position: Int = 0,

    /** 动作名，对应 `ExerciseLibrary` 或用户自建动作。 */
    @ColumnInfo(name = "exercise_name")
    val exerciseName: String,

    /** 目标组数，例如 4。 */
    @ColumnInfo(name = "target_sets")
    val targetSets: Int = DEFAULT_SETS,

    /**
     * 每组次数目标。允许三种写法：
     *  - `"8"`      单值；
     *  - `"8-12"`   区间（新建动作时的默认）；
     *  - `"12,12,10"` 逐组指定。
     */
    @ColumnInfo(name = "target_reps")
    val targetReps: String = DEFAULT_REPS,

    /** 目标重量（kg）。0 表示「还没设定」，此时卡片显示「未设定」。 */
    @ColumnInfo(name = "target_weight_kg")
    val targetWeightKg: Double = 0.0,

    /**
     * 这个动作在这个计划里的步进值（成功 +多少 kg）。
     * **默认 0，表示不自动加重**；要加重由用户自己填（例如 2.5）。
     * null 与 0 等价（历史行可能是 null），代码里一律按 0 处理。
     */
    @ColumnInfo(name = "increment_kg")
    val incrementKg: Double? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_SETS = 3
        const val DEFAULT_REPS = "8-12"

        /** 卡片上的「4 组 × 8-12 次」。 */
        fun label(targetSets: Int, targetReps: String): String =
            "$targetSets 组 × ${targetReps.ifBlank { DEFAULT_REPS }}"
    }
}
