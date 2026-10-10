package com.example.diettracker.domain

/**
 * 运动库里的一项：名称 + MET 值。
 *
 * ## MET 是什么
 *
 * MET（代谢当量）表示这项运动的强度是静息状态的多少倍。热量按
 * `MET × 体重(kg) × 时长(小时)` 估算，所以同一个项目，体重大的人消耗更多。
 *
 * MET 值取自通行的运动能量消耗对照表（如 Compendium of Physical Activities）。
 * 它们都是**估算参考值**，个体差异很大（强度、效率、动作标准度都会影响），
 * 所以界面上允许用户直接改算出来的热量。
 *
 * @param key 稳定标识，存进数据库；改名不影响历史记录。
 * @param name 显示名。
 * @param met 代谢当量。
 * @param defaultMinutes 默认时长，用户加进来时预填。
 * @param category 分组，用于运动库的展示。
 * @param strength 是否是「撸铁」——可以额外挂动作卡片的那一项。
 */
data class SportInfo(
    val key: String,
    val name: String,
    val met: Double,
    val defaultMinutes: Int,
    val category: SportCategory,
    val strength: Boolean = false,
    /** 补充说明，例如「8 km/h」这样的强度前提。 */
    val note: String = ""
)

/** 运动库的分组。 */
enum class SportCategory(val label: String) {
    CARDIO("有氧耐力"),
    BALL("球类"),
    CLASS("操课"),
    STRENGTH("力量与其它"),

    /** 用户自建的项目，排在最后一组。 */
    CUSTOM("我创建的")
}

/**
 * 内置运动库。
 *
 * 32 项，覆盖常见的有氧、球类、操课与力量训练。每一项都带默认时长：
 * 有氧 30 分钟、拉伸 / 瑜伽 20 分钟、撸铁 60 分钟。
 */
object SportLibrary {

    /** 撸铁的 key，代码里要按它判断「这一项能挂动作卡片」。 */
    const val STRENGTH_KEY = "STRENGTH"

    val all: List<SportInfo> = listOf(
        // ---------------------------------------------------------- 有氧耐力
        SportInfo("RUNNING", "跑步", 8.3, 30, SportCategory.CARDIO, note = "约 8 km/h"),
        SportInfo("JOGGING", "慢跑", 6.0, 30, SportCategory.CARDIO, note = "约 6.4 km/h"),
        SportInfo("WALKING", "快走", 4.8, 30, SportCategory.CARDIO),
        SportInfo("CYCLING_EASY", "骑行（休闲）", 4.0, 30, SportCategory.CARDIO, note = "低于 16 km/h"),
        SportInfo("CYCLING", "骑行（中等）", 8.0, 30, SportCategory.CARDIO, note = "16-19 km/h"),
        SportInfo("SWIMMING", "游泳", 8.0, 30, SportCategory.CARDIO, note = "自由泳，中等强度"),
        SportInfo("JUMP_ROPE", "跳绳", 12.3, 15, SportCategory.CARDIO),
        SportInfo("ELLIPTICAL", "椭圆机", 5.0, 30, SportCategory.CARDIO),
        SportInfo("ROWING", "划船机", 7.0, 20, SportCategory.CARDIO),
        SportInfo("STAIRS", "爬楼梯", 8.8, 20, SportCategory.CARDIO),
        SportInfo("HIKING", "爬山", 6.5, 60, SportCategory.CARDIO),
        SportInfo("SKATING", "溜冰", 7.0, 30, SportCategory.CARDIO),
        SportInfo("SKATEBOARD", "滑板", 5.0, 30, SportCategory.CARDIO),

        // -------------------------------------------------------------- 球类
        SportInfo("BASKETBALL", "篮球", 6.5, 60, SportCategory.BALL),
        SportInfo("FOOTBALL", "足球", 7.0, 60, SportCategory.BALL),
        SportInfo("BADMINTON", "羽毛球", 5.5, 45, SportCategory.BALL),
        SportInfo("TABLE_TENNIS", "乒乓球", 4.0, 45, SportCategory.BALL),
        SportInfo("TENNIS", "网球", 7.3, 60, SportCategory.BALL),
        SportInfo("BILLIARDS", "台球", 2.5, 60, SportCategory.BALL),

        // -------------------------------------------------------------- 操课
        SportInfo("YOGA", "瑜伽", 2.5, 20, SportCategory.CLASS),
        SportInfo("PILATES", "普拉提", 3.0, 30, SportCategory.CLASS),
        SportInfo("HIIT", "HIIT", 8.0, 20, SportCategory.CLASS),
        SportInfo("DANCING", "跳舞", 5.0, 45, SportCategory.CLASS),
        SportInfo("AEROBICS", "健美操", 6.5, 40, SportCategory.CLASS),
        SportInfo("SPINNING", "动感单车", 8.5, 45, SportCategory.CLASS),

        // -------------------------------------------------------- 力量与其它
        SportInfo(
            key = STRENGTH_KEY,
            name = "撸铁",
            met = 6.0,
            defaultMinutes = 60,
            category = SportCategory.STRENGTH,
            strength = true,
            note = "大重量力量训练"
        ),
        SportInfo("BODYWEIGHT", "徒手训练", 3.8, 30, SportCategory.STRENGTH),
        SportInfo("BOXING", "拳击", 7.8, 45, SportCategory.STRENGTH),
        SportInfo("MARTIAL_ARTS", "武术", 5.3, 60, SportCategory.STRENGTH),
        SportInfo("TAICHI", "太极", 3.0, 30, SportCategory.STRENGTH),
        SportInfo("STRETCHING", "拉伸放松", 2.3, 20, SportCategory.STRENGTH),
        SportInfo("DOG_WALKING", "遛狗", 3.0, 30, SportCategory.STRENGTH),
        SportInfo("HOUSEWORK", "做家务", 3.3, 30, SportCategory.STRENGTH)
    )

    /**
     * 按分组给运动库页面用（只含内置项）。
     *
     * 只返回**有内容**的组：内置项不覆盖 `CUSTOM`（那是用户自建专用的分组），
     * 所以必须过滤掉空组，否则页面会渲染出一个空的「我创建的」标题。
     */
    fun grouped(): List<Pair<SportCategory, List<SportInfo>>> =
        SportCategory.entries
            .filter { it != SportCategory.CUSTOM }
            .map { category -> category to all.filter { it.category == category } }
            .filter { it.second.isNotEmpty() }

    fun find(key: String): SportInfo? = all.firstOrNull { it.key == key }

    /** 撸铁那一项。 */
    val strength: SportInfo get() = find(STRENGTH_KEY) ?: all.last()

    /**
     * 把用户自建的运动并进分组结果，新增一组「我创建的」放在**最后**。
     *
     * 内置项在前：它们是大多数人的常用项，自建项通常只占少数。分开放也更清楚
     * 「哪些能删、哪些不能删」。没有自建项时**不会**产生空的 CUSTOM 组。
     */
    fun groupedWithCustom(
        custom: List<com.example.diettracker.data.db.CustomSportEntity>
    ): List<Pair<SportCategory, List<SportInfo>>> {
        val base = grouped()
        if (custom.isEmpty()) return base
        val customInfos = custom.map { entity ->
            SportInfo(
                key = entity.key,
                name = entity.name,
                met = entity.met,
                defaultMinutes = entity.defaultMinutes,
                category = SportCategory.CUSTOM,
                note = "自建"
            )
        }
        return base + (SportCategory.CUSTOM to customInfos)
    }

    /**
     * 按 MET 估算热量：`MET × 体重(kg) × 时长(小时)`。
     * 结果保留一位小数。
     */
    fun estimateKcal(met: Double, bodyWeightKg: Double, minutes: Int): Double {
        if (met <= 0.0 || bodyWeightKg <= 0.0 || minutes <= 0) return 0.0
        return Math.round(met * bodyWeightKg * (minutes / 60.0) * 10.0) / 10.0
    }

    /** 「跑步 30 分钟 ≈ 280 kcal」 */
    fun summaryLine(sport: SportInfo, bodyWeightKg: Double): String {
        val kcal = estimateKcal(sport.met, bodyWeightKg, sport.defaultMinutes)
        return "${sport.defaultMinutes} 分钟 ≈ ${Math.round(kcal)} kcal"
    }
}
