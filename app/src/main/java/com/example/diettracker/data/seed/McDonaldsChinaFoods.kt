package com.example.diettracker.data.seed

import com.example.diettracker.data.model.BrandTags
import com.example.diettracker.data.model.FoodCategory
import com.example.diettracker.data.model.FoodCredibility

/**
 * 麦当劳中国官方营养数据（仅单品）。
 *
 * ## 数据来源
 *
 * 麦当劳中国官网营养计算器：
 * https://www.mcdonalds.com.cn/nutrition_calculator
 *
 * 页面注明「上述营养成分数据更新时间为 2025 年 4 月」。可信度：官方。
 *
 * ## 为什么用「按份」而不是「每 100g」
 *
 * 官方只公布**单份餐品**的营养成分，且**不提供份量克重**。因此这里把数值原样
 * 存进 `food_variants` 表（每份营养），不做任何 per-100g 折算，保证与官网公布值
 * 完全一致。多个规格（如薯条 迷你/小/中/大）作为同一个食物的多个 variant 存储，
 * 添加记录时用「规格 + 份数」两段选择即可。
 *
 * ## 收录范围
 *
 * 只收录**可单独购买的单品**，共 86 个食物 / 129 个规格。
 * 「500 大卡套餐」属于套餐组合，未收录。
 *
 * 字段顺序：能量(千焦)、能量(大卡)、蛋白质(克)、脂肪(克)、碳水化合物(克)、
 * 钠(毫克)、钙(毫克)。
 */
object McDonaldsChinaFoods {

    /** 数据来源，写入食物库并展示给用户。 */
    const val SOURCE = "麦当劳中国官网营养计算器"

    /** 页面注明的数据更新时间。 */
    const val SOURCE_NOTE = "数据更新至 2025 年 4 月（官网营养计算器）"

    /** 一个规格：名称 + 每份营养。 */
    data class Variant(
        val spec: String,
        val kilojoules: Double,
        val kcal: Double,
        val proteinG: Double,
        val fatG: Double,
        val carbsG: Double,
        val sodiumMg: Double,
        val calciumMg: Double
    )

    /** 一个单品及其全部规格。 */
    data class Item(
        val name: String,
        val variants: List<Variant>
    )

    /** 品牌标签。 */
    val BRAND: String get() = BrandTags.MCDONALDS

    /**
     * 全部 86 个单品。
     *
     * 顺序沿用官网分类顺序（汉堡和卷 → 饮品 → 小食 → 甜品 → 早餐 → 开心乐园餐
     * → McCafé）。
     */
    fun all(): List<Item> = listOf(
        // 字段顺序：v(规格, 千焦, 大卡, 蛋白质g, 脂肪g, 碳水g, 钠mg, 钙mg)
        mcd(
            name = "巨无霸",
            variants = listOf(
                v("1个", 2146, 513, 27, 26, 42, 961, 171),
            )
        ),
        mcd(
            name = "汉堡包",
            variants = listOf(
                v("1个", 1039, 248, 13, 8, 29, 492, 61),
            )
        ),
        mcd(
            name = "麦辣鸡腿汉堡",
            variants = listOf(
                v("1个", 2029, 485, 24, 24, 42, 1208, 154),
            )
        ),
        mcd(
            name = "板烧鸡腿堡",
            variants = listOf(
                v("1个", 1638, 391, 23, 17, 35, 1041, 93),
            )
        ),
        mcd(
            name = "麦香鸡",
            variants = listOf(
                v("1个", 1546, 370, 15, 17, 39, 731, 73),
            )
        ),
        mcd(
            name = "麦香鱼",
            variants = listOf(
                v("1个", 1359, 325, 16, 13, 35, 556, 99),
            )
        ),
        mcd(
            name = "吉士汉堡包",
            variants = listOf(
                v("1个", 1231, 294, 16, 12, 30, 673, 144),
            )
        ),
        mcd(
            name = "双层吉士汉堡",
            variants = listOf(
                v("1个", 1796, 429, 27, 22, 31, 1017, 232),
            )
        ),
        mcd(
            name = "不素之霸双层牛堡",
            variants = listOf(
                v("1个", 2062, 493, 28, 27, 34, 1012, 75),
            )
        ),
        mcd(
            name = "双层深海鳕鱼堡",
            variants = listOf(
                v("1个", 2029, 485, 28, 21, 46, 917, 147),
            )
        ),
        mcd(
            name = "培根安格斯厚牛堡",
            variants = listOf(
                v("1个", 2959, 707, 34, 44, 43, 1037, 161),
            )
        ),
        mcd(
            name = "芝士安格斯厚牛堡",
            variants = listOf(
                v("1个", 2914, 696, 32, 44, 43, 897, 150),
            )
        ),
        mcd(
            name = "培根蔬萃双层牛堡",
            variants = listOf(
                v("1个", 1850, 442, 24, 23, 34, 728, 73),
            )
        ),
        mcd(
            name = "可口可乐",
            variants = listOf(
                v("小", 446, 107, 0, 0, 26, 0, 7),
                v("中", 616, 147, 0, 0, 36, 0, 10),
                v("大", 936, 224, 0, 0, 55, 0, 15),
            )
        ),
        mcd(
            name = "无糖可口可乐",
            variants = listOf(
                v("小", 0, 0, 0, 0, 0, 25, 0),
                v("中", 0, 0, 0, 0, 1, 35, 0),
                v("大", 0, 0, 0, 0, 1, 53, 0),
            )
        ),
        mcd(
            name = "阳光柠檬红茶",
            variants = listOf(
                v("小", 354, 85, 0, 0, 21, 12, 0),
                v("中", 488, 117, 0, 0, 29, 17, 0),
                v("大", 743, 178, 0, 0, 43, 25, 0),
            )
        ),
        mcd(
            name = "雪碧",
            variants = listOf(
                v("小", 319, 76, 0, 0, 19, 21, 0),
                v("中", 440, 105, 0, 0, 26, 29, 0),
                v("大", 669, 160, 0, 0, 39, 45, 0),
            )
        ),
        mcd(
            name = "怡泉+C",
            variants = listOf(
                v("中", 461, 110, 0, 0, 27, 0, 0),
                v("大", 701, 168, 0, 0, 41, 0, 0),
            )
        ),
        mcd(
            name = "阳光橙麦炫酷",
            variants = listOf(
                v("", 955, 228, 1, 2, 50, 77, 52),
            )
        ),
        mcd(
            name = "【美汁源】“黄金橙橙”",
            variants = listOf(
                v("中", 691, 165, 0, 0, 41, 53, 0),
            )
        ),
        mcd(
            name = "100%苹果汁",
            variants = listOf(
                v("1盒", 368, 88, 0, 0, 21, 0, 0),
            )
        ),
        mcd(
            name = "优品豆浆",
            variants = listOf(
                v("小", 633, 151, 6, 3, 25, 32, 15),
                v("大", 844, 202, 8, 4, 33, 43, 21),
            )
        ),
        mcd(
            name = "纯牛奶（盒装）",
            variants = listOf(
                v("", 541, 129, 7, 7, 10, 73, 231),
            )
        ),
        mcd(
            name = "锡兰红茶",
            variants = listOf(
                v("", 8, 2, 0, 0, 0, 0, 5),
            )
        ),
        mcd(
            name = "纯悦",
            variants = listOf(
                v("", 0, 0, 0, 0, 0, 0, 0),
            )
        ),
        mcd(
            name = "薯条",
            variants = listOf(
                v("迷你", 444, 106, 2, 4, 14, 61, 6),
                v("小", 877, 210, 3, 9, 28, 120, 13),
                v("中", 1210, 289, 4, 12, 38, 165, 18),
                v("大", 1587, 379, 6, 16, 50, 216, 23),
            )
        ),
        mcd(
            name = "脆脆薯条",
            variants = listOf(
                v("中", 1168, 279, 4, 14, 32, 540, 13),
            )
        ),
        mcd(
            name = "麦辣鸡翅",
            variants = listOf(
                v("2块", 937, 224, 13, 15, 9, 537, 13),
            )
        ),
        mcd(
            name = "薄皮焦香V翅",
            variants = listOf(
                v("2块", 804, 192, 17, 11, 6, 594, 15),
            )
        ),
        mcd(
            name = "麦乐鸡 (5块)",
            variants = listOf(
                v("5块", 890, 213, 12, 12, 13, 422, 9),
            )
        ),
        mcd(
            name = "那么大鸡排（椒盐风味）",
            variants = listOf(
                v("1个", 1612, 385, 24, 21, 24, 996, 13),
            )
        ),
        mcd(
            name = "玉米杯",
            variants = listOf(
                v("小", 223, 53, 2, 1, 7, 1, 6),
                v("大", 365, 87, 4, 1, 12, 2, 10),
            )
        ),
        mcd(
            name = "苹果片",
            variants = listOf(
                v("", 133, 32, 0, 0, 7, 0, 18),
            )
        ),
        mcd(
            name = "圆筒冰淇淋",
            variants = listOf(
                v("1个", 391, 93, 2, 3, 14, 36, 77),
            )
        ),
        mcd(
            name = "奥利奥麦旋风",
            variants = listOf(
                v("1个", 1115, 266, 5, 9, 40, 116, 185),
            )
        ),
        mcd(
            name = "草莓麦旋风",
            variants = listOf(
                v("1个", 1234, 295, 6, 9, 47, 122, 186),
            )
        ),
        mcd(
            name = "朱古力新地",
            variants = listOf(
                v("1个", 1178, 282, 5, 9, 44, 126, 182),
            )
        ),
        mcd(
            name = "草莓新地",
            variants = listOf(
                v("1个", 1027, 245, 4, 6, 43, 84, 158),
            )
        ),
        mcd(
            name = "迷你朱古力新地",
            variants = listOf(
                v("1份", 586, 140, 3, 4, 22, 63, 90),
            )
        ),
        mcd(
            name = "迷你草莓新地",
            variants = listOf(
                v("1份", 511, 122, 2, 3, 21, 42, 78),
            )
        ),
        mcd(
            name = "香芋派",
            variants = listOf(
                v("1个", 971, 232, 2, 12, 28, 159, 7),
            )
        ),
        mcd(
            name = "菠萝派",
            variants = listOf(
                v("1个", 925, 221, 2, 11, 27, 147, 5),
            )
        ),
        mcd(
            name = "猪柳麦满分",
            variants = listOf(
                v("1个", 1288, 308, 16, 16, 24, 781, 213),
            )
        ),
        mcd(
            name = "猪柳蛋麦满分",
            variants = listOf(
                v("1个", 1618, 387, 23, 21, 25, 846, 243),
            )
        ),
        mcd(
            name = "双层猪柳蛋麦满分",
            variants = listOf(
                v("1个", 2148, 513, 29, 32, 26, 1210, 249),
            )
        ),
        mcd(
            name = "火腿扒麦满分",
            variants = listOf(
                v("1个", 1092, 261, 14, 12, 24, 608, 131),
            )
        ),
        mcd(
            name = "大脆鸡扒麦满分",
            variants = listOf(
                v("1个", 1510, 361, 16, 17, 35, 804, 130),
            )
        ),
        mcd(
            name = "原味板烧鸡腿麦满分",
            variants = listOf(
                v("", 1029, 246, 15, 9, 25, 611, 133),
            )
        ),
        mcd(
            name = "双层原味板烧鸡腿麦满分",
            variants = listOf(
                v("1个", 1486, 355, 23, 17, 27, 984, 138),
            )
        ),
        mcd(
            name = "原味板烧鸡腿炒双蛋堡",
            variants = listOf(
                v("1个", 1754, 419, 27, 21, 30, 715, 116),
            )
        ),
        mcd(
            name = "猪柳炒双蛋堡",
            variants = listOf(
                v("1个", 1827, 437, 25, 24, 29, 705, 116),
            )
        ),
        mcd(
            name = "火腿扒早安营养卷",
            variants = listOf(
                v("1个", 1857, 444, 16, 24, 39, 1065, 108),
            )
        ),
        mcd(
            name = "图林根香肠早安营养卷",
            variants = listOf(
                v("1个", 1779, 425, 14, 23, 38, 977, 68),
            )
        ),
        mcd(
            name = "皮蛋鸡肉粥",
            variants = listOf(
                v("1份", 558, 133, 6, 3, 20, 611, 20),
            )
        ),
        mcd(
            name = "雪菜脆笋鸡肉粥",
            variants = listOf(
                v("1份", 504, 120, 4, 2, 22, 552, 14),
            )
        ),
        mcd(
            name = "脆薯饼",
            variants = listOf(
                v("1个", 612, 146, 1, 9, 14, 311, 7),
            )
        ),
        mcd(
            name = "脆香油条",
            variants = listOf(
                v("1个", 844, 202, 5, 12, 19, 230, 18),
            )
        ),
        mcd(
            name = "德式图林根香肠",
            variants = listOf(
                v("1个", 366, 87, 5, 6, 2, 293, 5),
            )
        ),
        mcd(
            name = "儿童鱼排堡",
            variants = listOf(
                v("1个", 1126, 269, 15, 6, 36, 442, 58),
            )
        ),
        mcd(
            name = "麦乐鸡（4块）",
            variants = listOf(
                v("4块", 712, 170, 10, 10, 11, 337, 7),
            )
        ),
        mcd(
            name = "薯条（迷你）",
            variants = listOf(
                v("薯条（迷你）", 444, 106, 2, 4, 14, 61, 6),
            )
        ),
        mcd(
            name = "玉米杯 (小)",
            variants = listOf(
                v("小", 223, 53, 2, 1, 7, 1, 6),
            )
        ),
        mcd(
            name = "美汁源100%苹果汁 (小)",
            variants = listOf(
                v("小", 368, 88, 0, 0, 21, 0, 0),
            )
        ),
        mcd(
            name = "冰奶铁",
            variants = listOf(
                v("小", 358, 86, 5, 4, 8, 44, 144),
                v("中", 618, 148, 8, 7, 13, 77, 251),
                v("大", 678, 162, 9, 7, 15, 85, 275),
            )
        ),
        mcd(
            name = "热奶铁",
            variants = listOf(
                v("小", 559, 134, 8, 6, 12, 72, 231),
                v("中", 779, 186, 11, 8, 17, 99, 320),
                v("大", 1040, 249, 14, 11, 22, 134, 431),
            )
        ),
        mcd(
            name = "冰燕麦奶铁",
            variants = listOf(
                v("小", 322, 77, 2, 3, 10, 50, 155),
                v("中", 554, 132, 3, 5, 17, 87, 270),
                v("大", 608, 145, 4, 5, 19, 96, 296),
            )
        ),
        mcd(
            name = "热燕麦奶铁",
            variants = listOf(
                v("小", 500, 120, 3, 5, 16, 81, 248),
                v("中", 697, 167, 4, 6, 22, 112, 345),
                v("大", 929, 222, 5, 8, 29, 152, 464),
            )
        ),
        mcd(
            name = "冰美式",
            variants = listOf(
                v("小", 41, 10, 1, 0, 1, 0, 6),
                v("中", 54, 13, 1, 0, 2, 0, 7),
                v("大", 60, 14, 1, 0, 2, 0, 8),
            )
        ),
        mcd(
            name = "热美式",
            variants = listOf(
                v("小", 41, 10, 1, 0, 1, 0, 6),
                v("中", 54, 13, 1, 0, 2, 0, 7),
                v("大", 60, 14, 1, 0, 2, 0, 8),
            )
        ),
        mcd(
            name = "冰焦糖玛奇朵",
            variants = listOf(
                v("中", 618, 148, 8, 7, 13, 77, 251),
            )
        ),
        mcd(
            name = "热焦糖玛奇朵",
            variants = listOf(
                // 官方公布钙为 284.7 毫克，是数据集中唯一的非整数值。
                v("中", 698.0, 167.0, 9.0, 7.0, 15.0, 88.0, 284.7),
            )
        ),
        mcd(
            name = "卡布奇诺",
            variants = listOf(
                v("中", 698, 167, 9, 7, 15, 88, 285),
                v("大", 932, 223, 13, 10, 20, 120, 385),
            )
        ),
        mcd(
            name = "冰浓浓燕麦黑巧",
            variants = listOf(
                v("中", 500, 120, 2, 5, 16, 87, 263),
                v("大", 547, 131, 3, 5, 17, 95, 388),
            )
        ),
        mcd(
            name = "热浓浓燕麦黑巧",
            variants = listOf(
                v("中", 655, 157, 3, 6, 21, 114, 344),
                v("大", 893, 213, 4, 8, 28, 156, 469),
            )
        ),
        mcd(
            name = "冰浓浓抹茶牛奶",
            variants = listOf(
                v("中", 821, 196, 8, 6, 25, 80, 275),
                v("大", 1004, 240, 9, 7, 33, 89, 314),
            )
        ),
        mcd(
            name = "热浓浓抹茶牛奶",
            variants = listOf(
                v("中", 996, 238, 11, 8, 29, 104, 351),
                v("大", 1393, 333, 14, 12, 41, 142, 482),
            )
        ),
        mcd(
            name = "冰燕麦奶",
            variants = listOf(
                v("小", 547, 131, 3, 5, 17, 95, 288),
                v("中", 833, 199, 4, 8, 26, 145, 438),
                v("大", 1000, 239, 5, 9, 32, 174, 525),
            )
        ),
        mcd(
            name = "热燕麦奶",
            variants = listOf(
                v("小", 547, 131, 3, 5, 17, 95, 288),
                v("中", 655, 157, 3, 6, 21, 114, 344),
                v("大", 893, 213, 4, 8, 28, 156, 469),
            )
        ),
        mcd(
            name = "冰浓浓黑巧",
            variants = listOf(
                v("中", 563, 135, 8, 6, 12, 77, 244),
                v("大", 617, 147, 8, 7, 13, 84, 267),
            )
        ),
        mcd(
            name = "热浓浓黑巧",
            variants = listOf(
                v("中", 738, 176, 10, 8, 15, 101, 319),
                v("大", 1006, 240, 13, 11, 21, 137, 435),
            )
        ),
        mcd(
            name = "冰牛奶",
            variants = listOf(
                v("小", 617, 147, 8, 7, 13, 84, 267),
                v("中", 939, 224, 13, 11, 19, 128, 406),
                v("大", 1127, 269, 15, 13, 23, 154, 487),
            )
        ),
        mcd(
            name = "热牛奶",
            variants = listOf(
                v("小", 617, 147, 8, 7, 13, 84, 267),
                v("中", 738, 176, 10, 8, 15, 101, 319),
                v("大", 1006, 240, 13, 11, 21, 137, 435),
            )
        ),
        mcd(
            name = "浓浓抹茶雪冰",
            variants = listOf(
                v("中", 1208, 289, 7, 11, 39, 181, 220),
                v("大", 1418, 339, 8, 12, 48, 194, 270),
            )
        ),
        mcd(
            name = "浓浓黑巧雪冰",
            variants = listOf(
                v("中", 1501, 359, 8, 12, 51, 224, 207),
                v("大", 1857, 444, 11, 14, 65, 258, 251),
            )
        ),
        mcd(
            name = "抹茶阿芙佳朵",
            variants = listOf(
                v("一杯", 613, 147, 3, 4, 23, 50, 136),
            )
        ),
        mcd(
            name = "咖啡阿芙佳朵",
            variants = listOf(
                v("一杯", 583, 139, 4, 4, 21, 49, 111),
            )
        ),
    )

    /**
     * 规格简写，供数据录入时保持可读性。
     *
     * 整数版本：绝大多数官方数值都是整数，写起来最直观。
     */
    @Suppress("LongParameterList")
    private fun v(
        spec: String,
        kilojoules: Long,
        kcal: Long,
        proteinG: Long,
        fatG: Long,
        carbsG: Long,
        sodiumMg: Long,
        calciumMg: Long
    ) = Variant(
        spec = spec,
        kilojoules = kilojoules.toDouble(),
        kcal = kcal.toDouble(),
        proteinG = proteinG.toDouble(),
        fatG = fatG.toDouble(),
        carbsG = carbsG.toDouble(),
        sodiumMg = sodiumMg.toDouble(),
        calciumMg = calciumMg.toDouble()
    )

    /**
     * 带小数的版本，用于官方个别带小数的数值（如钙 284.7 毫克）。
     */
    @Suppress("LongParameterList")
    private fun v(
        spec: String,
        kilojoules: Double,
        kcal: Double,
        proteinG: Double,
        fatG: Double,
        carbsG: Double,
        sodiumMg: Double,
        calciumMg: Double
    ) = Variant(
        spec = spec,
        kilojoules = kilojoules,
        kcal = kcal,
        proteinG = proteinG,
        fatG = fatG,
        carbsG = carbsG,
        sodiumMg = sodiumMg,
        calciumMg = calciumMg
    )

    private fun mcd(name: String, variants: List<Variant>) = Item(name, variants)
}