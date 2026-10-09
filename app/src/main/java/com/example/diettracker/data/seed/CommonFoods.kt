package com.example.diettracker.data.seed

import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.model.FoodCategory
import com.example.diettracker.data.model.FoodCredibility

/**
 * 常见食物：主食、豆类、蔬菜、甜品。
 *
 * 全部按**每 100g** 录入，数值与来源照抄，不做换算、不做四舍五入粉饰。
 *
 * ## 关于热量
 *
 * 应用里显示的热量是**由碳蛋脂按 4/4/9 反算**的（食物表没有单独的「官方热量」
 * 列）。绝大多数条目反算值和来源给出的热量一致到小数位，但个别条目对不上（例如
 * 牛油蛋糕：来源 420 kcal，但 6.7g 蛋白 / 45g 脂肪 / 26g 碳水反算出来是 536），
 * 这种情况我在 note 里把**来源的原始热量**写清楚，不擅自改碳蛋脂。
 */
object CommonFoods {

    const val SRC_CN_NUTRITION = "国家食物与营养咨询委员会"
    const val SRC_HK_HEALTH = "香港卫生署《营厨》"
    const val SRC_CN_CDC = "中国疾控中心营养与健康所"
    const val SRC_HK_CFS = "香港食物安全中心"

    fun all(): List<FoodEntity> = staples() + legumes() + vegetables() + desserts()

    /** 主食类（生重）。 */
    private fun staples(): List<FoodEntity> = listOf(
        of("大米", 7.5, 0.5, 79.0, SRC_CN_NUTRITION, "生米，未煮熟"),
        of("小米", 9.7, 1.7, 77.0, SRC_CN_NUTRITION, "生小米，未煮熟"),
        of("面粉", 12.0, 0.8, 70.0, SRC_CN_NUTRITION, "小麦粉，生粉")
    )

    /** 豆类（干豆）。 */
    private fun legumes(): List<FoodEntity> = listOf(
        of("黄豆", 39.2, 17.4, 25.0, SRC_CN_NUTRITION, "干黄豆，未泡发"),
        of("绿豆", 22.1, 0.8, 59.0, SRC_CN_NUTRITION, "干绿豆，未泡发")
    )

    /** 蔬菜类（生鲜可食部）。 */
    private fun vegetables(): List<FoodEntity> = listOf(
        of("番茄", 0.9, 0.2, 4.0, SRC_HK_HEALTH, "生鲜可食部"),
        of("青椒", 0.86, 0.17, 4.64, SRC_HK_HEALTH, "生鲜可食部"),
        of("菠菜", 2.3, 0.4, 0.3, SRC_HK_HEALTH, "生鲜可食部")
    )

    /** 甜品 / 烘焙。 */
    private fun desserts(): List<FoodEntity> = listOf(
        of("蛋糕（均值）", 8.6, 5.1, 67.1, SRC_CN_CDC),
        of("提拉米苏", 4.7, 28.6, 55.0, SRC_CN_CDC),
        of(
            name = "芝士蛋糕",
            protein = 7.6,
            fat = 30.0,
            carbs = 19.0,
            source = SRC_HK_CFS,
            note = "来源热量 320 kcal/100g；碳蛋脂反算约 376"
        ),
        of(
            name = "牛油蛋糕",
            protein = 6.7,
            fat = 45.0,
            carbs = 26.0,
            source = SRC_HK_CFS,
            note = "来源热量 420 kcal/100g；碳蛋脂反算约 536"
        )
    )

    // ------------------------------------------------------------- helper

    private fun of(
        name: String,
        protein: Double,
        fat: Double,
        carbs: Double,
        source: String,
        note: String = ""
    ): FoodEntity = FoodEntity(
        name = name,
        carbsPer100g = carbs,
        proteinPer100g = protein,
        fatPer100g = fat,
        // 数据本身就是每 100g，所以「一份」按 100g 处理。
        servingSizeGrams = 100.0,
        note = note,
        category = FoodCategory.RECOMMENDED.name,
        brandTag = "",
        nutritionTags = "",
        dailyRecommendation = "",
        dataSource = source,
        credibility = credibilityFor(source).name,
        hasVariants = false,
        sourceNote = ""
    )
}

/**
 * 把来源文字映射到现有三档可信度。
 *
 * 规则（按用户的说法）：**包装 / 官方标示 / 官方检测都算「官方」**；第三方平台整理
 * （fatsecret、薄荷网、海外电商转录、第三方整理）算「第三方」。宁可按高可信度记，
 * 也不自己造一个「估算」出来。
 */
internal fun credibilityFor(source: String): FoodCredibility = when {
    source.contains("官方") -> FoodCredibility.OFFICIAL
    source.contains("国家食物与营养咨询委员会") -> FoodCredibility.OFFICIAL
    source.contains("疾控中心") -> FoodCredibility.OFFICIAL
    source.contains("卫生署") -> FoodCredibility.OFFICIAL
    source.contains("食物安全中心") -> FoodCredibility.OFFICIAL
    source.contains("营养师协会") -> FoodCredibility.OFFICIAL
    source.contains("Starbucks") -> FoodCredibility.OFFICIAL
    else -> FoodCredibility.THIRD_PARTY
}
