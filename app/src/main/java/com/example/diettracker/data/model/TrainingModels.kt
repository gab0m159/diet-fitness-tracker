package com.example.diettracker.data.model

/**
 * Enums and constants for the training + body-metrics module.
 *
 * Every enum carries its own display label and the numeric parameters the
 * calculators need, so the UI never hard-codes a coefficient.
 */

/** Biological sex. Only used by the BMR formulas. */
enum class Sex(val label: String) {
    MALE("男"),
    FEMALE("女");

    companion object {
        fun fromStorage(value: String): Sex =
            entries.firstOrNull { it.name == value } ?: MALE
    }
}

/**
 * Fixed activity multipliers applied to BMR to get TDEE.
 * Deliberately a small fixed list rather than a free-form number.
 */
enum class ActivityLevel(
    val label: String,
    val multiplier: Double,
    val description: String
) {
    SEDENTARY("久坐", 1.2, "几乎不运动，办公室久坐"),
    LIGHT("轻度", 1.375, "每周运动 1-3 天"),
    MODERATE("中度", 1.55, "每周运动 3-5 天"),
    HIGH("高度", 1.725, "每周运动 6-7 天");

    companion object {
        fun fromStorage(value: String): ActivityLevel =
            entries.firstOrNull { it.name == value } ?: MODERATE
    }
}

/**
 * 用户的训练目标，决定**每公斤体重的碳蛋脂克数**。
 *
 * v7 起宏量不再按热量百分比切，而是按体重直接给（见
 * `NutritionCalculator.perKgTargets`）：
 *
 * | 模式 | 碳水 g/kg | 蛋白 g/kg | 脂肪 g/kg |
 * |------|-----------|-----------|-----------|
 * | 增肌 | 4.0       | 1.8       | 1.0       |
 * | 保持 | 3.5       | 1.6       | 0.9       |
 * | 减脂 | 2.5       | 2.0       | 0.8       |
 *
 * 蛋白质依据 ISSN 2017 立场声明：增肌/维持 1.4-2.0 g/kg 足够；减脂期取 2.0
 * （高于增肌以应对热量缺口，但不进入 2.3-3.1 的备赛区间）。
 *
 * [calorieFactor] 保留下来只用于**参考展示**（把 TDEE 与实际目标热量放在一起对比），
 * 不再参与宏量计算。
 */
enum class GoalMode(
    val label: String,
    val calorieFactor: Double,
    val description: String
) {
    BULK("增肌", 1.15, "碳水 4.0 / 蛋白 1.8 / 脂肪 1.0 g/kg"),
    MAINTAIN("保持", 1.00, "碳水 3.5 / 蛋白 1.6 / 脂肪 0.9 g/kg"),
    CUT("减脂", 0.80, "碳水 2.5 / 蛋白 2.0 / 脂肪 0.8 g/kg");

    companion object {
        fun fromStorage(value: String): GoalMode =
            entries.firstOrNull { it.name == value } ?: MAINTAIN
    }
}

/** Training split, i.e. how many distinct training days rotate. */
enum class SplitType(
    val label: String,
    val dayCount: Int,
    val summary: String
) {
    THREE("三分化", 3, "胸+三头 / 背+二头 / 腿+肩"),
    FIVE("五分化", 5, "胸 / 背 / 腿 / 肩 / 手臂"),
    SIX("六分化", 6, "胸 / 背 / 腿 / 肩 / 手臂 / 弱项");

    companion object {
        fun fromStorage(value: String): SplitType =
            entries.firstOrNull { it.name == value } ?: THREE
    }
}

/**
 * Training objective, which sets the default rep range, rest period and
 * therefore the progression rules.
 */
enum class TrainingObjective(
    val label: String,
    val minReps: Int,
    val maxReps: Int,
    val restSecondsMin: Int,
    val restSecondsMax: Int,
    val description: String
) {
    POWERLIFTING("力量举", 1, 6, 120, 300, "1-6 次/组，组间休息 2-5 分钟"),
    HYPERTROPHY("肌肥大", 6, 15, 60, 120, "6-15 次/组（核心 8-12），休息 60-120 秒"),
    PHYSIQUE("健体", 8, 12, 90, 150, "8-12 次/组，介于力量与肌肥大之间");

    companion object {
        fun fromStorage(value: String): TrainingObjective =
            entries.firstOrNull { it.name == value } ?: HYPERTROPHY
    }
}

/** Muscle group / body region a training day or exercise targets. */
enum class BodyPart(val label: String) {
    CHEST("胸"),
    BACK("背"),
    LEGS("腿"),
    SHOULDERS("肩"),
    ARMS("手臂"),
    GLUTES("臀"),
    CORE("核心"),
    FULL_BODY("全身"),
    REST("休息");

    companion object {
        fun fromStorage(value: String): BodyPart =
            entries.firstOrNull { it.name == value } ?: FULL_BODY
    }
}

/**
 * Whether an exercise is upper- or lower-body, which sets its **default** load
 * increment. The user can override the increment per exercise; a custom exercise
 * has no region and uses [DEFAULT_INCREMENT_KG].
 */
enum class BodyRegion(val label: String, val incrementKg: Double) {
    UPPER("上肢", 2.5),
    LOWER("下肢", 5.0);

    companion object {
        /** Weight increases are bigger for lower-body lifts. */
        const val DEFAULT_INCREMENT_KG = 2.5
    }
}

/**
 * State of one day's workout record.
 *
 * v6 dropped [POSTPONED]: 「延期」不再是某条记录的终态，而是把整个训练日的到期
 * 日往后挪一天，记录本身保持原样（不写失败也不写跳过）。
 */
enum class SessionStatus(val label: String) {
    /** Trained as planned. */
    COMPLETED("已完成"),

    /** "今天不练" — the day was abandoned and its schedule advanced. */
    SKIPPED("已跳过"),

    /** Created but not finished yet. */
    IN_PROGRESS("进行中");

    companion object {
        fun fromStorage(value: String): SessionStatus =
            entries.firstOrNull { it.name == value } ?: IN_PROGRESS
    }
}

/** Training intensity preset, used for the MET-based burn estimate. */
enum class TrainingIntensity(
    val label: String,
    val met: Double,
    val description: String
) {
    HEAVY("大重量力量训练", 6.0, "MET 6.0"),
    MODERATE("中等强度", 3.5, "MET 3.5");

    companion object {
        fun fromStorage(value: String): TrainingIntensity =
            entries.firstOrNull { it.name == value } ?: HEAVY
    }
}

/** Where a bundled food came from, and how much to trust it. */
enum class FoodCredibility(val label: String) {
    OFFICIAL("官方"),
    THIRD_PARTY("第三方"),
    ESTIMATED("估算");

    companion object {
        fun fromStorage(value: String): FoodCredibility =
            entries.firstOrNull { it.name == value } ?: ESTIMATED
    }
}

/** Top-level grouping used by the food library tabs. */
enum class FoodCategory(val label: String) {
    /** Foods the user created. Always the highest priority. */
    USER("我的食物"),

    /** Fast-food / chain products, tagged with a brand. */
    BRAND("品牌食品"),

    /** Foods carrying a "quality source" nutrition tag. */
    RECOMMENDED("推荐食品");

    companion object {
        fun fromStorage(value: String): FoodCategory =
            entries.firstOrNull { it.name == value } ?: USER
    }
}

/** The three "quality source" tags a food can carry. */
object NutritionTags {
    const val QUALITY_CARBS = "优质碳水"
    const val QUALITY_PROTEIN = "优质蛋白质"
    const val QUALITY_FAT = "优质脂肪"

    val all = listOf(QUALITY_CARBS, QUALITY_PROTEIN, QUALITY_FAT)
}

/** Brand labels used for the bundled fast-food data. */
object BrandTags {
    const val MCDONALDS = "麦当劳"
    const val KFC = "肯德基"
    const val LUCKIN = "瑞幸"
    const val MANNER = "Manner"
    const val SAM = "山姆"

    // ---- 包装食品（泡面 / 香肠）----------------------------------------
    const val KANG_SHIFU = "康师傅"
    const val TONG_YI = "统一"
    const val TANG_DA_REN = "汤达人"
    const val NONGSHIM = "农心"
    const val NISSIN = "日清"
    const val JINMAILANG = "今麦郎"
    const val SHUANGHUI = "双汇"
    const val TONGLIDE = "同利德"

    // ---- 饮料 -----------------------------------------------------------
    const val COCA_COLA = "可口可乐"
    const val PEPSI = "百事"
    const val FANTA = "芬达"
    const val MIRINDA = "美年达"
    const val SPRITE = "雪碧"
    const val BEIBINGYANG = "北冰洋"
    const val BINGFENG = "冰峰"
    const val MINUTE_MAID = "美汁源"
    const val SUNKIST = "新奇士"
    const val MINUTE_MAID_PULPY = "美粒果"
    const val MR_JUICE = "果汁先生"
    const val VITA = "维他"
    const val WANG_LAO_JI = "王老吉"
    const val HAI_ZHI_YAN = "海之言"
    const val LIZIYUAN = "李子园"
    const val C_ESTBON = "怡宝"
    const val OATLY = "Oatly"
    const val DUTCH_LADY = "子母"
    const val RED_BULL = "红牛"
    const val MONSTER = "魔爪"
    const val CULT = "CULT"
    const val GATORADE = "佳得乐"

    // ---- 啤酒 / 咖啡 -----------------------------------------------------
    const val TSINGTAO = "青岛啤酒"
    const val SNOW = "雪花"
    const val YANJING = "燕京"
    const val BUDWEISER = "百威"
    const val HARBIN = "哈尔滨"
    const val STARBUCKS = "星巴克"

    /**
     * 用于食物库的品牌筛选（界面只显示当前列表里真实存在的品牌）。
     */
    val all = listOf(
        MCDONALDS, KFC, LUCKIN, MANNER, SAM,
        KANG_SHIFU, TONG_YI, TANG_DA_REN, NONGSHIM, NISSIN, JINMAILANG,
        SHUANGHUI, TONGLIDE,
        COCA_COLA, PEPSI, FANTA, MIRINDA, SPRITE, BEIBINGYANG, BINGFENG,
        MINUTE_MAID, SUNKIST, MINUTE_MAID_PULPY, MR_JUICE, VITA, WANG_LAO_JI,
        HAI_ZHI_YAN, LIZIYUAN, C_ESTBON, OATLY, DUTCH_LADY,
        RED_BULL, MONSTER, CULT, GATORADE,
        TSINGTAO, SNOW, YANJING, BUDWEISER, HARBIN, STARBUCKS
    )
}
