package com.example.diettracker.domain

import com.example.diettracker.data.model.BodyPart
import com.example.diettracker.data.model.SplitType

/**
 * A complete, ready-to-use training plan offered on the plan screen.
 *
 * 每个预设就是一串训练日：用户看一眼内容、一键套用，之后训练日、动作、目标重量
 * 和每个训练日的频率都可以自由改。
 *
 * v6 去掉了 `scheduleMode` / `defaultRestAfter` / `defaultWeeklyDays`：排程不再有
 * 「按周固定 / 按序列循环」两种模式，每个训练日各自带一个「几天一次」的频率。
 */
data class PlanTemplate(
    val type: SplitType,
    val title: String,
    val trainingDays: List<SplitDayTemplate>,
    /** 套用时写进每个训练日的默认频率（几天一次）。 */
    val defaultIntervalDays: Int,
    /** Why someone would pick this plan. */
    val description: String
) {
    val dayCount: Int get() = trainingDays.size

    /** "3 个训练日 · 每 4 天一次" */
    val headline: String get() = "$dayCount 个训练日 · 每 $defaultIntervalDays 天一次"

    /** Total distinct exercises across all days. */
    val exerciseCount: Int get() = trainingDays.sumOf { it.exerciseNames.size }
}

/**
 * The three preset plans.
 *
 * Exercise content comes from [SplitTemplates] so the split definitions live in
 * exactly one place; this object adds the presentation and frequency defaults
 * that the training-day screen needs.
 */
object PlanTemplates {

    fun all(): List<PlanTemplate> = listOf(threeDay, fiveDay, sixDay)

    fun byType(type: SplitType): PlanTemplate =
        all().firstOrNull { it.type == type } ?: threeDay

    /** 三分化：胸+三头 / 背+二头 / 腿+肩 */
    val threeDay = PlanTemplate(
        type = SplitType.THREE,
        title = "三分化计划",
        trainingDays = SplitTemplates.forSplit(SplitType.THREE),
        defaultIntervalDays = SplitTemplates.defaultIntervalDays(SplitType.THREE),
        description = "每个训练日覆盖一个主要部位加一个协同小肌群，" +
            "默认每 4 天轮到一次，适合每周能练 3 天左右、想兼顾恢复的人。"
    )

    /** 五分化：胸 / 背 / 腿 / 肩 / 手臂 */
    val fiveDay = PlanTemplate(
        type = SplitType.FIVE,
        title = "五分化计划",
        trainingDays = SplitTemplates.forSplit(SplitType.FIVE),
        defaultIntervalDays = SplitTemplates.defaultIntervalDays(SplitType.FIVE),
        description = "每个部位单独一天，单次训练容量更集中，" +
            "默认每 6 天轮到一次，适合每周能练 5 天、追求单部位刺激的人。"
    )

    /** 六分化：在五分化基础上把腿拆成股四头与后链两天 */
    val sixDay = PlanTemplate(
        type = SplitType.SIX,
        title = "六分化计划",
        trainingDays = SplitTemplates.forSplit(SplitType.SIX),
        defaultIntervalDays = SplitTemplates.defaultIntervalDays(SplitType.SIX),
        description = "在五分化基础上把腿拆成「股四头肌」和「腘绳肌+臀」两天，" +
            "默认每 7 天轮到一次，适合每周能练 6 天、想给腿部更多容量的人。"
    )

    /** Preview lines for the card body: "胸 + 三头 — 杠铃卧推、上斜卧推…". */
    fun previewLines(
        template: PlanTemplate,
        maxExercisesPerDay: Int = 4
    ): List<Pair<String, String>> =
        template.trainingDays.map { day ->
            day.name to day.exerciseNames.take(maxExercisesPerDay).joinToString("、") +
                if (day.exerciseNames.size > maxExercisesPerDay) "…" else ""
        }

    /** All body parts used by a template, for a compact "覆盖部位" line. */
    fun coveredParts(template: PlanTemplate): List<BodyPart> =
        template.trainingDays.map { it.bodyPart }.distinct()
}
