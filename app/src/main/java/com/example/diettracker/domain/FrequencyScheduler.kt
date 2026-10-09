package com.example.diettracker.domain

import com.example.diettracker.data.db.TrainingSplitEntity
import com.example.diettracker.util.DateUtils

/**
 * 「这个训练日今天该不该练」的全部规则，取代原来的轮转序列解析器。
 *
 * ## 模型
 *
 * 每个训练日有一个 [TrainingSplitEntity.intervalDays]「几天一次」和一个
 * [TrainingSplitEntity.nextDueDate] 到期日。没有轮转下标，没有「练 X 休 1」的
 * 序列，也没有按周固定的星期表——训练日之间互相独立，各自按自己的节奏到期。
 *
 * | 事件 | nextDueDate 变化 |
 * |---|---|
 * | 完成 | 当天 + intervalDays |
 * | 跳过整个训练日 | 当天 + intervalDays（今天不再出现，明天也不会） |
 * | 延期 | 所有 `nextDueDate >= 今天` 的训练日 +1 天 |
 * | 从未练过（null） | 随时到期，可以被排进今天 |
 *
 * 「延期」是**全局**的：今天的内容整体挪到明天，后面所有到期日一起顺延，不是
 * 只挪当前这一个训练日。
 *
 * ## 日期比较
 *
 * 全程用 ISO `yyyy-MM-dd` 字符串，字典序即时间序，可以直接比较，无需解析。
 */
object FrequencyScheduler {

    /**
     * 这个训练日在 [date] 是否到期。从未练过（`nextDueDate == null`）视为到期，
     * 这样新建的训练日能立刻被排进今天。
     */
    fun isDue(split: TrainingSplitEntity, date: String): Boolean {
        val due = split.nextDueDate ?: return true
        return due <= date
    }

    /** 到期且应该出现在今日训练栏的训练日，先到期的在前，再按列表顺序。 */
    fun dueSplits(splits: List<TrainingSplitEntity>, date: String): List<TrainingSplitEntity> =
        splits.filter { isDue(it, date) }
            .sortedWith(
                compareBy<TrainingSplitEntity> { it.nextDueDate ?: "" }
                    .thenBy { it.position }
            )

    /** 把 [date] 之后该练的日期算出来（含到期日当天），用于「下次 3 月 5 日」提示。 */
    fun nextDueLabel(split: TrainingSplitEntity, today: String): String {
        val due = split.nextDueDate ?: return "随时可练"
        return when {
            due <= today -> "今天该练"
            due == DateUtils.plusDays(today, 1) -> "明天该练"
            else -> "${DateUtils.displayDate(due)}该练"
        }
    }

    /**
     * 完成或跳过之后的训练日：到期日推进一个间隔，并记下最近一次处理日期。
     */
    fun afterHandled(split: TrainingSplitEntity, date: String): TrainingSplitEntity =
        split.copy(
            nextDueDate = DateUtils.plusDays(date, split.intervalDays.toLong()),
            lastDoneDate = date,
            updatedAt = System.currentTimeMillis()
        )

    /**
     * 「延期」：今天的内容整体挪到明天，后续训练日一起顺延。
     *
     * 具体地，每个训练日的到期日都推到 `max(到期日, 今天) + 1 天`：
     *
     *  - 今天到期的（含从未练过的 null）→ 明天；
     *  - 排在将来的 → 往后挪一天。
     *
     * 已经逾期很久的不会因为延期而变得更靠后（只看今天），否则拖一次就越排越远。
     */
    fun postponeAll(
        splits: List<TrainingSplitEntity>,
        date: String
    ): List<TrainingSplitEntity> = splits.map { split ->
        val base = split.nextDueDate?.takeIf { it >= date } ?: date
        split.copy(
            nextDueDate = DateUtils.plusDays(base, 1),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 新建第 [position] 个训练日时的初始到期日。
     *
     * 刻意按位置错开一天：如果几个训练日都是「每 4 天一次」且初始到期日相同，
     * 它们会永远挤在同一天。错开之后自然形成「今天练 A、明天练 B、后天练 C」的
     * 轮换。
     */
    fun initialNextDueDate(date: String, position: Int): String =
        DateUtils.plusDays(date, position.toLong())

    /** 「每 3 天一次」。 */
    fun describeInterval(days: Int): String = "每 $days 天一次"

    /** 训练日卡片上的一行摘要：「每 4 天一次 · 今天该练」。 */
    fun describeStatus(split: TrainingSplitEntity, today: String): String =
        "${describeInterval(split.intervalDays)} · ${nextDueLabel(split, today)}"
}
