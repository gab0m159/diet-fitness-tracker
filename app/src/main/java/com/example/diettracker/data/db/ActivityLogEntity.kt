package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一天里的**一项运动**（跑步 30 分钟、撸铁 60 分钟…）。
 *
 * v7 起模型被大幅简化：不再有训练日、频率、到期日、延期、跳过，也没有成功 /
 * 失败 / 步进。用户每天直接往当天加运动项目，填时长，系统按 MET 算消耗。
 *
 * ## 热量
 *
 * [burnedKcal] = `MET × 体重(kg) × 时长(小时)`，其中 MET 存在 [met] 上（加进来
 * 的时候从运动库快照过来，之后改库不影响历史）。用户可以手改这个数字，改过之后
 * [burnOverridden] 为真。
 *
 * 它**只用于显示当天运动消耗**，不会加回可摄入额度（TDEE 已含活动系数）。
 *
 * ## 撸铁
 *
 * 「撸铁」也是一个普通运动项目（[sportKey] = `"STRENGTH"`），同样填时长、算热量；
 * 它额外挂一组动作卡片（[ExerciseLogEntity]）。
 */
@Entity(
    tableName = "activity_logs",
    indices = [Index("date"), Index(value = ["date", "position"])]
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 归属日期（ISO `yyyy-MM-dd`）。 */
    @ColumnInfo(name = "date")
    val date: String,

    /** 当天内的顺序，0-based。 */
    @ColumnInfo(name = "position")
    val position: Int = 0,

    /** 运动库里那一项的稳定标识，例如 `RUNNING` / `STRENGTH`。 */
    @ColumnInfo(name = "sport_key")
    val sportKey: String,

    /** 运动名快照，例如「跑步」。 */
    @ColumnInfo(name = "sport_name")
    val sportName: String,

    /** MET 值快照，用于算热量；之后运动库调整不影响已记录的行。 */
    @ColumnInfo(name = "met")
    val met: Double,

    /** 时长（分钟），用户可改。 */
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int,

    /** 消耗热量；默认按 MET 估算，可被用户覆盖。 */
    @ColumnInfo(name = "burned_kcal")
    val burnedKcal: Double = 0.0,

    /** 真表示热量是用户自己填的，不是估算值。 */
    @ColumnInfo(name = "burn_overridden")
    val burnOverridden: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /** MET 估算：MET × 体重(kg) × 小时。 */
    fun estimateBurn(bodyWeightKg: Double): Double =
        met * bodyWeightKg * (durationMinutes / 60.0)

    /** 是否是需要挂动作卡片的项目（撸铁）。 */
    val isStrength: Boolean get() = sportKey == SPORT_STRENGTH

    companion object {
        const val SPORT_STRENGTH = "STRENGTH"
    }
}

/**
 * 撸铁里的一个动作：`动作名 + 重量 + 组数 + 次数`，**都是单一值**。
 *
 * 刻意不支持「每组次数不一样」（例如 12,12,10）：需求明确要求只用三个滑轮选
 * 重量 / 组数 / 次数，所以这里就是两个整数加一个重量，没有逗号串。
 */
@Entity(
    tableName = "exercise_logs",
    indices = [Index("date"), Index("activity_id"), Index(value = ["activity_id", "position"])]
)
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 所属的运动记录（撸铁那一项）。 */
    @ColumnInfo(name = "activity_id")
    val activityId: Long,

    /** 日期快照，便于「某一天练了什么」一次查询。 */
    @ColumnInfo(name = "date")
    val date: String,

    /** 动作名，对应动作库或用户自建动作。 */
    @ColumnInfo(name = "exercise_name")
    val exerciseName: String,

    /** 重量（kg）。 */
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double = 0.0,

    /** 组数。 */
    @ColumnInfo(name = "sets")
    val sets: Int = 3,

    /** 每组次数（单一值，不是逐组列表）。 */
    @ColumnInfo(name = "reps")
    val reps: Int = 10,

    @ColumnInfo(name = "position")
    val position: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /** 「60kg × 4 组 × 8 次」 */
    val summary: String
        get() = "${trim(weightKg)}kg × $sets 组 × $reps 次"

    /** 总容量 kg×次数，用于小结展示。 */
    val volume: Double get() = weightKg * sets * reps

    private fun trim(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
        else rounded.toString()
    }
}
