package com.example.diettracker.data.seed

import com.example.diettracker.data.db.FoodDao
import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.db.FoodVariantDao
import com.example.diettracker.data.db.FoodVariantEntity
import com.example.diettracker.data.model.BrandTags
import com.example.diettracker.data.model.FoodCategory
import com.example.diettracker.data.model.FoodCredibility

/**
 * 包装食品、饮料与咖啡。
 *
 * ## 录入规则
 *
 *  - **一律照抄来源数值**，不换算、不补齐、不四舍五入粉饰；
 *  - 来源标注「—」的字段**记为 0 并在备注写「数据暂缺」**（不编造，也不假装是 0）；
 *  - 来源标注「Trace」的记为 0 并在备注写「微量」；
 *  - 饮料按**每 100ml**（备注里写明），其余按每 100g；
 *  - 星巴克只公布**每份**（Grande 473ml）的营养，所以走 `food_variants` 按份存，
 *    和麦当劳那批数据的做法一致，避免把「每份」当成「每 100ml」算错。
 *
 * ## 热量对不上的条目
 *
 * 应用显示的热量是碳蛋脂按 4/4/9 反算的，没有单独的「官方热量」列。酒类（酒精
 * 7 kcal/g 不计入碳蛋脂）、以及缺了某个宏量的条目，反算值会和来源差很多，这类
 * 条目在备注里写清来源热量。
 */
object PackagedFoods {

    private const val SRC_OFFICIAL = "品牌官方营养标示"
    private const val SRC_OFFICIAL_TEST = "官方检测"
    private const val SRC_THIRD = "第三方整理"
    private const val SRC_FATSECRET = "fatsecret（第三方）"
    private const val SRC_ECOM = "海外电商转录"
    private const val SRC_BOOH = "薄荷网（第三方）"
    private const val SRC_CDC = "中国疾控中心"
    private const val SRC_HK_NUTRITIONIST = "香港营养师协会"
    private const val SRC_STARBUCKS = "Starbucks UK 官方营养手册"

    fun all(): List<FoodEntity> =
        instantNoodles() + sausages() + carbonated() + juices() +
            teas() + milkTeas() + energyDrinks() + beers()

    // ------------------------------------------------------------ 泡面

    private fun instantNoodles(): List<FoodEntity> = listOf(
        of("康师傅红烧牛肉面", BrandTags.KANG_SHIFU, 9.4, 25.8, 54.8, SRC_THIRD),
        of("康师傅香辣牛肉面", BrandTags.KANG_SHIFU, 7.1, 27.1, 52.6, SRC_FATSECRET),
        of("康师傅老坛酸菜牛肉面（袋装）", BrandTags.KANG_SHIFU, 6.1, 23.2, 49.2, SRC_ECOM),
        of("康师傅鲜虾鱼板面（桶面）", BrandTags.KANG_SHIFU, 8.3, 21.5, 57.8, SRC_ECOM),
        of("康师傅香菇炖鸡面", BrandTags.KANG_SHIFU, 9.1, 18.0, 65.0, SRC_ECOM),
        of("康师傅泡椒牛肉面", BrandTags.KANG_SHIFU, 8.8, 19.0, 67.1, SRC_FATSECRET),
        of("康师傅藤椒牛肉拌面", BrandTags.KANG_SHIFU, 6.7, 31.2, 41.1, SRC_FATSECRET),
        of("康师傅金汤肥牛面", BrandTags.KANG_SHIFU, 7.2, 23.9, 53.5, SRC_FATSECRET),
        of("康师傅日式豚骨面", BrandTags.KANG_SHIFU, 9.5, 22.2, 58.9, SRC_ECOM),
        of("康师傅番茄鸡蛋牛肉面", BrandTags.KANG_SHIFU, 7.1, 22.8, 52.1, SRC_FATSECRET),
        of("康师傅辣番茄鸡蛋牛肉面", BrandTags.KANG_SHIFU, 7.4, 21.6, 56.6, SRC_ECOM),
        of(
            name = "康师傅黑胡椒牛排面（面饼）",
            brand = BrandTags.KANG_SHIFU,
            protein = 8.67,
            fat = 20.5,
            carbs = 65.83,
            source = SRC_BOOH,
            note = "仅面饼，不含调料包"
        ),
        of("统一老坛酸菜牛肉面", BrandTags.TONG_YI, 6.6, 20.1, 50.7, SRC_THIRD),
        of(
            name = "汤达人日式豚骨拉面",
            brand = BrandTags.TANG_DA_REN,
            protein = 7.85,
            fat = 21.35,
            carbs = 50.7,
            source = SRC_THIRD,
            note = "来源为区间，取中值：热量 426-432 kcal，蛋白 7.7-8g，脂肪 20.7-22g，碳水 49.4-52g"
        ),
        of("辛拉面（农心，袋装）", BrandTags.NONGSHIM, 8.9, 14.0, 67.0, SRC_OFFICIAL),
        of("辛拉面（农心，台湾版）", BrandTags.NONGSHIM, 9.5, 13.5, 66.0, SRC_OFFICIAL),
        of("辛拉面（辛辣白菜炒面）", BrandTags.NONGSHIM, 7.2, 12.8, 64.3, SRC_OFFICIAL),
        of("出前一丁麻油味（袋装）", BrandTags.NISSIN, 9.8, 19.0, 63.4, SRC_OFFICIAL),
        of(
            name = "出前一丁麻油味（大盛杯面）",
            brand = BrandTags.NISSIN,
            protein = 8.7,
            fat = 18.2,
            carbs = 0.0,
            source = SRC_OFFICIAL,
            note = "碳水数据暂缺（来源标注 —）；来源热量 445 kcal/100g"
        ),
        of("日清合味道鲜虾味（杯面）", BrandTags.NISSIN, 12.2, 16.5, 62.1, SRC_OFFICIAL),
        of("今麦郎雪菜肉丝面", BrandTags.JINMAILANG, 10.5, 22.0, 63.0, SRC_OFFICIAL)
    )

    // ------------------------------------------------------------ 香肠

    private fun sausages(): List<FoodEntity> = listOf(
        of("双汇Pro猪肉香肠", BrandTags.SHUANGHUI, 14.5, 19.0, 3.5, SRC_OFFICIAL),
        of("同利德哈尔滨风味红肠", BrandTags.TONGLIDE, 10.6, 8.0, 8.0, SRC_OFFICIAL)
    )

    // ------------------------------------------------------ 碳酸饮料

    private fun carbonated(): List<FoodEntity> = listOf(
        drink("可口可乐", BrandTags.COCA_COLA, 0.0, 0.0, 10.8, SRC_OFFICIAL),
        drink("百事可乐", BrandTags.PEPSI, 0.0, 0.0, 11.0, SRC_THIRD),
        drink("芬达橙味", BrandTags.FANTA, 0.0, 0.0, 10.6, SRC_THIRD),
        drink("美年达橙味", BrandTags.MIRINDA, 0.0, 0.0, 12.8, SRC_THIRD),
        drink("雪碧", BrandTags.SPRITE, 0.0, 0.0, 8.6, SRC_OFFICIAL),
        drink("北冰洋柠檬汽水", BrandTags.BEIBINGYANG, 0.0, 0.0, 10.9, SRC_OFFICIAL),
        drink("冰峰橙味汽水", BrandTags.BINGFENG, 0.0, 0.0, 8.0, SRC_OFFICIAL),
        drink("冰峰果果金桔柠檬", BrandTags.BINGFENG, 0.0, 0.0, 8.4, SRC_OFFICIAL)
    )

    // -------------------------------------------------- 果汁 / 果味饮料

    private fun juices(): List<FoodEntity> = listOf(
        drink("美汁源混合橙汁", BrandTags.MINUTE_MAID, 0.0, 0.0, 13.0, SRC_OFFICIAL),
        drink("美汁源酷儿苹果", BrandTags.MINUTE_MAID, 0.0, 0.0, 12.0, SRC_OFFICIAL),
        drink("新奇士橙汁汽水", BrandTags.SUNKIST, 0.0, 0.0, 11.0, SRC_OFFICIAL),
        drink("美粒果果粒橙", BrandTags.MINUTE_MAID_PULPY, 0.0, 0.3, 10.0, SRC_OFFICIAL),
        drink(
            name = "果汁先生橙汁饮品",
            brand = BrandTags.MR_JUICE,
            protein = 0.0,
            fat = 0.0,
            carbs = 11.0,
            source = SRC_OFFICIAL,
            note = "蛋白质与脂肪来源标注为微量（Trace），此处记 0"
        ),
        drink(
            name = "维他黑加仑子汁饮品",
            brand = BrandTags.VITA,
            protein = 0.0,
            fat = 0.0,
            carbs = 11.0,
            source = SRC_OFFICIAL,
            note = "脂肪来源标注为微量（Trace），此处记 0"
        ),
        drink(
            name = "王老吉石榴复合果汁饮料",
            brand = BrandTags.WANG_LAO_JI,
            protein = 0.0,
            fat = 0.0,
            carbs = 10.0,
            source = SRC_THIRD,
            note = "蛋白质、脂肪数据暂缺（来源标注 —）；来源热量 41 kcal/100ml"
        )
    )

    // ------------------------------------------------------------ 茶饮料

    private fun teas(): List<FoodEntity> = listOf(
        drink("康师傅冰红茶", BrandTags.KANG_SHIFU, 0.0, 0.0, 9.7, SRC_THIRD),
        drink("海之言", BrandTags.HAI_ZHI_YAN, 0.0, 0.0, 7.1, SRC_THIRD)
    )

    // ------------------------------------------------------ 奶茶 / 含乳

    private fun milkTeas(): List<FoodEntity> = listOf(
        drink("统一阿萨姆奶茶", BrandTags.TONG_YI, 0.5, 0.6, 7.4, SRC_OFFICIAL),
        drink("统一麦香阿萨姆奶茶", BrandTags.TONG_YI, 0.5, 0.6, 7.4, SRC_OFFICIAL),
        drink("李子园草莓风味乳饮料", BrandTags.LIZIYUAN, 1.0, 1.2, 4.8, SRC_THIRD),
        drink("怡宝小主菌乳味饮料", BrandTags.C_ESTBON, 0.0, 0.0, 10.9, SRC_THIRD),
        drink("Oatly原味燕麦露", BrandTags.OATLY, 1.0, 1.5, 6.6, SRC_THIRD),
        drink("维他奶咖啡拿铁味豆乳", BrandTags.VITA, 1.3, 1.9, 6.1, SRC_THIRD),
        drink(
            name = "子母朱古力牛奶饮品",
            brand = BrandTags.DUTCH_LADY,
            protein = 0.0,
            fat = 1.8,
            carbs = 10.3,
            source = SRC_HK_NUTRITIONIST,
            note = "蛋白质数据暂缺（来源标注 —）；来源热量 70 kcal/100ml"
        ),
        drink(
            name = "子母DHA+牛奶饮品",
            brand = BrandTags.DUTCH_LADY,
            protein = 0.0,
            fat = 2.5,
            carbs = 5.6,
            source = SRC_HK_NUTRITIONIST,
            note = "蛋白质数据暂缺（来源标注 —）；来源热量 57 kcal/100ml"
        )
    )

    // ------------------------------------------------------------ 能量饮料

    private fun energyDrinks(): List<FoodEntity> = listOf(
        drink("红牛能量饮料", BrandTags.RED_BULL, 0.0, 0.0, 12.1, SRC_CDC),
        drink("红牛白桃风味", BrandTags.RED_BULL, 0.0, 0.0, 10.7, SRC_OFFICIAL),
        drink("魔爪", BrandTags.MONSTER, 0.0, 0.0, 11.0, SRC_OFFICIAL_TEST),
        drink(
            name = "CULT Energy Drink",
            brand = BrandTags.CULT,
            protein = 0.0,
            fat = 0.0,
            carbs = 9.8,
            source = SRC_OFFICIAL_TEST,
            note = "蛋白质、脂肪数据暂缺（来源标注 —）；来源热量 42 kcal/100ml"
        ),
        drink(
            name = "佳得乐（蓝莓味）",
            brand = BrandTags.GATORADE,
            protein = 0.0,
            fat = 0.0,
            carbs = 6.0,
            source = SRC_THIRD,
            note = "蛋白质数据暂缺（来源标注 —）；来源热量 24 kcal/100ml"
        )
    )

    // ------------------------------------------------------------ 啤酒

    private fun beers(): List<FoodEntity> = listOf(
        drink(
            name = "青岛啤酒",
            brand = BrandTags.TSINGTAO,
            protein = 0.38,
            fat = 0.01,
            carbs = 3.5,
            source = SRC_THIRD,
            note = "来源热量 42 kcal/100ml；酒精热量不计入碳蛋脂，反算值会偏低"
        ),
        drink(
            name = "雪花啤酒（海拉尔经典）",
            brand = BrandTags.SNOW,
            protein = 0.0,
            fat = 0.0,
            carbs = 3.8,
            source = SRC_THIRD,
            note = "蛋白质、脂肪数据暂缺（来源标注 —）；来源热量 43 kcal/100ml；酒精热量不计入碳蛋脂"
        ),
        drink("燕京啤酒", BrandTags.YANJING, 0.3, 0.0, 4.0, SRC_CDC),
        drink("百威啤酒", BrandTags.BUDWEISER, 1.3, 0.0, 10.6, SRC_OFFICIAL),
        drink(
            name = "哈尔滨啤酒",
            brand = BrandTags.HARBIN,
            protein = 0.0,
            fat = 0.0,
            carbs = 3.0,
            source = SRC_OFFICIAL_TEST,
            note = "碳水来源标注为「≥3g」，此处记 3；来源热量 34 kcal/100ml"
        )
    )

    // --------------------------------------------------------- 星巴克（按份）

    /**
     * 星巴克按「每份」公布，走 `food_variants`，存原始数值、不做换算。
     * 咖啡因食物表里没有字段，写进备注。
     */
    suspend fun installStarbucks(foodDao: FoodDao, variantDao: FoodVariantDao) {
        starbucks().forEach { item ->
            if (foodDao.countWithName(item.name, 0L) > 0) return@forEach
            val foodId = foodDao.insert(
                FoodEntity(
                    name = item.name,
                    // 按份计量的食物不使用每 100g 列，营养全在 food_variants 里。
                    carbsPer100g = 0.0,
                    proteinPer100g = 0.0,
                    fatPer100g = 0.0,
                    servingSizeGrams = 0.0,
                    note = item.note,
                    category = FoodCategory.BRAND.name,
                    brandTag = BrandTags.STARBUCKS,
                    nutritionTags = "",
                    dailyRecommendation = "",
                    dataSource = SRC_STARBUCKS,
                    credibility = FoodCredibility.OFFICIAL.name,
                    hasVariants = true,
                    sourceNote = "官方公布的是每份（Grande 473ml）数值"
                )
            )
            variantDao.insertAll(
                listOf(
                    FoodVariantEntity(
                        foodId = foodId,
                        specName = "Grande 473ml",
                        position = 0,
                        kilojoules = item.kcal * 4.184,
                        kcal = item.kcal,
                        proteinG = item.protein,
                        fatG = item.fat,
                        carbsG = item.carbs,
                        sodiumMg = 0.0,
                        calciumMg = 0.0
                    )
                )
            )
        }
    }

    private data class ServingDrink(
        val name: String,
        val kcal: Double,
        val protein: Double,
        val fat: Double,
        val carbs: Double,
        val note: String
    )

    private fun starbucks(): List<ServingDrink> = listOf(
        ServingDrink(
            name = "卡布奇诺（全脂奶 Grande 473ml）",
            kcal = 174.0,
            protein = 11.9,
            fat = 9.4,
            carbs = 12.7,
            note = "咖啡因 9.6mg（Starbucks UK 官方营养手册如此标注）"
        ),
        ServingDrink(
            name = "卡布奇诺（燕麦奶 Grande 473ml）",
            kcal = 118.0,
            protein = 1.8,
            fat = 5.1,
            carbs = 7.9,
            note = "咖啡因数据暂缺（来源标注 —）"
        )
    )

    // ------------------------------------------------------------- helper

    /** 每 100g 的包装食品。 */
    private fun of(
        name: String,
        brand: String,
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
        servingSizeGrams = 100.0,
        note = note,
        category = FoodCategory.BRAND.name,
        brandTag = brand,
        nutritionTags = "",
        dailyRecommendation = "",
        dataSource = source,
        credibility = credibilityFor(source).name,
        hasVariants = false,
        sourceNote = ""
    )

    /**
     * 每 100ml 的饮料。备注里一定会写明「每 100ml」——应用内部按每 100g 存，
     * 液体按 100ml 记是最接近的读法，不能让用户以为这是每 100g。
     */
    private fun drink(
        name: String,
        brand: String,
        protein: Double,
        fat: Double,
        carbs: Double,
        source: String,
        note: String = ""
    ): FoodEntity = of(
        name = name,
        brand = brand,
        protein = protein,
        fat = fat,
        carbs = carbs,
        source = source,
        note = if (note.isBlank()) "每 100ml" else "每 100ml · $note"
    )
}
