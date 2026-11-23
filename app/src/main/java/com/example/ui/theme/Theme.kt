package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.util.parseHexColor

@Immutable
data class NabdExtendedColors(
    val hairline: Color,
    val mutedText: Color,
    val likeCoral: Color,
    val repostEmerald: Color,
    val verifiedPersonal: Color,
    val verifiedBusiness: Color,
    val verifiedGovernment: Color,
    val readBlue: Color,
    val warningAmber: Color,
    val cardElevated: Color
)

val LocalNabdColors = staticCompositionLocalOf {
    NabdExtendedColors(
        hairline = HairlineLight,
        mutedText = InkMutedLight,
        likeCoral = LikeCoral,
        repostEmerald = RepostEmerald,
        verifiedPersonal = VerifiedPersonalBlue,
        verifiedBusiness = VerifiedBusinessGold,
        verifiedGovernment = VerifiedGovernmentSlate,
        readBlue = ReadBlue,
        warningAmber = WarningAmber,
        cardElevated = PaperSurface
    )
}

private val LightColorScheme = lightColorScheme(
    primary = PulseTealLight,
    onPrimary = Color.White,
    primaryContainer = PulseTealContainerLight,
    onPrimaryContainer = Color(0xFF0A2E2B),
    secondary = InkSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = PaperSurfaceVariant,
    onSecondaryContainer = InkPrimaryLight,
    background = AlabasterBg,
    onBackground = InkPrimaryLight,
    surface = PaperSurface,
    onSurface = InkPrimaryLight,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = InkSecondaryLight,
    outline = HairlineLight,
    outlineVariant = HairlineLight,
    error = ErrorCrimson,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PulseTealDark,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = PulseTealContainerDark,
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = IvorySecondaryDark,
    onSecondary = ObsidianBg,
    secondaryContainer = SlateSurfaceVariant,
    onSecondaryContainer = IvoryPrimaryDark,
    background = ObsidianBg,
    onBackground = IvoryPrimaryDark,
    surface = SlateSurface,
    onSurface = IvoryPrimaryDark,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = IvorySecondaryDark,
    outline = HairlineDark,
    outlineVariant = HairlineDark,
    error = ErrorCrimson,
    onError = Color.White
)

private val OledColorScheme = darkColorScheme(
    primary = PulseTealDark,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = PulseTealContainerDark,
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = IvorySecondaryDark,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF14181F),
    onSecondaryContainer = IvoryPrimaryDark,
    background = Color.Black,
    onBackground = IvoryPrimaryDark,
    surface = Color.Black,
    onSurface = IvoryPrimaryDark,
    surfaceVariant = Color(0xFF11151C),
    onSurfaceVariant = IvorySecondaryDark,
    outline = Color(0xFF1E242E),
    outlineVariant = Color(0xFF1E242E),
    error = ErrorCrimson,
    onError = Color.White
)

val NabdShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

private fun resolveFontFamily(key: String, languageCode: String): FontFamily = when (key) {
    "amiri" -> AmiriFontFamily
    "tajawal" -> TajawalFontFamily
    "jakarta" -> PlusJakartaSansFamily
    "inter" -> InterFontFamily
    "ibm_arabic" -> IbmPlexArabicFontFamily
    "mono" -> JetBrainsMonoFontFamily
    "cairo" -> if (languageCode == "ar") CairoFontFamily else InterFontFamily
    else -> if (languageCode == "ar") IbmPlexArabicFontFamily else InterFontFamily
}

private fun typographyForFamily(key: String, languageCode: String): Typography {
    val bodyFamily = resolveFontFamily(key, languageCode)
    val headingFamily = if (languageCode == "ar") CairoFontFamily else PlusJakartaSansFamily
    return NabdTypography.copy(
        displaySmall = NabdTypography.displaySmall.copy(fontFamily = headingFamily),
        headlineMedium = NabdTypography.headlineMedium.copy(fontFamily = headingFamily),
        titleLarge = NabdTypography.titleLarge.copy(fontFamily = bodyFamily),
        titleMedium = NabdTypography.titleMedium.copy(fontFamily = bodyFamily),
        bodyLarge = NabdTypography.bodyLarge.copy(fontFamily = bodyFamily),
        bodyMedium = NabdTypography.bodyMedium.copy(fontFamily = bodyFamily),
        labelLarge = NabdTypography.labelLarge.copy(fontFamily = bodyFamily),
        labelMedium = NabdTypography.labelMedium.copy(fontFamily = bodyFamily)
    )
}

@Composable
fun NabdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: String = if (darkTheme) "dark" else "light",
    accentColorHex: String = "#0D9488",
    fontFamilyKey: String = "cairo",
    languageCode: String = "en",
    fontScaleMultiplier: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val effectiveDark = when (themeMode) {
        "dark", "oled" -> true
        "light" -> false
        else -> darkTheme
    }
    val accent = remember(accentColorHex) {
        parseHexColor(accentColorHex, if (effectiveDark) PulseTealDark else PulseTealLight)
    }

    val baseScheme = when {
        themeMode == "oled" -> OledColorScheme
        effectiveDark -> DarkColorScheme
        else -> LightColorScheme
    }
    val colorScheme = baseScheme.copy(
        primary = accent,
        primaryContainer = accent.copy(alpha = if (effectiveDark) 0.20f else 0.12f),
        onPrimaryContainer = if (effectiveDark) Color(0xFFF3F4F6) else Color(0xFF121212)
    )

    val extendedColors = if (effectiveDark) {
        NabdExtendedColors(
            hairline = if (themeMode == "oled") Color(0xFF1E232B) else HairlineDark,
            mutedText = IvorySecondaryDark,
            likeCoral = LikeCoral,
            repostEmerald = RepostEmerald,
            verifiedPersonal = VerifiedPersonalBlue,
            verifiedBusiness = VerifiedBusinessGold,
            verifiedGovernment = VerifiedGovernmentSlate,
            readBlue = ReadBlue,
            warningAmber = WarningAmber,
            cardElevated = if (themeMode == "oled") Color(0xFF0A0D12) else SlateSurface
        )
    } else {
        NabdExtendedColors(
            hairline = HairlineLight,
            mutedText = InkMutedLight,
            likeCoral = LikeCoral,
            repostEmerald = RepostEmerald,
            verifiedPersonal = VerifiedPersonalBlue,
            verifiedBusiness = VerifiedBusinessGold,
            verifiedGovernment = VerifiedGovernmentSlate,
            readBlue = ReadBlue,
            warningAmber = WarningAmber,
            cardElevated = PaperSurface
        )
    }

    val typography = remember(fontFamilyKey, languageCode) {
        typographyForFamily(fontFamilyKey, languageCode)
    }
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = (currentDensity.fontScale * fontScaleMultiplier.coerceIn(0.8f, 1.35f))
    )

    CompositionLocalProvider(
        LocalNabdColors provides extendedColors,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = NabdShapes,
            content = content
        )
    }
}
