package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val InterFontFamily = FontFamily(Font(R.font.inter, FontWeight.Normal))
val PlusJakartaSansFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val IbmPlexArabicFontFamily = FontFamily(Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal))
val CairoFontFamily = FontFamily(Font(R.font.cairo, FontWeight.Normal))
val AmiriFontFamily = FontFamily(Font(R.font.amiri, FontWeight.Normal))
val TajawalFontFamily = FontFamily(Font(R.font.tajawal, FontWeight.Normal))
val JetBrainsMonoFontFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

fun storyFontFamily(styleKey: String): FontFamily = when (styleKey) {
    "amiri" -> AmiriFontFamily
    "tajawal" -> TajawalFontFamily
    "mono" -> JetBrainsMonoFontFamily
    "ibm_arabic" -> IbmPlexArabicFontFamily
    else -> CairoFontFamily
}

val NabdTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)
