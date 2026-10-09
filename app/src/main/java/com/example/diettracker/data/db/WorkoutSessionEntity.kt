package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一天的运动记录（时长 / 强度 / 消耗热量）。
 *
 * v6 起**按天一条**：旧模型是每个训练日槽位一条（带 `split_day_index`），新模型
 * 里同一天可能既练了训练日又临时加了单个动作，热量只能按天累计，所以槽位维度
 * 被去掉，改成日期维度。
 *
 * 消耗热量显式存储（[burnedKcal]），因为 MET 估算值允许用户手改。它**只用于
 * 饮食页的「运动消耗」显示**，永远不会加回可摄入额度。
 */
@Entity(
    tableName = "workout_sessions",
    indices = [Index("date"), Index(value = ["date", "status"])]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** ISO 日期 `yyyy-MM-dd`。 */
    @ColumnInfo(name = "date")
    val date: String,

    /** 当天练了哪些训练日（名字快照，逗号分隔），供记录页做标题。 */
    @ColumnInfo(name = "title")
    val title: String = "",

    /** `SessionStatus.name`: IN_PROGRESS / COMPLETED / SKIPPED。 */
    @ColumnInfo(name = "status")
    val status: String = "IN_PROGRESS",

    /** 训练时长，分钟，用户手填。 */
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int = 0,

    /** `TrainingIntensity.name`，决定 MET 默认值。 */
    @ColumnInfo(name = "intensity")
    val intensity: String = "HEAVY",

    /** 消耗热量 = MET × 体重 × 小时，可被用户覆盖。 */
    @ColumnInfo(name = "burned_kcal")
    val burnedKcal: Double = 0.0,

    /** 真表示热量是用户自己填的，不是估算值。 */
    @ColumnInfo(name = "burn_overridden")
    val burnOverridden: Boolean = false,

    /** 可选备注。 */
    @ColumnInfo(name = "note")
    val note: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** MET 估算：MET × 体重(kg) × 小时。 */
    fun estimateBurn(met: Double, bodyWeightKg: Double): Double =
        met * bodyWeightKg * (durationMinutes / 60.0)
}
