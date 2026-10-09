package com.example.diettracker.domain

import com.example.diettracker.data.model.TrainingObjective

/**
 * 成功 / 失败 / 跳过 对「目标重量」的影响，以及一些次数区间的解析工具。
 *
 * ## v6 起规则被刻意简化了
 *
 * 目标重量由每个训练日里**每个动作自己**的步进值驱动，规则只有三条，用户按下
 * 按钮后的结果必须完全可预测：
 *
 * | 标记 | 目标重量 | 往期记录 |
 * |---|---|---|
 * | 成功 | +步进值 | 记录 |
 * | 失败 | **不变**（永不自动降重） | 记录 |
 * | 跳过 | 不变 | 不记录 |
 *
 * 旧版本在这里挂了一条「双重渐进 + 三层降级」的推荐阶梯（连续在上限完成就建议
 * 加重、连续失败 3 次降重 5%、组数不够先加组、组数够了放宽次数区间……）。那套
 * 逻辑和「失败不降重、步进只作用于本计划内这个动作」的新要求冲突，也和新模型里
 * 「目标重量就是下次要用的重量」重复，因此整体移除：卡片上的目标重量本身就是要
 * 执行的目标，不再另给一份建议。
 */
object ProgressionEngine {

    /** 调用方没给步进值时的兜底（2.5kg）。 */
    const val FALLBACK_INCREMENT_KG = 2.5

    /** 最小的可调步进。 */
    const val MIN_INCREMENT_KG = 0.5

    /** 给「成功」用的一次性结果。 */
    data class Outcome(
        val weightKg: Double,
        val action: Action,
        val explanation: String
    )

    enum class Action {
        /** 还没设定重量，先确定起始重量。 */
        BASELINE,

        /** 按步进加重。 */
        ADD_WEIGHT,

        /** 维持当前重量。 */
        HOLD
    }

    /**
     * 成功：目标重量 + 步进值。
     *
     * 如果当前还没有目标重量（0），这一下只确认「能做到」，不凭空发明一个数字，
     * 由用户自己填一个起始重量。
     */
    fun onSuccess(currentWeightKg: Double, incrementKg: Double): Outcome {
        // 步进 0（默认）表示不自动加重，直接保持原重量。
        if (incrementKg <= 0.0) {
            return Outcome(
                weightKg = currentWeightKg,
                action = Action.HOLD,
                explanation = if (currentWeightKg > 0.0) {
                    "成功；步进为 0，目标重量保持 ${formatKg(currentWeightKg)}kg"
                } else {
                    "已记录成功；还没设定目标重量"
                }
            )
        }
        if (currentWeightKg <= 0.0) {
            return Outcome(
                weightKg = 0.0,
                action = Action.BASELINE,
                explanation = "已记录成功；先填一个目标重量，之后成功才会按步进加重"
            )
        }
        val next = roundToStep(currentWeightKg + incrementKg, incrementKg)
        return Outcome(
            weightKg = next,
            action = Action.ADD_WEIGHT,
            explanation = "成功，目标重量 +${formatKg(incrementKg)}kg → ${formatKg(next)}kg"
        )
    }

    /**
     * 失败：目标重量**保持不变**，不自动降重。
     *
     * 连续失败多少次都一样；要减重就在训练日编辑器里直接改目标重量。
     */
    fun onFailure(currentWeightKg: Double): Outcome = Outcome(
        weightKg = currentWeightKg,
        action = if (currentWeightKg > 0.0) Action.HOLD else Action.BASELINE,
        explanation = if (currentWeightKg > 0.0) {
            "失败，目标重量维持 ${formatKg(currentWeightKg)}kg（不自动降重）"
        } else {
            "已记录失败；还没设定重量"
        }
    )

    /** 跳过：不记录、不改重量。保留函数只为让调用点的语义对称。 */
    fun onSkip(currentWeightKg: Double): Outcome = Outcome(
        weightKg = currentWeightKg,
        action = Action.HOLD,
        explanation = "已跳过，目标重量不变"
    )

    private fun usableIncrement(incrementKg: Double): Double =
        if (incrementKg > 0.0) incrementKg else FALLBACK_INCREMENT_KG

    // ------------------------------------------------------------ 次数区间

    /**
     * 解析目标次数字符串，支持三种写法：
     *
     *  - `"8"`        → [8, 8]
     *  - `"8-12"`     → [8, 12]
     *  - `"12,12,10"` → [10, 12]（逐组指定时取最小与最大）
     *
     * 解析不出来时返回 null，由调用方决定兜底。
     */
    fun repRange(spec: String): IntRange? {
        val text = spec.trim()
        if (text.isEmpty()) return null
        val rangeMatch = Regex("""^(\d+)\s*[-~－]\s*(\d+)$""").find(text)
        if (rangeMatch != null) {
            val min = rangeMatch.groupValues[1].toIntOrNull() ?: return null
            val max = rangeMatch.groupValues[2].toIntOrNull() ?: return null
            return if (min <= max) min..max else max..min
        }
        val numbers = parseReps(text)
        if (numbers.isEmpty()) return null
        return numbers.min()..numbers.max()
    }

    /** 逐组次数，例如 `"12,12,10"` → [12, 12, 10]；区间写法返回空列表。 */
    fun parseReps(spec: String): List<Int> =
        spec.split(',', '，', ' ')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }

    /** 卡片上的次数文案：区间显示 `8-12 次`，单值显示 `8 次`。 */
    fun repsLabel(spec: String): String {
        val range = repRange(spec)
        return when {
            range == null -> spec.ifBlank { "—" }
            range.first == range.last -> "${range.first} 次"
            else -> "${range.first}-${range.last} 次"
        }
    }

    // ------------------------------------------------------------ 默认值

    /** 新加动作时的默认次数区间，来自训练目标。 */
    fun defaultRepsMin(objective: TrainingObjective): Int = when (objective) {
        TrainingObjective.POWERLIFTING -> 3
        TrainingObjective.HYPERTROPHY -> 8
        TrainingObjective.PHYSIQUE -> 8
    }

    fun defaultRepsMax(objective: TrainingObjective): Int = when (objective) {
        TrainingObjective.POWERLIFTING -> 6
        TrainingObjective.HYPERTROPHY -> 12
        TrainingObjective.PHYSIQUE -> 12
    }

    /** 新加动作时的默认次数字符串，例如 `"8-12"`。 */
    fun defaultRepsSpec(objective: TrainingObjective): String =
        "${defaultRepsMin(objective)}-${defaultRepsMax(objective)}"

    /** 组间休息建议文案，例如「60-120 秒」。 */
    fun restLabel(objective: TrainingObjective): String =
        "${objective.restSecondsMin}-${objective.restSecondsMax} 秒"

    // ------------------------------------------------------------ 取整

    /**
     * 把重量吸附到步进的整数倍，避免出现 62.49999 这种数字。
     */
    fun roundToStep(kg: Double, step: Double): Double {
        if (kg <= 0.0) return 0.0
        val safeStep = usableIncrement(step)
        return Math.round(kg / safeStep) * safeStep
    }

    /** 没有动作专属步进时，按 2.5kg 吸附。 */
    fun roundToPlate(kg: Double): Double = roundToStep(kg, FALLBACK_INCREMENT_KG)

    /**
     * 步进值可选列表，只给「自建动作的默认步进」用（训练日里的步进是自由填的）。
     */
    val incrementChoices: List<Double> =
        listOf(0.5, 1.0, 1.25, 2.0, 2.5, 5.0, 10.0)

    private fun formatKg(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}
