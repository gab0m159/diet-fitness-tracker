package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 单行表，保存用户的训练偏好。
 *
 * v6 去掉了 `schedule_mode` 和 `rest_after`：排程不再有「按周固定 / 按序列循环」
 * 两种模式，改由每个训练日的 `interval_days` 频率 + `next_due_date` 驱动。
 *
 * [splitType] 只用来在预设计划列表里标出「当前用的是哪个预设」，不再参与任何
 * 排程计算——训练日现在是完全自由创建和编辑的。
 */
@Entity(tableName = "user_training_goal")
data class UserTrainingGoalEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLETON_ID,

    /** `SplitType.name`：THREE / FIVE / SIX，仅用于预设高亮。 */
    @ColumnInfo(name = "split_type")
    val splitType: String = "THREE",

    /** `TrainingObjective.name`：POWERLIFTING / HYPERTROPHY / PHYSIQUE。 */
    @ColumnInfo(name = "objective")
    val objective: String = "HYPERTROPHY",

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = 1

        fun default() = UserTrainingGoalEntity()
    }
}
