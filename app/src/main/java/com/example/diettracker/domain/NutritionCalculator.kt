package com.example.diettracker.domain

import com.example.diettracker.data.model.ActivityLevel
import com.example.diettracker.data.model.GoalMode
import com.example.diettracker.data.model.Macros
import com.example.diettracker.data.model.Sex
import kotlin.math.roundToInt

/**
 * All body-metric and macro-target math, as pure functions.
 *
 * Keeping this free of Android/Room types means it can be reasoned about (and
 * unit-tested) directly. The UI only formats the results.
 *
 * ## The calculation chain
 *
 * 1. `bmrMifflin` — Mifflin-St Jeor BMR.
 * 2. `tdee` — BMR x activity multiplier.
 * 3. `targetCalories` — TDEE x the goal-mode factor.
 * 4. `macroTargets` — split those calories into grams using the goal's ratio,
 *    with Atwater factors 4 / 4 / 9 kcal per gram.
 *
 * `bmrKatchMcArdle` is computed alongside as a **reference only**; by product
 * decision the default chain always uses Mifflin.
 */
object NutritionCalculator {

    // Atwater factors, kcal per gram.
    const val KCAL_PER_G_CARBS = 4.0
    const val KCAL_PER_G_PROTEIN = 4.0
    const val KCAL_PER_G_FAT = 9.0

    /** Katch-McArdle constants. */
    private const val KATCH_BASE = 370.0
    private const val KATCH_PER_KG_LBM = 21.6

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
     * Splits [calories] into macro grams using the goal's recommended ratio.
     *
     *   carbs   = calories x carbsPct   / 4
     *   protein = calories x proteinPct / 4
     *   fat     = calories x fatPct     / 9
     */
    fun macroTargets(calories: Double, goal: GoalMode): Macros = Macros(
        carbs = calories * goal.carbsPct / KCAL_PER_G_CARBS,
        protein = calories * goal.proteinPct / KCAL_PER_G_PROTEIN,
        fat = calories * goal.fatPct / KCAL_PER_G_FAT
    )

    /**
     * The full estimate shown on the profile screen.
     * [katchBmr] is null when no body-fat percentage was entered.
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
        val calories = targetCalories(tdeeValue, goal)
        val katch = leanBodyMass(weightKg, bodyFatPercent)
            ?.let { bmrKatchMcArdle(weightKg, bodyFatPercent!!) }
        return MacroEstimate(
            bmrMifflin = mifflin,
            bmrKatch = katch,
            tdee = tdeeValue,
            targetCalories = calories,
            targets = macroTargets(calories, goal)
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
