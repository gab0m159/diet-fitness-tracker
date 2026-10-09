package com.example.diettracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 字号体系。
 *
 * ## 为什么比默认值大
 *
 * 1. 应用里全是密集的数字（克数、热量、组数），中文在小字号下可读性差；
 * 2. 用户反馈在**长屏手机**（小米 18 Pro 这类 20:9 甚至更长的屏）上「各模块比例
 *    怪怪的」。根因是：屏幕变长但**字号没变**，于是每一块看着都偏小偏扁，纵向
 *    留白却很多。
 *
 * 对策是把正文与标签整体上调 1-2sp、行高同步放开，让内容「占得住」长屏。
 */
val DietTrackerTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 31.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 29.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.5.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
)

/**
 * 界面统一使用的间距刻度。
 *
 * 之前各页面自己写 12.dp / 14.dp / 16.dp，导致长屏上「块与块之间的呼吸感」不
 * 一致，看起来就是比例怪。这里统一成一套，页面只从这几个值里取。
 */
object Spacing {
    /** 卡片内边距。 */
    val cardPadding = 16.dp

    /** 页面左右边距。 */
    val screenHorizontal = 16.dp

    /** 卡片之间的竖直间距。 */
    val sectionGap = 14.dp

    /** 卡片内部元素之间的间距。 */
    val itemGap = 10.dp

    /** 列表底部留白，避免被底部栏压住。 */
    val listBottom = 28.dp
}

private val Int.dp get() = androidx.compose.ui.unit.Dp(this.toFloat())
