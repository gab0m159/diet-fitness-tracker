package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One size / spec of a branded food, e.g. 可口可乐（中）.
 *
 * ## Why this exists
 *
 * The official McDonald's China nutrition data publishes **per-serving** figures
 * only — it gives 千焦 / 大卡 / 蛋白质 / 脂肪 / 碳水化合物 / 钠 / 钙 for one
 * serving, and never states the serving's weight in grams. Several products come
 * in several sizes (薯条 迷你/小/中/大, 可乐 小/中/大), and each size has its own
 * published numbers.
 *
 * Storing these as variants of a single food (rather than as separate foods) keeps
 * the library readable and lets the add-entry sheet offer a size selector next to
 * the serving count.
 *
 * The values here are the **published per-serving numbers, stored verbatim**. No
 * conversion to per-100g is performed, so nothing is distorted.
 */
@Entity(
    tableName = "food_variants",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("food_id"), Index(value = ["food_id", "position"])]
)
data class FoodVariantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "food_id")
    val foodId: Long,

    /**
     * Size label as published, e.g. "小" / "中" / "大" / "1个" / "2块".
     * Empty string for a product that has only one form.
     */
    @ColumnInfo(name = "spec_name")
    val specName: String,

    /** Display order among a food's variants. */
    @ColumnInfo(name = "position")
    val position: Int = 0,

    /** Energy in kilojoules, as published. */
    @ColumnInfo(name = "kilojoules")
    val kilojoules: Double = 0.0,

    /** Energy in kilocalories, as published. */
    @ColumnInfo(name = "kcal")
    val kcal: Double = 0.0,

    /** Protein grams per serving. */
    @ColumnInfo(name = "protein_g")
    val proteinG: Double = 0.0,

    /** Fat grams per serving. */
    @ColumnInfo(name = "fat_g")
    val fatG: Double = 0.0,

    /** Carbohydrate grams per serving. */
    @ColumnInfo(name = "carbs_g")
    val carbsG: Double = 0.0,

    /** Sodium milligrams per serving. */
    @ColumnInfo(name = "sodium_mg")
    val sodiumMg: Double = 0.0,

    /** Calcium milligrams per serving. */
    @ColumnInfo(name = "calcium_mg")
    val calciumMg: Double = 0.0
) {
    /** "小" or "一份" when there is no meaningful size label. */
    val displaySpec: String get() = specName.ifBlank { "一份" }

    /** True when this variant is one of several choices. */
    val isMultiVariant: Boolean get() = specName.isNotBlank()

    /** Scale this variant's nutrition by a serving count. */
    fun scaledBy(servings: Double): VariantNutrition = VariantNutrition(
        kcal = kcal * servings,
        carbsG = carbsG * servings,
        proteinG = proteinG * servings,
        fatG = fatG * servings,
        sodiumMg = sodiumMg * servings,
        calciumMg = calciumMg * servings
    )
}

/** A variant's nutrition multiplied by a serving count. */
data class VariantNutrition(
    val kcal: Double,
    val carbsG: Double,
    val proteinG: Double,
    val fatG: Double,
    val sodiumMg: Double = 0.0,
    val calciumMg: Double = 0.0
) {
    val hasCalcium: Boolean get() = calciumMg > 0.0
    val hasSodium: Boolean get() = sodiumMg > 0.0
}
