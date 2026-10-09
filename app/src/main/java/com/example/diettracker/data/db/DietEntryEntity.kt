package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One food entry inside one day's diary.
 *
 * The nutrition columns are **denormalized snapshots** taken when the entry was
 * created. That way, editing or deleting a food later never rewrites history.
 *
 * [grams] is always the authoritative amount; [servings] is remembered only so the
 * UI can show "按份数 1.5 份" again.
 */
@Entity(
    tableName = "diet_entries",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("food_id"), Index("date")]
)
data class DietEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Day the entry belongs to, ISO-8601 `yyyy-MM-dd` in the device's local zone. */
    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "food_id")
    val foodId: Long,

    /** Food name at the time of logging. */
    @ColumnInfo(name = "food_name")
    val foodName: String,

    /** Amount actually eaten, in grams. */
    @ColumnInfo(name = "grams")
    val grams: Double,

    /** How the amount was entered: see `AmountMode`. */
    @ColumnInfo(name = "amount_mode")
    val amountMode: String,

    /** Number of servings, when [amountMode] is `SERVING`; informational only. */
    @ColumnInfo(name = "servings")
    val servings: Double,

    /** Serving size in grams at the time of logging. */
    @ColumnInfo(name = "serving_size_grams")
    val servingSizeGrams: Double,

    /** Snapshot: carbohydrate grams per 100 g. */
    @ColumnInfo(name = "carbs_per_100g")
    val carbsPer100g: Double,

    /** Snapshot: protein grams per 100 g. */
    @ColumnInfo(name = "protein_per_100g")
    val proteinPer100g: Double,

    /** Snapshot: fat grams per 100 g. */
    @ColumnInfo(name = "fat_per_100g")
    val fatPer100g: Double,

    /** Which meal this belongs to: see `MealType`. */
    @ColumnInfo(name = "meal_type")
    val mealType: String = "OTHER",

    /**
     * Snapshot of the chosen size for a branded food, e.g. "中". Empty for foods
     * that are not sold in sizes.
     */
    @ColumnInfo(name = "variant_spec")
    val variantSpec: String = "",

    /**
     * Per-serving nutrition of the chosen variant, snapshotted at entry time.
     *
     * The entry's contribution is these values multiplied by [servings], which
     * keeps the published figures exact and immune to later edits of the library.
     */
    @ColumnInfo(name = "variant_kcal_per_serving")
    val variantKcalPerServing: Double = 0.0,

    @ColumnInfo(name = "variant_carbs_per_serving")
    val variantCarbsPerServing: Double = 0.0,

    @ColumnInfo(name = "variant_protein_per_serving")
    val variantProteinPerServing: Double = 0.0,

    @ColumnInfo(name = "variant_fat_per_serving")
    val variantFatPerServing: Double = 0.0,

    @ColumnInfo(name = "variant_sodium_per_serving")
    val variantSodiumPerServing: Double = 0.0,

    @ColumnInfo(name = "variant_calcium_per_serving")
    val variantCalciumPerServing: Double = 0.0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /** True when this entry was logged against a sized / per-serving food. */
    val usesVariant: Boolean
        get() = variantKcalPerServing > 0.0 ||
            variantCarbsPerServing > 0.0 ||
            variantProteinPerServing > 0.0 ||
            variantFatPerServing > 0.0

    /** Servings actually eaten; falls back to 1 for legacy rows. */
    private val servingCount: Double get() = if (servings > 0.0) servings else 1.0

    val carbsGrams: Double
        get() = if (usesVariant) {
            variantCarbsPerServing * servingCount
        } else {
            carbsPer100g * grams / 100.0
        }

    val proteinGrams: Double
        get() = if (usesVariant) {
            variantProteinPerServing * servingCount
        } else {
            proteinPer100g * grams / 100.0
        }

    val fatGrams: Double
        get() = if (usesVariant) {
            variantFatPerServing * servingCount
        } else {
            fatPer100g * grams / 100.0
        }

    /** Calories: the published value when available, else Atwater from macros. */
    val kcalTotal: Double
        get() = if (usesVariant) {
            variantKcalPerServing * servingCount
        } else {
            carbsGrams * 4.0 + proteinGrams * 4.0 + fatGrams * 9.0
        }

    val sodiumMgTotal: Double get() = variantSodiumPerServing * servingCount
    val calciumMgTotal: Double get() = variantCalciumPerServing * servingCount
}
