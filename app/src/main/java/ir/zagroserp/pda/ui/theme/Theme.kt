package ir.zagroserp.pda.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.zagroserp.pda.data.prefs.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo700,
    secondary = Emerald600,
    onSecondary = Color.White,
    secondaryContainer = Emerald100,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Amber600,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = Coral600,
    onError = Color.White,
    errorContainer = Coral100,
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColorScheme = darkColorScheme(
    primary = Violet400,
    onPrimary = Violet900,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Violet300,
    secondary = SuccessDark,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = WarningDark,
    onTertiary = Color(0xFF451A03),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = DangerDark,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
)

val LocalZagrosAccents = staticCompositionLocalOf { LightAccents }
val LocalZagrosDark = staticCompositionLocalOf { false }

/** Spacing scale shared by every screen – keeps the tile grid visually consistent. */
object ZagrosSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
}

/**
 * Fixed tile heights. Lazy grids and rows measure each item independently, so two tiles in the
 * same row only line up if their height is pinned rather than derived from their content.
 */
object ZagrosDimens {
    val heroTileHeight = 148.dp
    val actionTileHeight = 160.dp
    val statTileHeight = 76.dp
}

@Composable
fun ZagrosPDATheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalZagrosAccents provides if (darkTheme) DarkAccents else LightAccents,
        LocalZagrosDark provides darkTheme,
        // The product is Persian-only: every screen lays out right-to-left. Rows, grids,
        // navigation bars, icons and text alignment all inherit this.
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ZagrosTypography,
            shapes = ZagrosShapes,
            content = content,
        )
    }
}
