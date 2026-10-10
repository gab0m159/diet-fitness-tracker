package com.example.diettracker.domain

import com.example.diettracker.data.model.ActivityLevel
import com.example.diettracker.data.model.GoalMode
import com.example.diettracker.data.model.Macros
import com.example.diettracker.data.model.Sex
import kotlin.math.roundToInt

/**
 * 身体数据与宏量目标的计算，全部是纯函数。
 *
 * 不依赖 Android / Room 类型，因此可以直接单测；界面只负责把结果格式化。
 *
 * ## 计算链（v7 起）
 *
 * 1. `bmrMifflin` —— Mifflin-St Jeor 的 BMR；
 * 2. `tdee` —— BMR × 活动系数；
 * 3. `macroTargets` —— **按体重给克数**（不是按热量百分比切）；
 * 4. `targetCaloriesFromMacros` —— 目标热量 = 碳×4 + 蛋×4 + 脂×9。
 *
 * ## 为什么改成按体重算宏量
 *
 * 早期版本是「TDEE × 倍率 → 按 50/25/25 等比例切」，结果是 70kg 的人拿到
 * 碳水 324g / 蛋白 162g，明显偏高：蛋白质到了 2.3g/kg，碳水占了一半热量。
 * 主流的健身做法是按体重直接定克数（蛋白 1.4-2.0 g/kg、脂肪 0.8-1.0 g/kg、
 * 碳水按训练量给），所以现在：
 *
 * | 模式 | 碳水 g/kg | 蛋白 g/kg | 脂肪 g/kg |
 * |------|-----------|-----------|-----------|
 * | 增肌 | 4.0       | 1.8       | 1.0       |
 * | 保持 | 3.5       | 1.6       | 0.9       |
 * | 减脂 | 2.5       | 2.2       | 0.8       |
 *
 * 由此算出的三个克数**反过来决定热量目标**，界面上只显示这一个热量数字，
 * 避免「热量目标和宏量对不上」的困惑。TDEE 仍然计算并显示，作为参考。
 *
 * `bmrKatchMcArdle` 只作为**参考值**并列显示；默认链路始终用 Mifflin。
 */
object NutritionCalculator {

    // Atwater factors, kcal per gram.
    const val KCAL_PER_G_CARBS = 4.0
    const val KCAL_PER_G_PROTEIN = 4.0
    const val KCAL_PER_G_FAT = 9.0

    /** Katch-McArdle constants. */
    private const val KATCH_BASE = 370.0
    private const val KATCH_PER_KG_LBM = 21.6

    /**
     * 每公斤体重的克数。
     *
     * ## 蛋白质的依据
     *
     * ISSN（国际运动营养学会）2017 年立场声明（Jäger et al.）：
     *  - 第 2 条：增肌与维持肌肉，**1.4–2.0 g/kg/天** 对大多数运动人群已经足够；
     *  - 第 3 条：热量缺口期（减脂）为最大化保留瘦体重，可能需要 **2.3–3.1 g/kg**。
     *
     * 那个 2.3–3.1 是给**有训练经验的备赛运动员**的上限区间，且多按去脂体重研究；
     * 对普通用户不现实（70kg 要 168g 蛋白 ≈ 700g 鸡胸肉）。同类研究里，
     * 30% 热量缺口下 1.6 g/kg 已能达成约 70% 的减脂比例。
     *
     * 所以取：增肌 1.8 / 保持 1.6 / 减脂 2.0 —— 减脂高于增肌（符合"缺口期需要更多"），
     * 但不进入不现实的区间。
     *
     * ## 脂肪与碳水
     *
     * 脂肪取 0.8–1.0 g/kg：通行底线是 0.5–0.6 g/kg（保激素），这里给得更宽裕。
     * 碳水按训练强度给：增肌最高（训练量大）、减脂最低。
     */
    data class PerKgTargets(
        val carbsPerKg: Double,
        val proteinPerKg: Double,
        val fatPerKg: Double
    )

    fun perKgTargets(goal: GoalMode): PerKgTargets = when (goal) {
        GoalMode.BULK -> PerKgTargets(carbsPerKg = 4.0, proteinPerKg = 1.8, fatPerKg = 1.0)
        GoalMode.MAINTAIN -> PerKgTargets(carbsPerKg = 3.5, proteinPerKg = 1.6, fatPerKg = 0.9)
        GoalMode.CUT -> PerKgTargets(carbsPerKg = 2.5, proteinPerKg = 2.0, fatPerKg = 0.8)
    }

    // ------------------------------------------------------------ BMR / TDEE

    /**
     * Mifflin-St Jeor basal metabolic rate, in kcal/day.
     *
     * Male:   10w + 6.25h - 5a + 5
     * Female: 10w + 6.25h - 5a - 161
     */
    fun bmrMifflin(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        sex: Sex
    ): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return when (sex) {
            Sex.MALE -> base + 5.0
            Sex.FEMALE -> base - 161.0
        }
    }

    /**
     * Katch-McArdle BMR from lean body mass. Requires a body-fat percentage.
     *
     * LBM = weight x (1 - bf%/100)
     * BMR = 370 + 21.6 x LBM
     */
    fun bmrKatchMcArdle(weightKg: Double, bodyFatPercent: Double): Double {
        val leanMass = leanBodyMass(weightKg, bodyFatPercent) ?: return 0.0
        return KATCH_BASE + KATCH_PER_KG_LBM * leanMass
    }

    /** Lean body mass in kg, or null when the input is unusable. */
    fun leanBodyMass(weightKg: Double, bodyFatPercent: Double?): Double? {
        if (bodyFatPercent == null) return null
        if (weightKg <= 0.0) return null
        // A body-fat percentage outside (0, 70) is treated as not entered.
        if (bodyFatPercent <= 0.0 || bodyFatPercent >= 70.0) return null
        return weightKg * (1.0 - bodyFatPercent / 100.0)
    }

    /** Total daily energy expenditure: BMR x activity multiplier. */
    fun tdee(bmr: Double, activity: ActivityLevel): Double =
        bmr * activity.multiplier

    /** Target calories after the goal-mode adjustment (e.g. TDEE x 1.15). */
    fun targetCalories(tdee: Double, goal: GoalMode): Double =
        tdee * goal.calorieFactor

    // ------------------------------------------------------------- targets

    /**
     * 目标宏量：**按体重 × 每公斤克数**直接给（v7 起）。
     *
     * 不再从热量反推，所以这三个数才是"用户真正要吃的东西"；
     * 目标热量由 [targetCaloriesFromMacros] 从它们反过来算。
     */
    fun macroTargets(weightKg: Double, goal: GoalMode): Macros {
        if (weightKg <= 0.0) return Macros(0.0, 0.0, 0.0)
        val perKg = perKgTargets(goal)
        return Macros(
            carbs = weightKg * perKg.carbsPerKg,
            protein = weightKg * perKg.proteinPerKg,
            fat = weightKg * perKg.fatPerKg
        )
    }

    /**
     * 目标热量 = 碳×4 + 蛋×4 + 脂×9。
     *
     * 界面上只显示这一个热量目标，避免和 TDEE 参考值混淆。
     */
    fun targetCaloriesFromMacros(macros: Macros): Double =
        macros.carbs * KCAL_PER_G_CARBS +
            macros.protein * KCAL_PER_G_PROTEIN +
            macros.fat * KCAL_PER_G_FAT

    /**
     * 完整估算结果（「我的」页显示的那一块）。
     *
     * [MacroEstimate.tdee] 仍然给出，用于告诉用户"你的消耗大概是多少"；
     * 但 [MacroEstimate.targetCalories] 来自宏量之和，不是 TDEE × 倍率。
     */
    fun estimate(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        sex: Sex,
        activity: ActivityLevel,
        goal: GoalMode,
        bodyFatPercent: Double?
    ): MacroEstimate {
        val mifflin = bmrMifflin(weightKg, heightCm, age, sex)
        val tdeeValue = tdee(mifflin, activity)
        val targets = macroTargets(weightKg, goal)
        val katch = leanBodyMass(weightKg, bodyFatPercent)
            ?.let { bmrKatchMcArdle(weightKg, bodyFatPercent!!) }
        return MacroEstimate(
            bmrMifflin = mifflin,
            bmrKatch = katch,
            tdee = tdeeValue,
            targetCalories = targetCaloriesFromMacros(targets),
            targets = targets
        )
    }

    /**
     * Rounds macro grams to whole numbers for display/prefill.
     * The sum of the rounded grams may differ from the calorie target by a few
     * kcal, which is expected and not corrected.
     */
    fun roundMacros(macros: Macros): Macros = Macros(
        carbs = macros.carbs.roundToInt().toDouble(),
        protein = macros.protein.roundToInt().toDouble(),
        fat = macros.fat.roundToInt().toDouble()
    )

    // -------------------------------------------------------- energy ledger

    /**
     * Net calories = intake - workout burn.
     *
     * IMPORTANT: the burn is **display-only**. It is never added back to the
     * remaining macro allowance, by explicit product decision.
     */
    fun netCalories(intakeCalories: Double, burnedCalories: Double): Double =
        intakeCalories - burnedCalories

    // -------------------------------------------------------- progress state

    /**
     * Colour band for one tracked metric.
     *
     * - [OVER] above 100% of goal -> red
     * - [NEAR] 90%-100% of goal   -> amber
     * - [ON_TRACK] below 90%      -> green
     * - [NO_GOAL] goal is 0       -> neutral, nothing tracked
     */
    enum class ProgressBand { ON_TRACK, NEAR, OVER, NO_GOAL }

    fun progressBand(consumed: Double, goal: Double): ProgressBand {
        if (goal <= 0.0) return ProgressBand.NO_GOAL
        val ratio = consumed / goal
        return when {
            ratio > 1.0 -> ProgressBand.OVER
            ratio >= 0.9 -> ProgressBand.NEAR
            else -> ProgressBand.ON_TRACK
        }
    }

    /** Completion ratio, clamped to [0,1] for drawing the bar fill. */
    fun progressFraction(consumed: Double, goal: Double): Float =
        if (goal <= 0.0) 0f else (consumed / goal).coerceIn(0.0, 1.0).toFloat()

    /** True when the metric is within the "almost there" band. */
    fun isNearGoal(consumed: Double, goal: Double): Boolean =
        progressBand(consumed, goal) == ProgressBand.NEAR
}

/**
 * Everything the profile screen displays after pressing "估算".
 *
 * [bmrKatch] is null when the user did not enter a body-fat percentage.
 */
data class MacroEstimate(
    val bmrMifflin: Double,
    val bmrKatch: Double?,
    val tdee: Double,
    val targetCalories: Double,
    val targets: Macros
) {
    /** Difference between the two BMR estimates, for a "reference" hint. */
    val katchDelta: Double? get() = bmrKatch?.let { it - bmrMifflin }
}
