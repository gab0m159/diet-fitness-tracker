package com.example.diettracker.domain

import com.example.diettracker.data.model.BodyPart
import com.example.diettracker.data.model.SplitType

/**
 * A training day produced from a split template: a display name plus the
 * exercises to perform.
 */
data class SplitDayTemplate(
    val name: String,
    val bodyPart: BodyPart,
    val exerciseNames: List<String>
) {
    /** "胸+三头 · 4 个动作" */
    val summary: String get() = "$name · ${exerciseNames.size} 个动作"
}

/**
 * The default training-day arrangement for each split type.
 *
 * These are starting points: once generated they are written to the database and
 * the user is free to rename days, change the exercise lists and change each
 * day's frequency.
 */
object SplitTemplates {

    fun forSplit(type: SplitType): List<SplitDayTemplate> = when (type) {
        SplitType.THREE -> threeDay
        SplitType.FIVE -> fiveDay
        SplitType.SIX -> sixDay
    }

    /** 三分化：胸+三头 / 背+二头 / 腿+肩 */
    private val threeDay = listOf(
        SplitDayTemplate(
            name = "胸 + 三头",
            bodyPart = BodyPart.CHEST,
            exerciseNames = listOf(
                "杠铃卧推",
                "上斜卧推",
                "绳索夹胸",
                "绳索下压",
                "过顶臂屈伸"
            )
        ),
        SplitDayTemplate(
            name = "背 + 二头",
            bodyPart = BodyPart.BACK,
            exerciseNames = listOf(
                "高位下拉",
                "杠铃划船",
                "坐姿划船",
                "杠铃弯举",
                "锤式弯举"
            )
        ),
        SplitDayTemplate(
            name = "腿 + 肩",
            bodyPart = BodyPart.LEGS,
            exerciseNames = listOf(
                "深蹲",
                "罗马尼亚硬拉",
                "腿举",
                "站姿推举",
                "侧平举"
            )
        )
    )

    /** 五分化：胸 / 背 / 腿 / 肩 / 手臂 */
    private val fiveDay = listOf(
        SplitDayTemplate(
            name = "胸",
            bodyPart = BodyPart.CHEST,
            exerciseNames = listOf(
                "杠铃卧推",
                "上斜卧推",
                "哑铃卧推",
                "绳索夹胸",
                "双杠臂屈伸"
            )
        ),
        SplitDayTemplate(
            name = "背",
            bodyPart = BodyPart.BACK,
            exerciseNames = listOf(
                "引体向上",
                "高位下拉",
                "杠铃划船",
                "坐姿划船",
                "单臂哑铃划船"
            )
        ),
        SplitDayTemplate(
            name = "腿",
            bodyPart = BodyPart.LEGS,
            exerciseNames = listOf(
                "深蹲",
                "腿举",
                "罗马尼亚硬拉",
                "腿弯举",
                "提踵"
            )
        ),
        SplitDayTemplate(
            name = "肩",
            bodyPart = BodyPart.SHOULDERS,
            exerciseNames = listOf(
                "站姿推举",
                "哑铃肩推",
                "侧平举",
                "面拉"
            )
        ),
        SplitDayTemplate(
            name = "手臂",
            bodyPart = BodyPart.ARMS,
            exerciseNames = listOf(
                "杠铃弯举",
                "锤式弯举",
                "窄距卧推",
                "绳索下压",
                "过顶臂屈伸"
            )
        )
    )

    /** 六分化：在五分化基础上把腿拆成「股四头肌为主」和「腘绳肌/臀为主」 */
    private val sixDay = listOf(
        fiveDay[0].copy(
            name = "胸",
            exerciseNames = listOf(
                "杠铃卧推",
                "上斜卧推",
                "哑铃卧推",
                "绳索夹胸",
                "双杠臂屈伸"
            )
        ),
        fiveDay[1].copy(
            name = "背",
            exerciseNames = listOf(
                "引体向上",
                "高位下拉",
                "杠铃划船",
                "坐姿划船",
                "单臂哑铃划船"
            )
        ),
        SplitDayTemplate(
            name = "腿（股四头肌）",
            bodyPart = BodyPart.LEGS,
            exerciseNames = listOf("深蹲", "前蹲", "腿举", "腿屈伸", "提踵")
        ),
        SplitDayTemplate(
            name = "腿（腘绳肌 + 臀）",
            bodyPart = BodyPart.GLUTES,
            exerciseNames = listOf("罗马尼亚硬拉", "硬拉", "腿弯举", "臀推")
        ),
        fiveDay[3].copy(
            name = "肩",
            exerciseNames = listOf("站姿推举", "哑铃肩推", "侧平举", "面拉")
        ),
        fiveDay[4].copy(
            name = "手臂 + 核心",
            exerciseNames = listOf(
                "杠铃弯举",
                "锤式弯举",
                "绳索下压",
                "过顶臂屈伸",
                "平板支撑"
            )
        )
    )

    /**
     * 「几天一次」的默认频率，按每个训练日原来的周频率换算：
     *
     *  - 三分化 = 练 3 休 1 → 每 4 天轮到一次；
     *  - 五分化 = 练 5 休 1 → 每 6 天；
     *  - 六分化 = 练 6 休 1 → 每 7 天。
     *
     * 用户建完之后可以逐个训练日单独改。
     */
    fun defaultIntervalDays(type: SplitType): Int = when (type) {
        SplitType.THREE -> 4
        SplitType.FIVE -> 6
        SplitType.SIX -> 7
    }
}
