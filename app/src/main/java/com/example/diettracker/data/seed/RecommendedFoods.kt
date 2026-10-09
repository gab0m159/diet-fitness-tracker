package com.example.diettracker.data.seed

import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.model.FoodCategory
import com.example.diettracker.data.model.FoodCredibility
import com.example.diettracker.data.model.NutritionTags

/**
 * Bundled "quality source" foods, tagged 优质碳水 / 优质蛋白质 / 优质脂肪.
 *
 * Each row carries a plain-language [FoodEntity.dailyRecommendation] such as
 * "每天约 200-300g". That string is **display only**: it is never parsed and
 * never influences the user's macro targets.
 *
 * ## Raw vs cooked
 *
 * Values are per 100 g of the state named in the food's own name. Chicken breast
 * in particular differs enormously between raw and cooked, so it is listed as
 * two separate foods — otherwise anyone weighing raw meat would overestimate by
 * roughly 80%.
 */
object RecommendedFoods {

    private const val SRC_USDA = "USDA FoodData Central"
    private const val SRC_USDA_APPROX = "USDA 标准数据（近似值）"

    fun all(): List<FoodEntity> = qualityCarbs() + qualityProtein() + qualityFat()

    // ------------------------------------------------------- 优质碳水

    private fun qualityCarbs(): List<FoodEntity> = listOf(
        recommended(
            name = "土豆（蒸）",
            servingGrams = 150.0,
            carbs = 17.5,
            protein = 2.05,
            fat = 0.09,
            tag = NutritionTags.QUALITY_CARBS,
            recommendation = "每天约 200-300g",
            note = "带皮蒸制，冷却后抗性淀粉增加"
        ),
        recommended(
            name = "玉米（煮）",
            servingGrams = 150.0,
            carbs = 19.0,
            protein = 3.3,
            fat = 1.2,
            tag = NutritionTags.QUALITY_CARBS,
            recommendation = "每天约 150-200g",
            note = "甜玉米，一根中等大小约 150g 可食部"
        ),
        recommended(
            name = "燕麦（干）",
            servingGrams = 40.0,
            carbs = 66.3,
            protein = 15.0,
            fat = 6.7,
            tag = NutritionTags.QUALITY_CARBS,
            recommendation = "每天约 40-60g",
            note = "干重，β-葡聚糖含量高的整粒燕麦更好"
        ),
        recommended(
            name = "糙米（熟）",
            servingGrams = 200.0,
            carbs = 23.0,
            protein = 2.6,
            fat = 0.9,
            tag = NutritionTags.QUALITY_CARBS,
            recommendation = "每天约 150-250g",
            note = "熟重，保留麸皮与胚芽"
        )
    )

    // ----------------------------------------------------- 优质蛋白质

    private fun qualityProtein(): List<FoodEntity> = listOf(
        // Raw and cooked are separate foods on purpose (see class doc).
        recommended(
            name = "鸡胸肉（生）",
            servingGrams = 150.0,
            carbs = 0.0,
            protein = 22.5,
            fat = 2.6,
            tag = NutritionTags.QUALITY_PROTEIN,
            recommendation = "每天约 100-150g",
            note = "生重。烹饪后约失水 25%，100g 生肉≈75g 熟肉"
        ),
        recommended(
            name = "鸡胸肉（熟）",
            servingGrams = 120.0,
            carbs = 0.0,
            protein = 30.0,
            fat = 3.5,
            tag = NutritionTags.QUALITY_PROTEIN,
            recommendation = "每天约 100-150g",
            note = "熟重（水煮/煎烤去皮）。若称的是生重请选「鸡胸肉（生）」",
            source = SRC_USDA_APPROX
        ),
        recommended(
            name = "鸡蛋（全蛋）",
            servingGrams = 50.0,
            carbs = 1.1,
            protein = 13.3,
            fat = 8.8,
            tag = NutritionTags.QUALITY_PROTEIN,
            recommendation = "每天约 2-3 个",
            note = "一个中等大小约 50g"
        ),
        recommended(
            name = "瘦牛肉（生）",
            servingGrams = 150.0,
            carbs = 0.0,
            protein = 21.0,
            fat = 5.0,
            tag = NutritionTags.QUALITY_PROTEIN,
            recommendation = "每天约 100-150g",
            note = "生重，后腿/里脊等瘦部位"
        ),
        recommended(
            name = "三文鱼（生）",
            servingGrams = 150.0,
            carbs = 0.0,
            protein = 20.0,
            fat = 13.0,
            tag = NutritionTags.QUALITY_PROTEIN,
            recommendation = "每天约 100-150g",
            note = "同时是优质脂肪来源（Omega-3）"
        ),
        recommended(
            name = "虾（生）",
            servingGrams = 100.0,
            carbs = 0.9,
            protein = 20.0,
            fat = 1.0,
            tag = NutritionTags.QUALITY_PROTEIN,
            recommendation = "每天约 100-150g"
        ),
        recommended(
            name = "白身鱼（鳕鱼/龙利鱼）",
            servingGrams = 150.0,
            carbs = 0.0,
            protein = 18.0,
            fat = 1.0,
            tag = NutritionTags.QUALITY_PROTEIN,
            recommendation = "每天约 100-150g"
        )
    )

    // ------------------------------------------------------- 优质脂肪

    private fun qualityFat(): List<FoodEntity> = listOf(
        recommended(
            name = "橄榄油",
            servingGrams = 10.0,
            carbs = 0.0,
            protein = 0.0,
            fat = 100.0,
            tag = NutritionTags.QUALITY_FAT,
            recommendation = "每天约 25-30g",
            note = "一汤匙约 10g（约 90 kcal）。适合凉拌或中低温烹饪"
        ),
        recommended(
            name = "杏仁",
            servingGrams = 30.0,
            carbs = 21.6,
            protein = 21.2,
            fat = 49.9,
            tag = NutritionTags.QUALITY_FAT,
            recommendation = "每天约 30-50g",
            note = "一小把约 30g"
        ),
        recommended(
            name = "核桃",
            servingGrams = 30.0,
            carbs = 13.7,
            protein = 15.2,
            fat = 65.2,
            tag = NutritionTags.QUALITY_FAT,
            recommendation = "每天约 30-50g",
            note = "α-亚麻酸含量较高"
        ),
        recommended(
            name = "牛油果",
            servingGrams = 150.0,
            carbs = 8.5,
            protein = 2.0,
            fat = 14.7,
            tag = NutritionTags.QUALITY_FAT,
            recommendation = "每天约 100-150g",
            note = "一个中等大小约 150g 可食部"
        ),
        recommended(
            name = "花生酱（无糖）",
            servingGrams = 15.0,
            carbs = 20.0,
            protein = 25.0,
            fat = 50.0,
            tag = NutritionTags.QUALITY_FAT,
            recommendation = "每天约 15-30g",
            note = "配料表只有花生的版本更好"
        )
    )

    // ------------------------------------------------------------ helper

    private fun recommended(
        name: String,
        servingGrams: Double,
        carbs: Double,
        protein: Double,
        fat: Double,
        tag: String,
        recommendation: String,
        note: String = "",
        source: String = SRC_USDA
    ): FoodEntity = FoodEntity(
        name = name,
        carbsPer100g = carbs,
        proteinPer100g = protein,
        fatPer100g = fat,
        servingSizeGrams = servingGrams,
        note = note,
        category = FoodCategory.RECOMMENDED.name,
        brandTag = "",
        nutritionTags = tag,
        dailyRecommendation = recommendation,
        dataSource = source,
        credibility = FoodCredibility.OFFICIAL.name
    )
}
