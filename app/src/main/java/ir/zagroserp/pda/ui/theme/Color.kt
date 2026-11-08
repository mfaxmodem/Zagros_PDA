package ir.zagroserp.pda.ui.theme

import androidx.compose.ui.graphics.Color

// ---- brand palette --------------------------------------------------------

val Indigo600 = Color(0xFF4F46E5)
val Indigo500 = Color(0xFF6366F1)
val Indigo100 = Color(0xFFE0E7FF)
val Indigo700 = Color(0xFF4338CA)

val Emerald600 = Color(0xFF059669)
val Emerald500 = Color(0xFF10B981)
val Emerald100 = Color(0xFFD1FAE5)

val Amber600 = Color(0xFFD97706)
val Amber100 = Color(0xFFFEF3C7)

val Coral600 = Color(0xFFDC2626)
val Coral100 = Color(0xFFFEE2E2)

val Violet400 = Color(0xFF818CF8)
val Violet300 = Color(0xFFA5B4FC)
val Violet900 = Color(0xFF312E81)

// ---- light surfaces -------------------------------------------------------

val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightBorder = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF64748B)

// ---- dark surfaces --------------------------------------------------------

val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF273449)
val DarkBorder = Color(0xFF334155)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)

// ---- semantic accents -----------------------------------------------------

val SuccessLight = Emerald600
val SuccessDark = Color(0xFF34D399)
val WarningLight = Amber600
val WarningDark = Color(0xFFFBBF24)
val DangerLight = Coral600
val DangerDark = Color(0xFFF87171)

/**
 * Accent colours that are not part of the Material colour scheme but are used by the
 * dashboard tiles and status pills. Kept in a composition local so dark mode swaps them.
 */
data class ZagrosAccents(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val border: Color,
    val textSecondary: Color,
    val tileGradients: List<List<Color>>,
)

val LightAccents = ZagrosAccents(
    success = SuccessLight,
    successContainer = Emerald100,
    warning = WarningLight,
    warningContainer = Amber100,
    danger = DangerLight,
    dangerContainer = Coral100,
    border = LightBorder,
    textSecondary = LightTextSecondary,
    tileGradients = listOf(
        listOf(Color(0xFF4F46E5), Color(0xFF6366F1)),
        listOf(Color(0xFF059669), Color(0xFF10B981)),
        listOf(Color(0xFFD97706), Color(0xFFF59E0B)),
        listOf(Color(0xFFDC2626), Color(0xFFF87171)),
        listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8)),
        listOf(Color(0xFF7C3AED), Color(0xFFA78BFA)),
    ),
)

val DarkAccents = ZagrosAccents(
    success = SuccessDark,
    successContainer = Color(0xFF064E3B),
    warning = WarningDark,
    warningContainer = Color(0xFF78350F),
    danger = DangerDark,
    dangerContainer = Color(0xFF7F1D1D),
    border = DarkBorder,
    textSecondary = DarkTextSecondary,
    tileGradients = listOf(
        listOf(Color(0xFF4338CA), Color(0xFF6366F1)),
        listOf(Color(0xFF047857), Color(0xFF10B981)),
        listOf(Color(0xFFB45309), Color(0xFFF59E0B)),
        listOf(Color(0xFFB91C1C), Color(0xFFF87171)),
        listOf(Color(0xFF0369A1), Color(0xFF38BDF8)),
        listOf(Color(0xFF6D28D9), Color(0xFFA78BFA)),
    ),
)
