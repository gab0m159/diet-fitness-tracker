package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一条个人纪录（PR, Personal Record）。
 *
 * ## 为什么允许多条同名动作
 *
 * 用户要求「一个动作只显示最新的，但点进去可以看到历史」，所以同一动作可以有多条，
 * 列表按动作分组、每组取 [weightKg] 最大的那条作为当前 PR，其余作为历史。
 *
 * 记录的是 `动作 + 重量 + 次数 + 日期` —— 次数是必须的，因为「100kg × 1 次」和
 * 「100kg × 5 次」是完全不同的成就。
 */
@Entity(
    tableName = "personal_records",
    indices = [Index("exercise_name"), Index("date")]
)
data class PersonalRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 动作名，对应动作库里的名字。 */
    @ColumnInfo(name = "exercise_name")
    val exerciseName: String,

    /** 重量（kg）。 */
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,

    /** 次数（做这个重量做了几下）。 */
    @ColumnInfo(name = "reps")
    val reps: Int = 1,

    /** 达成日期（ISO `yyyy-MM-dd`）。 */
    @ColumnInfo(name = "date")
    val date: String,

    /** 可选备注，例如「比赛」「状态超好」。 */
    @ColumnInfo(name = "note")
    val note: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /** 「100kg × 1 次」 */
    val summary: String
        get() = "${trim(weightKg)}kg × $reps 次"

    /** 估算 1RM（Epley 公式），用于跨次数比较不同 PR 的含金量。 */
    val estimatedOneRm: Double
        get() = if (reps <= 1) weightKg else weightKg * (1.0 + reps / 30.0)

    private fun trim(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
        else rounded.toString()
    }
}
