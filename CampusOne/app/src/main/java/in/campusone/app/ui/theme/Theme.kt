package `in`.campusone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.campusone.app.R

// Semantic attendance health colors (forest-harmonized)
val SafeGreen = Color(0xFF2E7D5B)
val SafeGreenContainer = Color(0xFFDDEBE2)
val EdgeAmber = Color(0xFFB45309)
val EdgeAmberContainer = Color(0xFFF7ECD9)
val DangerRed = Color(0xFFB42318)
val DangerRedContainer = Color(0xFFFBE5E2)

// Forest & Ivory — light
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF183D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7DF),
    onPrimaryContainer = Color(0xFF102D25),
    secondary = Color(0xFF315C4B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2EAE4),
    onSecondaryContainer = Color(0xFF102D25),
    tertiary = Color(0xFF7A5C50),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0E4DE),
    onTertiaryContainer = Color(0xFF2E1B14),
    background = Color(0xFFF7F6F0),
    onBackground = Color(0xFF202923),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202923),
    surfaceVariant = Color(0xFFEFEEE6),
    onSurfaceVariant = Color(0xFF5E665E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F1E9),
    surfaceContainer = Color(0xFFEDECE3),
    surfaceContainerHigh = Color(0xFFE7E6DC),
    surfaceContainerHighest = Color(0xFFE1E0D5),
    outline = Color(0xFFE6E6DC),
    outlineVariant = Color(0xFFD8D8CC),
    error = Color(0xFFB42318),
    errorContainer = Color(0xFFFBE5E2),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF4A0E08)
)

// Forest & Ivory — dark
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF83A995),
    onPrimary = Color(0xFF102D25),
    primaryContainer = Color(0xFF24443A),
    onPrimaryContainer = Color(0xFFC9DCD1),
    secondary = Color(0xFF9DB8AA),
    onSecondary = Color(0xFF102D25),
    secondaryContainer = Color(0xFF2A463B),
    onSecondaryContainer = Color(0xFFD5E4DB),
    tertiary = Color(0xFFD3B8AC),
    onTertiary = Color(0xFF2E1B14),
    tertiaryContainer = Color(0xFF4A352C),
    onTertiaryContainer = Color(0xFFF0E4DE),
    background = Color(0xFF101B17),
    onBackground = Color(0xFFF1F2EA),
    surface = Color(0xFF182720),
    onSurface = Color(0xFFF1F2EA),
    surfaceVariant = Color(0xFF20332A),
    onSurfaceVariant = Color(0xFFA6B0A7),
    surfaceContainerLowest = Color(0xFF0D1713),
    surfaceContainerLow = Color(0xFF14211B),
    surfaceContainer = Color(0xFF192720),
    surfaceContainerHigh = Color(0xFF1E2F27),
    surfaceContainerHighest = Color(0xFF243630),
    outline = Color(0xFF2B3C32),
    outlineVariant = Color(0xFF24352C),
    error = Color(0xFFFF8A7A),
    errorContainer = Color(0xFF4A1510),
    onError = Color(0xFF3B0A05),
    onErrorContainer = Color(0xFFFBE5E2)
)

// iOS-adjacent continuous-feeling corner radii
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

// ---------------------------------------------------------------------------
// TYPOGRAPHY — Manrope (display voice) + Inter (text voice) + Noto Sans
// Devanagari (bundled Hindi fallback).
//
// Roles: display/headline/title carry the brand voice and use Manrope; body,
// label and form roles use Inter. Every style ships the Devanagari fallback so
// Hindi never renders in an unknown system face.
// ---------------------------------------------------------------------------

/** Display voice: hero numbers, screen titles, section headings. */
val CampusOneDisplayFamily = FontFamily(
    Font(R.font.manrope_variable, FontWeight.Normal),
    Font(R.font.manrope_variable, FontWeight.Medium),
    Font(R.font.manrope_variable, FontWeight.SemiBold),
    Font(R.font.manrope_variable, FontWeight.Bold)
)

/** Text voice: body copy, navigation, buttons, labels, forms. */
val CampusOneTextFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
    Font(R.font.inter_variable, FontWeight.Bold)
)

private fun campusType(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    tracking: Double = 0.0
): TextStyle = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp
)

/**
 * The type scale. On dark surfaces body text gets one extra point of leading
 * and a touch more tracking so light-on-dark stays comfortable.
 */
private fun campusTypography(dark: Boolean): Typography {
    val bodyLine = if (dark) 1 else 0
    val textWeight = if (dark) FontWeight.Normal else FontWeight.Normal
    return Typography(
        displayLarge = campusType(CampusOneDisplayFamily, FontWeight.Bold, 40, 46 + bodyLine, -1.0),
        displayMedium = campusType(CampusOneDisplayFamily, FontWeight.Bold, 34, 40 + bodyLine, -0.8),
        displaySmall = campusType(CampusOneDisplayFamily, FontWeight.Bold, 30, 36 + bodyLine, -0.6),
        headlineLarge = campusType(CampusOneDisplayFamily, FontWeight.Bold, 26, 32 + bodyLine, -0.4),
        headlineMedium = campusType(CampusOneDisplayFamily, FontWeight.SemiBold, 22, 28 + bodyLine, -0.3),
        headlineSmall = campusType(CampusOneDisplayFamily, FontWeight.SemiBold, 19, 25 + bodyLine, -0.2),
        titleLarge = campusType(CampusOneDisplayFamily, FontWeight.SemiBold, 18, 24 + bodyLine, -0.2),
        titleMedium = campusType(CampusOneTextFamily, FontWeight.SemiBold, 16, 22 + bodyLine, -0.1),
        titleSmall = campusType(CampusOneTextFamily, FontWeight.SemiBold, 14, 20 + bodyLine, -0.1),
        bodyLarge = campusType(CampusOneTextFamily, textWeight, 16, 24 + bodyLine, -0.1),
        bodyMedium = campusType(CampusOneTextFamily, textWeight, 14, 21 + bodyLine, 0.0),
        bodySmall = campusType(CampusOneTextFamily, textWeight, 13, 19 + bodyLine, 0.0),
        labelLarge = campusType(CampusOneTextFamily, FontWeight.SemiBold, 14, 20, 0.0),
        labelMedium = campusType(CampusOneTextFamily, FontWeight.Medium, 12, 16, 0.1),
        labelSmall = campusType(CampusOneTextFamily, FontWeight.Medium, 11, 14, 0.2)
    )
}

@Composable
fun CampusOneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = campusTypography(darkTheme),
            shapes = ExpressiveShapes,
            content = content
        )
    }
}

/** Theme-aware semantic colors: success/warning/info pairs and the hairline. */
val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Convenience accessor for semantic colors inside composables. */
val extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current
