package com.example.diettracker.data.seed

import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.model.BrandTags
import com.example.diettracker.data.model.FoodCategory
import com.example.diettracker.data.model.FoodCredibility
import com.example.diettracker.data.model.NutritionTags

/**
 * Bundled fast-food / chain-brand nutrition data.
 *
 * ## Sourcing and honesty
 *
 * Nothing here is downloaded at runtime — the app is offline by design and the
 * numbers are transcribed from published nutrition information. Each row
 * carries its [FoodEntity.dataSource] and a credibility grade so the UI can be
 * upfront about how much to trust it:
 *
 *  - `OFFICIAL`     — brand's own published nutrition table
 *  - `THIRD_PARTY`  — reputable third-party nutrition database
 *  - `ESTIMATED`    — self-media reviews / rough figures; shown as "仅供参考"
 *
 * Where a figure is not known it is simply absent rather than invented.
 * Serve sizes are the brand's stated per-item weight where available.
 */
object BrandFoods {

    private const val SRC_KFC_PDF = "肯德基官方营养资料 PDF"
    private const val SRC_LUCKIN = "第三方营养数据库记录"
    private const val SRC_MANNER = "第三方营养数据库记录"
    private const val SRC_SAM = "自媒体测评与第三方平台，仅供参考"

    /**
     * Brands stored as per-100g foods.
     *
     * McDonald's is **not** here: its official figures are per serving with no
     * stated weight, so it lives in [McDonaldsChinaFoods] and is installed as
     * per-serving variants by [FoodSeed].
     */
    fun nonMcDonalds(): List<FoodEntity> = kfc() + luckin() + manner() + sam()

    /** Everything this file contributes, for reference. */
    fun all(): List<FoodEntity> = nonMcDonalds()


    // ------------------------------------------------------------ 肯德基

    private fun kfc(): List<FoodEntity> = listOf(
        brand(
            name = "原味鸡（一块）",
            brand = BrandTags.KFC,
            servingGrams = 96.0,
            kcal = 265.0,
            carbs = 8.0,
            protein = 21.0,
            fat = 16.0,
            source = SRC_KFC_PDF,
            credibility = FoodCredibility.OFFICIAL
        ),
        brand(
            name = "香辣鸡腿堡",
            brand = BrandTags.KFC,
            servingGrams = 215.0,
            kcal = 520.0,
            carbs = 48.0,
            protein = 26.0,
            fat = 25.0,
            source = SRC_KFC_PDF,
            credibility = FoodCredibility.OFFICIAL
        ),
        brand(
            name = "葡式蛋挞",
            brand = BrandTags.KFC,
            servingGrams = 60.0,
            kcal = 220.0,
            carbs = 22.0,
            protein = 3.0,
            fat = 13.0,
            source = SRC_KFC_PDF,
            credibility = FoodCredibility.OFFICIAL
        ),
        brand(
            name = "薯条（中）",
            brand = BrandTags.KFC,
            servingGrams = 117.0,
            kcal = 320.0,
            carbs = 42.0,
            protein = 4.0,
            fat = 15.0,
            source = SRC_KFC_PDF,
            credibility = FoodCredibility.OFFICIAL
        ),
        brand(
            name = "老北京鸡肉卷",
            brand = BrandTags.KFC,
            servingGrams = 195.0,
            kcal = 445.0,
            carbs = 51.0,
            protein = 20.0,
            fat = 18.0,
            source = SRC_KFC_PDF,
            credibility = FoodCredibility.OFFICIAL
        )
    )

    // ------------------------------------------------------------- 瑞幸

    private fun luckin(): List<FoodEntity> = listOf(
        brand(
            name = "生椰拿铁（大杯）",
            brand = BrandTags.LUCKIN,
            servingGrams = 450.0,
            kcal = 195.0,
            carbs = 26.0,
            protein = 5.0,
            fat = 8.0,
            source = SRC_LUCKIN,
            credibility = FoodCredibility.THIRD_PARTY,
            note = "不同糖度差异较大，数据为常规配比估值",
            tags = listOf(NutritionTags.QUALITY_PROTEIN),
            recommendation = "含咖啡因，每天咖啡因约 400mg"
        ),
        brand(
            name = "美式咖啡（大杯）",
            brand = BrandTags.LUCKIN,
            servingGrams = 480.0,
            kcal = 15.0,
            carbs = 2.0,
            protein = 1.0,
            fat = 0.0,
            source = SRC_LUCKIN,
            credibility = FoodCredibility.THIRD_PARTY,
            recommendation = "含咖啡因，每天咖啡因约 400mg"
        ),
        brand(
            name = "拿铁（大杯）",
            brand = BrandTags.LUCKIN,
            servingGrams = 450.0,
            kcal = 160.0,
            carbs = 15.0,
            protein = 9.0,
            fat = 7.0,
            source = SRC_LUCKIN,
            credibility = FoodCredibility.THIRD_PARTY,
            tags = listOf(NutritionTags.QUALITY_PROTEIN),
            recommendation = "含咖啡因，每天咖啡因约 400mg"
        )
    )

    // ------------------------------------------------------------ Manner

    private fun manner(): List<FoodEntity> = listOf(
        brand(
            name = "拿铁（中杯）",
            brand = BrandTags.MANNER,
            servingGrams = 360.0,
            kcal = 140.0,
            carbs = 13.0,
            protein = 8.0,
            fat = 6.0,
            source = SRC_MANNER,
            credibility = FoodCredibility.THIRD_PARTY,
            tags = listOf(NutritionTags.QUALITY_PROTEIN),
            recommendation = "含咖啡因，每天咖啡因约 400mg"
        ),
        brand(
            name = "美式咖啡（中杯）",
            brand = BrandTags.MANNER,
            servingGrams = 360.0,
            kcal = 10.0,
            carbs = 1.0,
            protein = 0.5,
            fat = 0.0,
            source = SRC_MANNER,
            credibility = FoodCredibility.THIRD_PARTY,
            recommendation = "含咖啡因，每天咖啡因约 400mg"
        ),
        brand(
            name = "燕麦拿铁（中杯）",
            brand = BrandTags.MANNER,
            servingGrams = 360.0,
            kcal = 165.0,
            carbs = 22.0,
            protein = 4.0,
            fat = 6.0,
            source = SRC_MANNER,
            credibility = FoodCredibility.THIRD_PARTY,
            recommendation = "含咖啡因，每天咖啡因约 400mg"
        )
    )

    // -------------------------------------------------------------- 山姆

    private fun sam(): List<FoodEntity> = listOf(
        brand(
            name = "沙拉鸡胸肉",
            brand = BrandTags.SAM,
            servingGrams = 100.0,
            kcal = 133.0,
            carbs = 3.0,
            protein = 23.0,
            fat = 3.0,
            source = SRC_SAM,
            credibility = FoodCredibility.ESTIMATED,
            note = "数据来自自媒体测评，仅供参考",
            tags = listOf(NutritionTags.QUALITY_PROTEIN),
            recommendation = "每天约 100-150g"
        ),
        brand(
            name = "苹果干",
            brand = BrandTags.SAM,
            servingGrams = 100.0,
            kcal = 275.0,
            carbs = 65.0,
            protein = 1.0,
            fat = 0.5,
            source = SRC_SAM,
            credibility = FoodCredibility.ESTIMATED,
            note = "数据来自自媒体测评，仅供参考"
        ),
        brand(
            name = "混合坚果",
            brand = BrandTags.SAM,
            servingGrams = 100.0,
            kcal = 600.0,
            carbs = 20.0,
            protein = 18.0,
            fat = 52.0,
            source = SRC_SAM,
            credibility = FoodCredibility.ESTIMATED,
            note = "数据来自自媒体测评，仅供参考",
            tags = listOf(NutritionTags.QUALITY_FAT),
            recommendation = "每天约 50g"
        ),
        brand(
            name = "麻薯",
            brand = BrandTags.SAM,
            servingGrams = 100.0,
            kcal = 320.0,
            carbs = 60.0,
            protein = 5.0,
            fat = 7.0,
            source = SRC_SAM,
            credibility = FoodCredibility.ESTIMATED,
            note = "数据来自自媒体测评，仅供参考"
        ),
        brand(
            name = "希腊酸奶",
            brand = BrandTags.SAM,
            servingGrams = 100.0,
            kcal = 97.0,
            carbs = 4.0,
            protein = 9.0,
            fat = 5.0,
            source = SRC_SAM,
            credibility = FoodCredibility.ESTIMATED,
            note = "数据来自自媒体测评，仅供参考",
            tags = listOf(NutritionTags.QUALITY_PROTEIN)
        )
    )

    // ------------------------------------------------------------ helper

    /**
     * Builds a per-serving row and converts it to per-100 g, which is how the
     * rest of the app stores nutrition.
     */
    private fun brand(
        name: String,
        brand: String,
        servingGrams: Double,
        kcal: Double,
        carbs: Double,
        protein: Double,
        fat: Double,
        source: String,
        credibility: FoodCredibility,
        note: String = "",
        tags: List<String> = emptyList(),
        recommendation: String = ""
    ): FoodEntity {
        val factor = 100.0 / servingGrams
        return FoodEntity(
            name = name,
            carbsPer100g = round1(carbs * factor),
            proteinPer100g = round1(protein * factor),
            fatPer100g = round1(fat * factor),
            servingSizeGrams = servingGrams,
            note = if (note.isNotBlank()) note else "一份 ${servingGrams.toInt()}g，约 ${kcal.toInt()} kcal",
            category = FoodCategory.BRAND.name,
            brandTag = brand,
            nutritionTags = FoodEntity.encodeTags(tags),
            dailyRecommendation = recommendation,
            dataSource = source,
            credibility = credibility.name
        )
    }

    private fun round1(value: Double): Double = Math.round(value * 10.0) / 10.0
}
