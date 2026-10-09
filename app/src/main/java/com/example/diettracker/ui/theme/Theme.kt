package com.example.diettracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// A calm green "nutrition" palette. Kept explicit (rather than dynamic color)
// so the app looks identical on every device and API level.
private val Green40 = Color(0xFF2E7D32)
private val Green80 = Color(0xFF9CD67F)
private val GreenGrey40 = Color(0xFF52634F)
private val GreenGrey80 = Color(0xFFB9CCB4)
private val Sand40 = Color(0xFF6C5D2F)
private val Sand80 = Color(0xFFD9C58C)
private val ErrorRed = Color(0xFFBA1A1A)

/** Colour used for each macro everywhere in the app. */
object MacroColors {
    val Carbs = Color(0xFFF2A93B)
    val Protein = Color(0xFF4C8DF6)
    val Fat = Color(0xFFE0668B)
}

private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F0A0),
    onPrimaryContainer = Color(0xFF052100),
    secondary = GreenGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8CF),
    onSecondaryContainer = Color(0xFF111F10),
    tertiary = Sand40,
    onTertiary = Color.White,
    tertiaryContainer = Sand80,
    onTertiaryContainer = Color(0xFF231B00),
    error = ErrorRed,
    onError = Color.White,
    background = Color(0xFFFBFDF7),
    onBackground = Color(0xFF191D17),
    surface = Color(0xFFFBFDF7),
    onSurface = Color(0xFF191D17),
    surfaceVariant = Color(0xFFDFE4D8),
    onSurfaceVariant = Color(0xFF43483F),
    outline = Color(0xFF73796E)
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF14531C),
    onPrimaryContainer = Color(0xFFB4F0A0),
    secondary = GreenGrey80,
    onSecondary = Color(0xFF253423),
    secondaryContainer = Color(0xFF3B4B38),
    onSecondaryContainer = Color(0xFFD5E8CF),
    tertiary = Sand80,
    onTertiary = Color(0xFF3B2F05),
    tertiaryContainer = Color(0xFF534619),
    onTertiaryContainer = Color(0xFFF7E0A8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF11140F),
    onBackground = Color(0xFFE1E4DB),
    surface = Color(0xFF11140F),
    onSurface = Color(0xFFE1E4DB),
    surfaceVariant = Color(0xFF43483F),
    onSurfaceVariant = Color(0xFFC3C8BC),
    outline = Color(0xFF8D9387)
)

@Composable
fun DietTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    // The Activity is edge-to-edge (see MainActivity), so the system bars are
    // transparent and we only need to pick light or dark bar icons.
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
        content = content
    )
}
