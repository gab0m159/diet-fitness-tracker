package com.example.diettracker.data.model

import com.example.diettracker.data.db.DietEntryEntity

/** How the user typed the amount when adding a diary entry. */
enum class AmountMode {
    /** Entered a gram amount directly. */
    GRAMS,

    /** Entered a number of servings; grams = servings * food.servingSizeGrams. */
    SERVING;

    companion object {
        fun fromStorage(value: String): AmountMode =
            entries.firstOrNull { it.name == value } ?: GRAMS
    }
}

/** Rough meal buckets used to group the day's diary. */
enum class MealType(val label: String) {
    BREAKFAST("早餐"),
    LUNCH("午餐"),
    DINNER("晚餐"),
    SNACK("加餐"),
    OTHER("其他");

    companion object {
        fun fromStorage(value: String): MealType =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}

/** A macro triple in grams (or kcal, depending on context). */
data class Macros(
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0
) {
    operator fun plus(other: Macros) = Macros(
        carbs = carbs + other.carbs,
        protein = protein + other.protein,
        fat = fat + other.fat
    )

    /** Atwater factors: 4 / 4 / 9 kcal per gram. */
    val calories: Double get() = carbs * 4.0 + protein * 4.0 + fat * 9.0

    companion object {
        val ZERO = Macros()
    }
}

/**
 * Domain view of a diary entry, with its macros already computed.
 * The UI never does nutrition math itself.
 */
data class DiaryEntry(
    val id: Long,
    val date: String,
    val foodId: Long,
    val foodName: String,
    val grams: Double,
    val amountMode: AmountMode,
    val servings: Double,
    val servingSizeGrams: Double,
    val macros: Macros,
    val mealType: MealType,
    val createdAt: Long,
    /** Chosen size for a branded food, e.g. "中". Empty when not applicable. */
    val variantSpec: String = "",
    /** Calories, using the published figure when the food has one. */
    val kcal: Double = 0.0,
    /** True when this entry was logged against a per-serving branded food. */
    val usesVariant: Boolean = false
) {
    /**
     * "1.5 份 · 中" for a sized branded food, "1.5 份 · 300 g" for a per-100g food
     * logged by servings, "120 g" when grams were entered directly.
     */
    val amountLabel: String
        get() = when {
            usesVariant -> {
                val spec = variantSpec.trim()
                if (spec.isNotEmpty()) {
                    "${formatQuantity(servings)} 份 · $spec"
                } else {
                    "${formatQuantity(servings)} 份"
                }
            }
            amountMode == AmountMode.SERVING && servings > 0.0 ->
                "${formatQuantity(servings)} 份 · ${formatGrams(grams)} g"
            else -> "${formatGrams(grams)} g"
        }
}

fun DietEntryEntity.toDiaryEntry(): DiaryEntry = DiaryEntry(
    id = id,
    date = date,
    foodId = foodId,
    foodName = foodName,
    grams = grams,
    amountMode = AmountMode.fromStorage(amountMode),
    servings = servings,
    servingSizeGrams = servingSizeGrams,
    macros = Macros(carbsGrams, proteinGrams, fatGrams),
    mealType = MealType.fromStorage(mealType),
    createdAt = createdAt,
    variantSpec = variantSpec,
    kcal = kcalTotal,
    usesVariant = usesVariant
)

/** Trims trailing ".0" and keeps at most one decimal place. */
fun formatGrams(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
    else String.format(java.util.Locale.US, "%.1f", rounded)
}

/** Like [formatGrams] but keeps up to two decimals, for servings. */
fun formatQuantity(value: Double): String {
    val rounded = Math.round(value * 100.0) / 100.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
    else String.format(java.util.Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
}

/** Whole-gram display for macro totals. */
fun formatMacro(value: Double): String = Math.round(value).toString()

/**
 * A day's energy ledger.
 *
 * [burnedCalories] comes from the training module and is **display only**: it is
 * never added back to the remaining macro allowance. [netCalories] exists so the
 * user can judge for themselves whether to eat more.
 */
data class DailyEnergy(
    val intakeCalories: Double = 0.0,
    val burnedCalories: Double = 0.0
) {
    val netCalories: Double get() = intakeCalories - burnedCalories

    /** True when there is any burn to show at all. */
    val hasBurn: Boolean get() = burnedCalories > 0.0

    /** "摄入 2100 − 消耗 420 = 净 1680 kcal" */
    val summary: String
        get() = "摄入 ${Math.round(intakeCalories)} − 消耗 ${Math.round(burnedCalories)}" +
            " = 净 ${Math.round(netCalories)} kcal"
}
