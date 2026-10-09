package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A food in the library.
 *
 * Nutrition is stored per 100 g. [servingSizeGrams] is "how big one typical
 * serving is" (e.g. one egg = 50 g), which lets the diary convert servings to grams.
 *
 * ## Tagging
 *
 * Three independent tag fields let one food carry several labels at once — e.g.
 * "山姆 混合坚果" is `brandTag = 山姆` **and** `nutritionTags = 优质脂肪`:
 *
 *  - [category]         which library tab it lives in (user / brand / recommended)
 *  - [brandTag]         a single brand name for chain products
 *  - [nutritionTags]    comma-separated "quality source" tags
 *
 * [dailyRecommendation] is a plain display string ("每天约 50g"). It is never
 * parsed and never feeds into the macro goal math.
 */
@Entity(
    tableName = "foods",
    indices = [Index("category"), Index("brand_tag")]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Food name, e.g. "鸡蛋". */
    @ColumnInfo(name = "name")
    val name: String,

    /** Carbohydrate grams per 100 g. */
    @ColumnInfo(name = "carbs_per_100g")
    val carbsPer100g: Double,

    /** Protein grams per 100 g. */
    @ColumnInfo(name = "protein_per_100g")
    val proteinPer100g: Double,

    /** Fat grams per 100 g. */
    @ColumnInfo(name = "fat_per_100g")
    val fatPer100g: Double,

    /** Grams in one typical serving, e.g. 50 for one egg, 200 for a bowl of rice. */
    @ColumnInfo(name = "serving_size_grams")
    val servingSizeGrams: Double,

    /** Optional free-form note (brand, cooking method, ...). */
    @ColumnInfo(name = "note")
    val note: String = "",

    /** `FoodCategory.name`: USER / BRAND / RECOMMENDED. */
    @ColumnInfo(name = "category")
    val category: String = "USER",

    /** Brand label for chain products, e.g. "麦当劳". Empty when not branded. */
    @ColumnInfo(name = "brand_tag")
    val brandTag: String = "",

    /** Comma-separated quality tags, e.g. "优质蛋白质". Empty when none. */
    @ColumnInfo(name = "nutrition_tags")
    val nutritionTags: String = "",

    /** Display-only advice such as "每天约 50g". Never used in calculations. */
    @ColumnInfo(name = "daily_recommendation")
    val dailyRecommendation: String = "",

    /** Where the numbers came from, for transparency. */
    @ColumnInfo(name = "data_source")
    val dataSource: String = "",

    /** `FoodCredibility.name`: OFFICIAL / THIRD_PARTY / ESTIMATED. */
    @ColumnInfo(name = "credibility")
    val credibility: String = "OFFICIAL",

    /**
     * True when this food's nutrition lives in `food_variants` (per serving)
     * rather than in the per-100g columns above.
     *
     * Branded products take this path because the official data is published per
     * serving and states no weight, so there is nothing to convert — and storing
     * it verbatim keeps the published numbers exact.
     */
    @ColumnInfo(name = "has_variants")
    val hasVariants: Boolean = false,

    /** Publication note, e.g. "数据更新至 2025 年 4 月". */
    @ColumnInfo(name = "source_note")
    val sourceNote: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Parsed nutrition tags. */
    val tags: List<String>
        get() = nutritionTags.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** True when this food is user-created (highest priority). */
    val isUserCreated: Boolean
        get() = category == "USER"

    /** "麦当劳" or "" */
    val brandLabel: String
        get() = brandTag.trim()

    /** kcal per 100 g, using Atwater factors. */
    val kcalPer100g: Double
        get() = carbsPer100g * 4.0 + proteinPer100g * 4.0 + fatPer100g * 9.0

    /** Everything a search query should match against. */
    val searchableText: String
        get() = buildString {
            append(name).append(' ')
            if (brandTag.isNotBlank()) append(brandTag).append(' ')
            if (nutritionTags.isNotBlank()) append(nutritionTags).append(' ')
            if (note.isNotBlank()) append(note)
        }

    companion object {
        fun encodeTags(tags: List<String>): String =
            tags.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")

        fun decodeTags(raw: String): List<String> =
            raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
