package ir.zagroserp.pda.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ir.zagroserp.pda.R

/** Plain IRANSans – digits render exactly as written. Used for URLs, IPs and version strings. */
val IranSans = FontFamily(
    Font(R.font.iransans_ultralight, FontWeight.ExtraLight),
    Font(R.font.iransans_light, FontWeight.Light),
    Font(R.font.iransans_regular, FontWeight.Normal),
    Font(R.font.iransans_medium, FontWeight.Medium),
    Font(R.font.iransans_bold, FontWeight.Bold),
    Font(R.font.iransans_black, FontWeight.Black),
)

/**
 * IRANSans with Persian digit glyphs mapped onto the Latin digit code points.
 *
 * This is the **default** family: it guarantees that any number reaching the UI renders with
 * Persian digits, including values that come straight from the server and are never passed
 * through a formatter (order numbers, invoice numbers, product codes). Contexts where Latin
 * digits are meaningful — server addresses, ports, version strings — explicitly opt back into
 * [IranSans].
 */
val IranSansFaNum = FontFamily(
    Font(R.font.iransans_ultralight_fanum, FontWeight.ExtraLight),
    Font(R.font.iransans_light_fanum, FontWeight.Light),
    Font(R.font.iransans_regular_fanum, FontWeight.Normal),
    Font(R.font.iransans_medium_fanum, FontWeight.Medium),
    Font(R.font.iransans_bold_fanum, FontWeight.Bold),
    Font(R.font.iransans_black_fanum, FontWeight.Black),
)

/**
 * Persian-first type scale: slightly generous line heights and heavier display weights so the
 * dashboard stays legible at arm's length in the field.
 */
val ZagrosTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = IranSansFaNum,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 44.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = IranSansFaNum,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 40.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = IranSansFaNum,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = IranSansFaNum,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = IranSansFaNum,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = IranSansFaNum,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 23.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = IranSansFaNum,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = IranSansFaNum,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 23.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = IranSansFaNum,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = IranSansFaNum,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = IranSansFaNum,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 19.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = IranSansFaNum,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 17.sp,
    ),
)
