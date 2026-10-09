package com.example.diettracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * 应用的配色方案。
 *
 * ## 设计取向
 *
 * 主体仍是「饮食 + 运动」的清爽绿，但**每个功能域有自己的强调色**，让密集的
 * 数据一眼能区分开：
 *
 *  - 主色 绿：饮食 / 达标
 *  - 运动 蓝紫：运动项目与消耗
 *  - 撸铁 橙：力量训练
 *  - 拉伸 紫：拉伸与恢复
 *
 * 刻意**不使用动态取色**（Material You），这样在任何设备、任何系统版本上颜色
 * 都一致，截图和设计稿对得上。
 */
private val GreenSeed = Color(0xFF2E7D32)
private val GreenContainer = Color(0xFFB7F0A8)
private val ErrorRed = Color(0xFFBA1A1A)
private val ErrorRedDark = Color(0xFFFFB4AB)

/** 各功能域的强调色，全应用统一从这里取，避免颜色散落在各个界面里。 */
object AppColors {
    /** 饮食 / 主色。 */
    val Diet = Color(0xFF2E7D32)
    val DietSoft = Color(0xFFE8F5E4)

    /** 运动项目（跑步、游泳…）。 */
    val Sport = Color(0xFF3F6BE0)
    val SportSoft = Color(0xFFE7EDFD)

    /** 撸铁 / 力量训练。 */
    val Strength = Color(0xFFE07C24)
    val StrengthSoft = Color(0xFFFDEFE1)

    /** 拉伸 / 恢复。 */
    val Stretch = Color(0xFF8E5BD8)
    val StretchSoft = Color(0xFFF2EAFB)

    /** 品牌食品 / 包装食品。 */
    val Brand = Color(0xFFC2456B)
    val BrandSoft = Color(0xFFFBE9EF)

    /** 「未设定 / 提示」类的次要色。 */
    val Muted = Color(0xFF6B7280)
}

/** 三大宏量营养素的固定配色，进度条、圆环、小标签都用它。 */
object MacroColors {
    val Carbs = Color(0xFFF2A93B)
    val Protein = Color(0xFF4C8DF6)
    val Fat = Color(0xFFE0668B)
    val Calories = Color(0xFF2E7D32)

    /** 接近目标（90%-100%）时的黄色提醒。 */
    val Warning = Color(0xFFE0A800)
}

private val LightColors = lightColorScheme(
    primary = GreenSeed,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = Color(0xFF042100),

    secondary = Color(0xFF3F6BE0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE6FF),
    onSecondaryContainer = Color(0xFF00174B),

    tertiary = Color(0xFF8E5BD8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEEDDFF),
    onTertiaryContainer = Color(0xFF2A0055),

    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFF7FAF4),
    onBackground = Color(0xFF191D17),
    surface = Color(0xFFFDFEFA),
    onSurface = Color(0xFF191D17),
    surfaceVariant = Color(0xFFE3E8DC),
    onSurfaceVariant = Color(0xFF43483F),
    surfaceContainer = Color(0xFFEFF3EA),
    surfaceContainerHigh = Color(0xFFE8EDE3),
    outline = Color(0xFF73796E),
    outlineVariant = Color(0xFFC3C8BC)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD67F),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF14531C),
    onPrimaryContainer = GreenContainer,

    secondary = Color(0xFFB3C5FF),
    onSecondary = Color(0xFF002B75),
    secondaryContainer = Color(0xFF1F3F8F),
    onSecondaryContainer = Color(0xFFDDE6FF),

    tertiary = Color(0xFFD6BBFF),
    onTertiary = Color(0xFF431A78),
    tertiaryContainer = Color(0xFF5A2E9A),
    onTertiaryContainer = Color(0xFFEEDDFF),

    error = ErrorRedDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF11140F),
    onBackground = Color(0xFFE1E4DB),
    surface = Color(0xFF151812),
    onSurface = Color(0xFFE1E4DB),
    surfaceVariant = Color(0xFF43483F),
    onSurfaceVariant = Color(0xFFC3C8BC),
    surfaceContainer = Color(0xFF1D211A),
    surfaceContainerHigh = Color(0xFF272B23),
    outline = Color(0xFF8D9387),
    outlineVariant = Color(0xFF43483F)
)

/**
 * 圆角统一调大一点：小屏手机上圆角卡片更耐看，长屏幕上也更「整」。
 */
val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

@Composable
fun DietTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    // 界面是 edge-to-edge 的（见 MainActivity），系统栏透明，这里只决定图标明暗。
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DietTrackerTypography,
        shapes = AppShapes,
        content = content
    )
}
