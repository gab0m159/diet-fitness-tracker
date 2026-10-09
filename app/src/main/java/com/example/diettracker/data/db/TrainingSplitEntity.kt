package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一个训练日，例如「胸 + 三头」。
 *
 * 用户可以在「我的 → 训练日管理」里创建任意多个训练日，每个训练日：
 *
 *  - 有名字和部位；
 *  - 有一份动作清单（存在 [TrainingDayExerciseEntity]，每个动作自带目标组数 /
 *    每组次数 / 目标重量 / 步进）；
 *  - 有 [intervalDays]「几天一次」的频率，用来算它什么时候该出现在今日日程里。
 *
 * ## 排程模型（v6 起）
 *
 * 不再有「练 X 休 1」的轮转序列，也不再有按周固定。排程完全由 [nextDueDate]
 * 驱动：
 *
 *  - `nextDueDate == null` 表示还没练过，随时可以被排进今天；
 *  - `nextDueDate <= 今天` 表示到期，会自动出现在今日训练栏；
 *  - 完成（或跳过）后 `nextDueDate = 当天 + intervalDays`；
 *  - 「延期」会把所有 `nextDueDate >= 今天` 的训练日整体 +1 天。
 *
 * 新建训练日时按 [position] 错开初始到期日，否则同频率的训练日会全部挤在同一天。
 */
@Entity(tableName = "training_splits", indices = [Index("position")])
data class TrainingSplitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 显示名，例如「胸 + 三头」。 */
    @ColumnInfo(name = "name")
    val name: String,

    /** `BodyPart.name`，用于分组展示。 */
    @ColumnInfo(name = "body_part")
    val bodyPart: String,

    /** 列表顺序，0-based。刻意不做唯一约束，避免 REPLACE 插入误删相邻行。 */
    @ColumnInfo(name = "position")
    val position: Int,

    /** 几天练一次；例如 4 表示每 4 天轮到它一次。 */
    @ColumnInfo(name = "interval_days")
    val intervalDays: Int = DEFAULT_INTERVAL_DAYS,

    /** 下次到期日（ISO `yyyy-MM-dd`）；null 表示还没练过、随时可排。 */
    @ColumnInfo(name = "next_due_date")
    val nextDueDate: String? = null,

    /** 最近一次完成或跳过的日期。 */
    @ColumnInfo(name = "last_done_date")
    val lastDoneDate: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** 「每 4 天一次」等价于原来的「练 3 休 1」。 */
        const val DEFAULT_INTERVAL_DAYS = 4
        const val MIN_INTERVAL_DAYS = 1
        const val MAX_INTERVAL_DAYS = 30

        /** 把用户输入夹到合法区间。 */
        fun sanitizeInterval(days: Int): Int =
            days.coerceIn(MIN_INTERVAL_DAYS, MAX_INTERVAL_DAYS)
    }
}
