package com.example.diettracker.domain

import com.example.diettracker.data.model.BodyPart
import com.example.diettracker.data.model.BodyRegion

/**
 * A stretch, with a cited source.
 *
 * ## Methodology
 *
 * The prescription defaults follow the ACSM flexibility guidelines — static
 * stretches held **15-60 seconds**, repeated **2-4 times**, accumulating roughly
 * **60 seconds total per muscle group** — and the general advice to stretch after
 * activity, breathe normally and avoid bouncing (Harvard Health).
 *
 * Every entry carries [source] so the user can see where the movement comes from
 * rather than trusting an unattributed blob.
 *
 * @param mediaUrl reserved slot for a photo or video link. Null means "not
 *        provided yet"; the UI shows a placeholder.
 */
data class StretchGuide(
    val name: String,
    val targetMuscle: String,
    val holdSecondsMin: Int = 20,
    val holdSecondsMax: Int = 30,
    val sets: Int = 3,
    /** Step-by-step execution cue. */
    val howTo: String,
    /** Longer explanation: why it helps, common mistakes, safety notes. */
    val description: String = "",
    val source: String = "",
    val mediaUrl: String? = null
) {
    /** "保持 20-30 秒，重复 3 组（约 60 秒）". */
    val prescription: String
        get() = "保持 $holdSecondsMin-$holdSecondsMax 秒，重复 $sets 组" +
            "（每侧约 ${holdSecondsMin * sets}-${holdSecondsMax * sets} 秒）"

    companion object {
        /** Shown on every stretch screen so the numbers are traceable. */
        const val METHODOLOGY_NOTE =
            "方法学依据：ACSM 柔韧性训练指南建议静态拉伸每个动作保持 15-60 秒、" +
                "重复 2-4 组、每个肌群累计约 60 秒；建议在训练后或热身后进行，" +
                "呼吸自然、不要弹震（Harvard Health）。"

        const val ACSM_SOURCE = "ACSM 柔韧性训练指南（静态拉伸 15-60 秒 / 2-4 组）"
        const val HARVARD_SOURCE = "Harvard Health《The ideal stretching routine》"
    }
}

/**
 * One movement in the library: which muscle it works, whether it counts as
 * upper or lower body (which sets the default load increment), and what to
 * stretch afterwards.
 *
 * @param source where the movement description comes from, shown in the UI so
 *        nothing is presented as unattributed fact.
 */
data class ExerciseInfo(
    val name: String,
    val bodyPart: BodyPart,
    val region: BodyRegion,
    val primaryMuscle: String,
    val stretches: List<StretchGuide>,
    /** Optional coaching cue shown on the workout log screen. */
    val cue: String = "",
    val source: String = ""
) {
    /** Default load increment for this movement, in kg. */
    val incrementKg: Double get() = region.incrementKg
}

/**
 * Built-in exercise library.
 *
 * Exercises listed here get precise muscle attribution, a default load increment
 * (upper vs lower body) and a curated set of stretches. Anything the user adds
 * themselves lives in the `custom_exercises` table and starts at the default
 * 2.5 kg increment, without an upper/lower-body classification.
 */
object ExerciseLibrary {

    // ============================================================ stretches
    //
    // Sources:
    //  [ACSM]   ACSM flexibility guidelines (static 15-60 s, 2-4 reps, ~60 s per muscle)
    //  [HARV]   Harvard Health, "The ideal stretching routine"
    //  [UCD]    UC Davis Sports Medicine stretching handout (muscle-specific positions)
    //  [NHS]    NHS "How to stretch properly" guidance (no bouncing, breathe)

    private const val SRC_ACSM = "ACSM 柔韧性训练指南"
    private const val SRC_HARVARD = "Harvard Health 拉伸指南"
    private const val SRC_UCDAVIS = "UC Davis Sports Medicine 拉伸手册"
    private const val SRC_NHS = "NHS 拉伸建议"
    private const val SRC_GENERAL = "通用运动解剖学（动作描述）"

    // ---- Chest ------------------------------------------------------------

    val doorChestStretch = StretchGuide(
        name = "门框胸部拉伸",
        targetMuscle = "胸大肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 3,
        howTo = "站在门框前，前臂贴住门框、肘与肩同高，同侧脚向前迈半步，" +
            "身体缓慢前倾直到胸前有牵拉感。",
        description = "主要拉伸胸大肌的胸肋部与锁骨部。改善圆肩与胸小肌紧张，" +
            "对卧推后放松和久坐含胸体态都有帮助。注意身体前倾幅度由小到大，不要一上来就压到底；" +
            "肩关节有伤病时肘部可略低于肩，避免肩前侧过度受力。",
        source = "$SRC_UCDAVIS / $SRC_ACSM"
    )

    private val supineChestStretch = StretchGuide(
        name = "仰卧哑铃扩胸",
        targetMuscle = "胸大肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "仰卧于地面或垫上，双手持轻哑铃（或空手）向两侧打开至上臂与躯干约 90°，" +
            "肘微屈，感受胸前拉伸后保持。",
        description = "比门框拉伸更温和，适合卧推后立刻放松。地面支撑让下背不必过伸，" +
            "对腰不好的人比躺在长凳上更安全。全程保持肘关节微屈，不要完全锁死。",
        source = "$SRC_GENERAL / $SRC_HARVARD"
    )

    // ---- Shoulders --------------------------------------------------------

    val frontDeltStretch = StretchGuide(
        name = "站姿三角肌前束拉伸",
        targetMuscle = "三角肌前束",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 3,
        howTo = "站立，双手在身后十指交叉、手臂伸直，缓慢向后上方抬起，" +
            "同时挺胸，感受肩前侧拉伸。",
        description = "针对推举、卧推后紧张的前束。日常伏案工作让肩前侧长期缩短，" +
            "这条拉伸也有助于改善体态。抬臂高度以肩前有牵拉感为度，" +
            "若出现肩关节夹挤感请立即降低高度。",
        source = "$SRC_UCDAVIS"
    )

    private val crossBodyShoulderStretch = StretchGuide(
        name = "交叉臂肩后束拉伸",
        targetMuscle = "三角肌中束 / 后束",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 3,
        howTo = "将一侧手臂横过胸前，另一手托住其肘部上方轻轻拉向对侧肩，" +
            "肩部保持下沉不要耸起。",
        description = "拉伸三角肌后束与肩袖肌群。适合划船、面拉等大量后束训练之后。" +
            "关键是肩膀要主动下沉，很多人耸肩拉导致拉伸落到了斜方肌上。",
        source = "$SRC_UCDAVIS"
    )

    // ---- Back / lats ------------------------------------------------------

    val latStretch = StretchGuide(
        name = "跪姿背阔肌拉伸",
        targetMuscle = "背阔肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 3,
        howTo = "跪坐于垫上，双手前伸贴地，臀部向脚跟方向坐，手臂伸直下压，" +
            "感受腋下到体侧的拉伸。",
        description = "背阔肌起自下背与髂嵴，止于肱骨，是引体向上和高位下拉的主要发力肌。" +
            "这条拉伸能同时牵拉腋下与腰侧。想加强一侧可双手向斜前方移动。",
        source = "$SRC_UCDAVIS / $SRC_ACSM"
    )

    private val hangingLatStretch = StretchGuide(
        name = "单臂扶杆背阔肌拉伸",
        targetMuscle = "背阔肌",
        holdSecondsMin = 15,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "单手扶住固定横杆或门框，屈髋屈膝、臀部后坐，" +
            "让身体重量把肩胛向远处拉，感受体侧拉伸。",
        description = "借助体重做牵引式拉伸，力度比自己用力压更均匀。" +
            "因为需要悬挂发力，建议在握力尚可时做，握力疲劳后容易脱手。",
        source = "$SRC_GENERAL"
    )

    private val thoracicExtensionStretch = StretchGuide(
        name = "泡沫轴胸椎伸展",
        targetMuscle = "胸椎 / 中背",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "仰卧，把泡沫轴横放在肩胛骨下方，双手抱头，缓慢向后伸展上背，" +
            "在无痛范围内停留。",
        description = "针对胸椎活动度而非肌肉长度。胸椎僵硬会让肩推和深蹲时被迫用腰椎代偿，" +
            "改善这里往往能直接提高过顶动作的舒适度。腰部有伤者请从更小的幅度开始。",
        source = "$SRC_GENERAL"
    )

    // ---- Arms -------------------------------------------------------------

    val bicepsStretch = StretchGuide(
        name = "靠墙二头肌拉伸",
        targetMuscle = "肱二头肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "手臂后伸、掌心贴墙，身体缓慢向对侧转动，感受上臂前侧拉伸。",
        description = "肱二头肌跨过肩关节，所以肩伸展角度决定拉伸强度。" +
            "弯举、划船、引体向上之后做这条能缓解肘窝紧张。手掌贴墙比手指贴墙更安全。",
        source = "$SRC_UCDAVIS"
    )

    val tricepsStretch = StretchGuide(
        name = "过头三头肌拉伸",
        targetMuscle = "肱三头肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "一手举过头顶屈肘，手掌摸对侧肩胛，另一手轻压肘部，感受上臂后侧拉伸。",
        description = "同时牵拉肱三头肌长头与背阔肌上部。窄距卧推、绳索下压之后使用。" +
            "压肘的力度要轻，感觉肘关节内侧疼痛就说明压过头了。",
        source = "$SRC_UCDAVIS"
    )

    private val wristFlexorStretch = StretchGuide(
        name = "腕屈肌拉伸",
        targetMuscle = "前臂屈肌群",
        holdSecondsMin = 15,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "手臂前伸、掌心朝上、手指朝下，另一手轻拉手指向身体方向，感受前臂内侧拉伸。",
        description = "前臂屈肌在大量握持动作（硬拉、引体、弯举）后容易紧张，" +
            "长期紧张与高尔夫球肘相关。拉伸时肘关节保持伸直效果才到位。",
        source = "$SRC_UCDAVIS"
    )

    private val wristExtensorStretch = StretchGuide(
        name = "腕伸肌拉伸",
        targetMuscle = "前臂伸肌群",
        holdSecondsMin = 15,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "手臂前伸、掌心朝下、手指朝下，另一手轻拉手背向身体方向，感受前臂外侧拉伸。",
        description = "针对肱桡肌与腕伸肌群，与网球肘关系密切。" +
            "做腕弯举、反向弯举或长时间用鼠标后特别值得做。",
        source = "$SRC_UCDAVIS / $SRC_NHS"
    )

    // ---- Neck / traps -----------------------------------------------------

    private val neckSideStretch = StretchGuide(
        name = "侧颈拉伸",
        targetMuscle = "斜方肌上部 / 颈侧",
        holdSecondsMin = 15,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "坐或站直，一手轻放对侧头部，缓慢向同侧倾斜让耳朵靠近肩膀，" +
            "对侧肩膀主动下沉。",
        description = "缓解斜方肌上部紧张。耸肩、久坐、大重量硬拉后常见。" +
            "动作务必轻柔，颈部血管神经密集，绝不要用手猛拉或做绕环。",
        source = "$SRC_NHS"
    )

    private val upperTrapStretch = StretchGuide(
        name = "斜方肌上束拉伸",
        targetMuscle = "斜方肌上部",
        holdSecondsMin = 15,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "坐姿，一手抓住椅子边缘固定肩膀下沉，头向对侧倾斜并轻微转向对侧，" +
            "感受颈肩交界拉伸。",
        description = "先固定肩胛再倾斜头部，才能把拉伸集中在斜方肌上束而不是颈部深层。" +
            "适合耸肩类动作（硬拉、农夫行走）之后。",
        source = "$SRC_GENERAL"
    )

    private val rhomboidStretch = StretchGuide(
        name = "抱肩中背拉伸",
        targetMuscle = "菱形肌 / 中背",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "坐或站姿，双臂交叉抱住对侧肩膀，含胸拱背，" +
            "把肩胛骨向前推开，感受肩胛之间拉伸。",
        description = "针对菱形肌与中斜方肌。划船、面拉之后使用。" +
            "主动含胸是关键，只抱肩不含胸基本没有拉伸效果。",
        source = "$SRC_UCDAVIS"
    )

    // ---- Low back / core --------------------------------------------------

    val lowerBackStretch = StretchGuide(
        name = "猫式下背拉伸",
        targetMuscle = "下背 / 竖脊肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "四点跪姿，吸气塌腰抬头，呼气拱背低头，动作缓慢，" +
            "在拱背位停留感受下背伸展。",
        description = "温和地活动脊柱而非强拉，适合硬拉、深蹲后放松竖脊肌。" +
            "优先追求节段控制，而不是追求幅度。急性腰痛期不要做。",
        source = "$SRC_GENERAL / $SRC_HARVARD"
    )

    private val kneesToChestStretch = StretchGuide(
        name = "仰卧抱膝",
        targetMuscle = "下背 / 臀大肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "仰卧，双手抱住双膝轻轻拉向胸口，下背贴向地面，保持呼吸。",
        description = "让腰椎做轻度屈曲、放松竖脊肌与腰方肌。" +
            "是训练后或久坐后最安全的放松动作之一。抱单膝可加强一侧。",
        source = "$SRC_UCDAVIS"
    )

    private val childPoseStretch = StretchGuide(
        name = "婴儿式背部放松",
        targetMuscle = "下背 / 背阔肌",
        holdSecondsMin = 30,
        holdSecondsMax = 60,
        sets = 2,
        howTo = "跪坐，臀部坐向脚跟，上身前俯、额头贴地，双手向前伸展，" +
            "深呼吸并在呼气时让身体更下沉。",
        description = "同时放松下背、背阔肌与臀部，是很适合作为整套拉伸收尾的静态动作。" +
            "因为保持时间较长，注意保持均匀呼吸不要憋气。",
        source = "$SRC_HARVARD"
    )

    // ---- Hip flexors ------------------------------------------------------

    private val hipFlexorStretch = StretchGuide(
        name = "弓步髋屈肌拉伸",
        targetMuscle = "髂腰肌 / 股直肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 3,
        howTo = "单膝跪地成弓步，前腿屈膝约 90°，收紧臀部并向前送髋，" +
            "感受后侧大腿根到髋前拉伸。",
        description = "髂腰肌连接腰椎与股骨，久坐会缩短并造成骨盆前倾，是深蹲与硬拉的常见限制因素。" +
            "关键是先收紧臀部把骨盆后倾，否则会变成塌腰代偿，拉伸效果大打折扣还可能压到腰椎。",
        source = "$SRC_UCDAVIS / $SRC_ACSM"
    )

    private val adductorStretch = StretchGuide(
        name = "蝴蝶式内收肌拉伸",
        targetMuscle = "大腿内收肌群",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "坐姿，双脚脚掌相对、双膝向外打开，双手握住脚踝，" +
            "保持背部挺直从髋部前倾。",
        description = "内收肌在深蹲、相扑硬拉中参与显著，却最常被忽略。" +
            "务必保持背部挺直前倾，弓背会让拉伸跑到腰椎上。幅度以髋内侧有牵拉感为准。",
        source = "$SRC_UCDAVIS"
    )

    private val pigeonStretch = StretchGuide(
        name = "鸽子式臀部拉伸",
        targetMuscle = "臀大肌 / 梨状肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "四点跪姿或坐姿，一侧小腿横放身前，后腿向后伸直，" +
            "骨盆保持中立并缓慢前倾。",
        description = "针对深层臀部与梨状肌，对久坐产生的臀部紧绷和坐骨神经区域不适有帮助。" +
            "膝关节有伤者请把小腿收得更靠近身体以减小膝盖扭转角度。",
        source = "$SRC_GENERAL"
    )

    // ---- Glutes / hamstrings ---------------------------------------------

    val gluteStretch = StretchGuide(
        name = "仰卧四字臀部拉伸",
        targetMuscle = "臀大肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "仰卧，把一侧脚踝搭在对侧膝上，双手抱住对侧大腿后侧拉向胸口，" +
            "臀部有牵拉感。",
        description = "比鸽子式更安全易做的臀部拉伸，仰卧姿势对腰椎几乎没有压力。" +
            "深蹲、腿举、臀推之后使用。拉向胸口时保持下背贴地效果更好。",
        source = "$SRC_UCDAVIS"
    )

    val hamstringStretch = StretchGuide(
        name = "坐姿腘绳肌拉伸",
        targetMuscle = "腘绳肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 3,
        howTo = "坐姿一腿伸直，另一腿屈曲，保持背部挺直从髋部前倾，" +
            "大腿后侧有拉伸感。",
        description = "腘绳肌是硬拉、罗马尼亚硬拉的主要目标肌。" +
            "重点在于「从髋部折叠」而不是弓背用手去够脚尖——弓背时拉伸会转移到腰椎，" +
            "既拉不到腘绳肌又有腰椎风险。",
        source = "$SRC_UCDAVIS / $SRC_ACSM"
    )

    private val standingHamstringStretch = StretchGuide(
        name = "站姿抬腿腘绳肌拉伸",
        targetMuscle = "腘绳肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "站姿，把一侧脚跟放在齐髋高的支撑面上、腿伸直，" +
            "保持背部挺直从髋部前倾。",
        description = "站姿版本更贴近日常动作模式，可作为训练前的动态准备或训练后的静态拉伸。" +
            "支撑面高度不宜过高，否则会靠弓背代偿。",
        source = "$SRC_GENERAL"
    )

    // ---- Quads ------------------------------------------------------------

    val quadStretch = StretchGuide(
        name = "站姿股四头肌拉伸",
        targetMuscle = "股四头肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "单手扶墙，另一手抓同侧脚踝拉向臀部，膝盖并拢、骨盆中立，" +
            "大腿前侧有拉伸感。",
        description = "股直肌跨过髋关节，所以要把骨盆保持中立（收紧臀部、不塌腰）才能拉到。" +
            "深蹲、腿举、腿屈伸之后使用。若膝盖有不适，可改用侧卧版本。",
        source = "$SRC_UCDAVIS"
    )

    private val sideLyingQuadStretch = StretchGuide(
        name = "侧卧股四头肌拉伸",
        targetMuscle = "股四头肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "侧卧，下侧手支撑头部，上侧手抓同侧脚踝拉向臀部，膝盖贴近地面。",
        description = "相比站姿版本，侧卧能更好地固定骨盆、避免塌腰代偿，" +
            "膝关节压力也更小。适合站姿版本做不好或平衡感欠佳的人。",
        source = "$SRC_GENERAL"
    )

    // ---- Calves / shins ---------------------------------------------------

    private val calfStretch = StretchGuide(
        name = "靠墙小腿拉伸",
        targetMuscle = "腓肠肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 3,
        howTo = "面墙站立，一腿后撤伸直、脚跟踩实，前腿屈膝，重心前移，小腿后侧有拉伸感。",
        description = "拉伸跨膝关节的腓肠肌，是深蹲、提踵、跑步后最常用的放松动作。" +
            "后脚跟必须踩实，脚跟一离地拉伸就失效了。",
        source = "$SRC_UCDAVIS / $SRC_ACSM"
    )

    private val soleusStretch = StretchGuide(
        name = "屈膝比目鱼肌拉伸",
        targetMuscle = "比目鱼肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "与靠墙小腿拉伸相同的站位，但后腿膝盖弯曲、脚跟保持踩实，" +
            "感受更靠近跟腱上方的拉伸。",
        description = "比目鱼肌不跨膝关节，所以必须屈膝才能拉到。" +
            "踝关节背屈受限常与它有关，深蹲蹲不下去的人值得重点做。",
        source = "$SRC_GENERAL"
    )

    private val tibialisStretch = StretchGuide(
        name = "跪坐胫骨前肌拉伸",
        targetMuscle = "胫骨前肌",
        holdSecondsMin = 20,
        holdSecondsMax = 30,
        sets = 2,
        howTo = "跪坐，脚背贴地、臀部坐在脚跟上，感受小腿前侧拉伸；" +
            "感觉过强可在膝下垫毛巾。",
        description = "胫骨前肌负责勾脚，跑步、爬坡或大量提踵训练后容易紧张。" +
            "这是少数能有效拉伸到小腿前侧的体位，但跪姿对膝关节压力较大，有伤请跳过。",
        source = "$SRC_GENERAL"
    )

    // ============================================== stretch groups / lookup

    /**
     * Every built-in stretch, grouped for the stand-alone stretch library.
     * Ordered roughly from upper body to lower body.
     */
    fun allStretchGroups(): List<Pair<String, List<StretchGuide>>> = listOf(
        "胸部" to listOf(doorChestStretch, supineChestStretch),
        "肩部" to listOf(
            frontDeltStretch,
            crossBodyShoulderStretch,
            neckSideStretch,
            upperTrapStretch
        ),
        "背部" to listOf(
            latStretch,
            hangingLatStretch,
            thoracicExtensionStretch,
            rhomboidStretch
        ),
        "手臂与前臂" to listOf(
            bicepsStretch,
            tricepsStretch,
            wristFlexorStretch,
            wristExtensorStretch
        ),
        "下背与核心" to listOf(lowerBackStretch, kneesToChestStretch, childPoseStretch),
        "髋部与臀部" to listOf(hipFlexorStretch, adductorStretch, pigeonStretch, gluteStretch),
        "大腿" to listOf(
            hamstringStretch,
            standingHamstringStretch,
            quadStretch,
            sideLyingQuadStretch
        ),
        "小腿" to listOf(calfStretch, soleusStretch, tibialisStretch)
    )

    /** Flat list of every built-in stretch (de-duplicated by name). */
    fun allStretches(): List<StretchGuide> =
        allStretchGroups().flatMap { it.second }.distinctBy { it.name }

    /** Look up one built-in stretch by name. */
    fun findStretch(name: String): StretchGuide? =
        allStretches().firstOrNull { it.name == name }

    // ----------------------------------------------------------- exercises

    /**
     * Every known exercise, keyed by name.
     *
     * Adding an entry here automatically improves both the stretch guidance and
     * the load-increment classification for that movement.
     */
    private val library: Map<String, ExerciseInfo> = listOf(
        // ---- Chest / triceps ------------------------------------------------
        ExerciseInfo(
            name = "杠铃卧推",
            bodyPart = BodyPart.CHEST,
            region = BodyRegion.UPPER,
            primaryMuscle = "胸大肌",
            stretches = listOf(doorChestStretch, frontDeltStretch),
            cue = "肩胛后缩下沉，杠铃下放到胸中部",
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "哑铃卧推",
            bodyPart = BodyPart.CHEST,
            region = BodyRegion.UPPER,
            primaryMuscle = "胸大肌",
            stretches = listOf(doorChestStretch, frontDeltStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "上斜卧推",
            bodyPart = BodyPart.CHEST,
            region = BodyRegion.UPPER,
            primaryMuscle = "胸大肌上部",
            stretches = listOf(doorChestStretch, frontDeltStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "双杠臂屈伸",
            bodyPart = BodyPart.CHEST,
            region = BodyRegion.UPPER,
            primaryMuscle = "胸大肌下部 / 肱三头肌",
            stretches = listOf(doorChestStretch, tricepsStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "绳索夹胸",
            bodyPart = BodyPart.CHEST,
            region = BodyRegion.UPPER,
            primaryMuscle = "胸大肌",
            stretches = listOf(doorChestStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "窄距卧推",
            bodyPart = BodyPart.ARMS,
            region = BodyRegion.UPPER,
            primaryMuscle = "肱三头肌",
            stretches = listOf(tricepsStretch, chestStretchOf()),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "绳索下压",
            bodyPart = BodyPart.ARMS,
            region = BodyRegion.UPPER,
            primaryMuscle = "肱三头肌",
            stretches = listOf(tricepsStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "过顶臂屈伸",
            bodyPart = BodyPart.ARMS,
            region = BodyRegion.UPPER,
            primaryMuscle = "肱三头肌",
            stretches = listOf(tricepsStretch),
            source = "通用力量训练动作库"
        ),

        // ---- Shoulders ------------------------------------------------------
        ExerciseInfo(
            name = "站姿推举",
            bodyPart = BodyPart.SHOULDERS,
            region = BodyRegion.UPPER,
            primaryMuscle = "三角肌前束",
            stretches = listOf(frontDeltStretch, tricepsStretch),
            cue = "核心收紧，避免过度挺腰",
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "哑铃肩推",
            bodyPart = BodyPart.SHOULDERS,
            region = BodyRegion.UPPER,
            primaryMuscle = "三角肌前束",
            stretches = listOf(frontDeltStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "侧平举",
            bodyPart = BodyPart.SHOULDERS,
            region = BodyRegion.UPPER,
            primaryMuscle = "三角肌中束",
            stretches = listOf(frontDeltStretch, crossBodyShoulderStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "面拉",
            bodyPart = BodyPart.SHOULDERS,
            region = BodyRegion.UPPER,
            primaryMuscle = "三角肌后束",
            stretches = listOf(crossBodyShoulderStretch, latStretch),
            source = "通用力量训练动作库"
        ),

        // ---- Back / biceps --------------------------------------------------
        ExerciseInfo(
            name = "引体向上",
            bodyPart = BodyPart.BACK,
            region = BodyRegion.UPPER,
            primaryMuscle = "背阔肌",
            stretches = listOf(latStretch, bicepsStretch, wristFlexorStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "高位下拉",
            bodyPart = BodyPart.BACK,
            region = BodyRegion.UPPER,
            primaryMuscle = "背阔肌",
            stretches = listOf(latStretch, bicepsStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "杠铃划船",
            bodyPart = BodyPart.BACK,
            region = BodyRegion.UPPER,
            primaryMuscle = "背阔肌 / 中背",
            stretches = listOf(latStretch, lowerBackStretch, rhomboidStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "坐姿划船",
            bodyPart = BodyPart.BACK,
            region = BodyRegion.UPPER,
            primaryMuscle = "中背 / 菱形肌",
            stretches = listOf(rhomboidStretch, latStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "单臂哑铃划船",
            bodyPart = BodyPart.BACK,
            region = BodyRegion.UPPER,
            primaryMuscle = "背阔肌",
            stretches = listOf(latStretch, rhomboidStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "杠铃弯举",
            bodyPart = BodyPart.ARMS,
            region = BodyRegion.UPPER,
            primaryMuscle = "肱二头肌",
            stretches = listOf(bicepsStretch, wristFlexorStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "哑铃弯举",
            bodyPart = BodyPart.ARMS,
            region = BodyRegion.UPPER,
            primaryMuscle = "肱二头肌",
            stretches = listOf(bicepsStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "锤式弯举",
            bodyPart = BodyPart.ARMS,
            region = BodyRegion.UPPER,
            primaryMuscle = "肱肌 / 肱桡肌",
            stretches = listOf(bicepsStretch, wristExtensorStretch),
            source = "通用力量训练动作库"
        ),

        // ---- Legs / glutes --------------------------------------------------
        ExerciseInfo(
            name = "深蹲",
            bodyPart = BodyPart.LEGS,
            region = BodyRegion.LOWER,
            primaryMuscle = "股四头肌 / 臀大肌",
            stretches = listOf(quadStretch, gluteStretch, hipFlexorStretch),
            cue = "膝盖与脚尖同向，下蹲至大腿平行或更低",
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "前蹲",
            bodyPart = BodyPart.LEGS,
            region = BodyRegion.LOWER,
            primaryMuscle = "股四头肌",
            stretches = listOf(quadStretch, gluteStretch, wristFlexorStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "腿举",
            bodyPart = BodyPart.LEGS,
            region = BodyRegion.LOWER,
            primaryMuscle = "股四头肌 / 臀大肌",
            stretches = listOf(quadStretch, gluteStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "腿屈伸",
            bodyPart = BodyPart.LEGS,
            region = BodyRegion.LOWER,
            primaryMuscle = "股四头肌",
            stretches = listOf(quadStretch, sideLyingQuadStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "箭步蹲",
            bodyPart = BodyPart.LEGS,
            region = BodyRegion.LOWER,
            primaryMuscle = "股四头肌 / 臀大肌",
            stretches = listOf(quadStretch, gluteStretch, hipFlexorStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "罗马尼亚硬拉",
            bodyPart = BodyPart.LEGS,
            region = BodyRegion.LOWER,
            primaryMuscle = "腘绳肌 / 臀大肌",
            stretches = listOf(hamstringStretch, lowerBackStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "硬拉",
            bodyPart = BodyPart.BACK,
            region = BodyRegion.LOWER,
            primaryMuscle = "腘绳肌 / 下背 / 臀大肌",
            stretches = listOf(hamstringStretch, lowerBackStretch, latStretch),
            cue = "背部中立，用腿和髋发力而不是用手拉",
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "腿弯举",
            bodyPart = BodyPart.LEGS,
            region = BodyRegion.LOWER,
            primaryMuscle = "腘绳肌",
            stretches = listOf(hamstringStretch, calfStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "臀推",
            bodyPart = BodyPart.GLUTES,
            region = BodyRegion.LOWER,
            primaryMuscle = "臀大肌",
            stretches = listOf(gluteStretch, hipFlexorStretch, quadStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "提踵",
            bodyPart = BodyPart.LEGS,
            region = BodyRegion.LOWER,
            primaryMuscle = "腓肠肌",
            stretches = listOf(calfStretch, soleusStretch),
            source = "通用力量训练动作库"
        ),

        // ---- Core -----------------------------------------------------------
        ExerciseInfo(
            name = "平板支撑",
            bodyPart = BodyPart.CORE,
            region = BodyRegion.UPPER,
            primaryMuscle = "腹横肌",
            stretches = listOf(lowerBackStretch, childPoseStretch),
            source = "通用力量训练动作库"
        ),
        ExerciseInfo(
            name = "卷腹",
            bodyPart = BodyPart.CORE,
            region = BodyRegion.UPPER,
            primaryMuscle = "腹直肌",
            stretches = listOf(lowerBackStretch, kneesToChestStretch),
            source = "通用力量训练动作库"
        )
    ).associateBy { it.name }

    /** Kept for readability at the call sites above. */
    private fun chestStretchOf(): StretchGuide = doorChestStretch

    /** Lookup by exact name. */
    fun find(name: String): ExerciseInfo? = library[name.trim()]

    /**
     * Load-increment classification. Only built-in exercises have one; custom
     * exercises deliberately do not, and fall back to [BodyRegion.UPPER].
     */
    fun regionOf(name: String): BodyRegion =
        find(name)?.region ?: BodyRegion.UPPER

    /**
     * Default increment for an exercise: lower-body lifts move in bigger steps
     * than upper-body ones. Custom exercises start at the shared default.
     */
    fun defaultIncrementFor(name: String): Double {
        val info = find(name) ?: return BodyRegion.DEFAULT_INCREMENT_KG
        return info.region.incrementKg
    }

    /**
     * Stretch guidance for a session: the union of the stretches for every
     * exercise performed, de-duplicated by name and capped so the card stays
     * readable.
     */
    fun stretchesFor(exerciseNames: List<String>, limit: Int = 6): List<StretchGuide> {
        val seen = LinkedHashMap<String, StretchGuide>()
        exerciseNames.forEach { name ->
            find(name)?.stretches?.forEach { guide ->
                seen.putIfAbsent(guide.name, guide)
            }
        }
        return seen.values.take(limit)
    }

    /** Primary muscles worked, for the session summary. */
    fun musclesFor(exerciseNames: List<String>): List<String> =
        exerciseNames.mapNotNull { find(it)?.primaryMuscle }.distinct()

    /** All exercises available in the picker, sorted for display. */
    fun allNames(): List<String> = library.keys.sorted()

    /** Exercises filtered to one body part, for the plan editor. */
    fun namesForBodyPart(part: BodyPart): List<String> =
        library.values.filter { it.bodyPart == part }.map { it.name }.sorted()

    /** Convenience for rest guidance text. */
    fun isKnown(name: String): Boolean = library.containsKey(name.trim())

    /** Built-in exercises grouped by body part, for the library screen. */
    fun groupedByBodyPart(): List<Pair<BodyPart, List<ExerciseInfo>>> =
        BodyPart.entries
            .map { part -> part to library.values.filter { it.bodyPart == part } }
            .filter { it.second.isNotEmpty() }
            .map { (part, list) -> part to list.sortedBy { it.name } }
}
