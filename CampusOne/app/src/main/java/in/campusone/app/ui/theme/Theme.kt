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

// Forest & Ivory — Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B4D3A),           // Deep Forest
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F0E0),  // Light Mint
    onPrimaryContainer = Color(0xFF0A2D1E),
    secondary = Color(0xFF3D5A4A),         // Forest Green
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0EDE4), // Pale Sage
    onSecondaryContainer = Color(0xFF1A3326),
    tertiary = Color(0xFF7A5C4A),          // Warm Earth
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0E4D9),  // Warm Sand
    onTertiaryContainer = Color(0xFF2E1E12),
    error = Color(0xFFB82E1A),
    errorContainer = Color(0xFFFFE8E4),
    onError = Color.White,
    onErrorContainer = Color(0xFF4A0E08),
    background = Color(0xFFFAF9F6),        // Ivory White
    onBackground = Color(0xFF1A1C1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFF0EEE8),    // Warm Gray
    onSurfaceVariant = Color(0xFF4A4844),
    outline = Color(0xFFD8D6CE),
    outlineVariant = Color(0xFFE8E6E0),
    shadow = Color(0xFF000000),
    scrim = Color(0x99000000),
    inverseSurface = Color(0xFF2A2C29),
    inverseOnSurface = Color(0xFFF5F3ED),
    inversePrimary = Color(0xFF8FD6A8),
    surfaceTint = Color(0xFF1B4D3A),
)

// Forest & Ivory — Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8FD6A8),           // Light Mint
    onPrimary = Color(0xFF052010),
    primaryContainer = Color(0xFF1A3D28),
    onPrimaryContainer = Color(0xFFC8E8D8),
    secondary = Color(0xFFB8D6C4),         // Pale Sage
    onSecondary = Color(0xFF0A2015),
    secondaryContainer = Color(0xFF2A3D32),
    onSecondaryContainer = Color(0xFFD8E8DC),
    tertiary = Color(0xFFD6B89A),          // Warm Sand
    onTertiary = Color(0xFF2E1E12),
    tertiaryContainer = Color(0xFF3D2E22),
    onTertiaryContainer = Color(0xFFF0E4D9),
    error = Color(0xFFFF6B5A),
    errorContainer = Color(0xFF4A1510),
    onError = Color(0xFF3B0A05),
    onErrorContainer = Color(0xFFFFE8E4),
    background = Color(0xFF121412),        // Deep Charcoal
    onBackground = Color(0xFFF5F3ED),
    surface = Color(0xFF1A1C1A),
    onSurface = Color(0xFFF5F3ED),
    surfaceVariant = Color(0xFF2A2C29),
    onSurfaceVariant = Color(0xFFB8B6B0),
    outline = Color(0xFF3A3733),
    outlineVariant = Color(0xFF2D2B28),
    shadow = Color(0xFF000000),
    scrim = Color(0xCC000000),
    inverseSurface = Color(0xFFF5F3ED),
    inverseOnSurface = Color(0xFF1A1C1A),
    inversePrimary = Color(0xFF1B4D3A),
    surfaceTint = Color(0xFF8FD6A8),
)

// iOS-adjacent continuous-feeling corner radii
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
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
        displayLarge = campusType(CampusOneDisplayFamily, FontWeight.Bold, 57, 64, -1.5),
        displayMedium = campusType(CampusOneDisplayFamily, FontWeight.Bold, 45, 52, -1.0),
        displaySmall = campusType(CampusOneDisplayFamily, FontWeight.Bold, 36, 44, -0.8),
        headlineLarge = campusType(CampusOneDisplayFamily, FontWeight.Bold, 32, 40, -0.6),
        headlineMedium = campusType(CampusOneDisplayFamily, FontWeight.SemiBold, 28, 36, -0.4),
        headlineSmall = campusType(CampusOneDisplayFamily, FontWeight.SemiBold, 24, 32, -0.3),
        titleLarge = campusType(CampusOneDisplayFamily, FontWeight.SemiBold, 22, 28, -0.2),
        titleMedium = campusType(CampusOneTextFamily, FontWeight.SemiBold, 16, 24, -0.1),
        titleSmall = campusType(CampusOneTextFamily, FontWeight.SemiBold, 14, 20, -0.1),
        bodyLarge = campusType(CampusOneTextFamily, textWeight, 16, 24 + bodyLine, -0.1),
        bodyMedium = campusType(CampusOneTextFamily, textWeight, 14, 21 + bodyLine, 0.0),
        bodySmall = campusType(CampusOneTextFamily, textWeight, 12, 18 + bodyLine, 0.0),
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
    val componentColors = if (darkTheme) DarkComponentColors else LightComponentColors

    CompositionLocalProvider(
        LocalExtendedColors provides extended,
        LocalComponentColors provides componentColors
    ) {
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

/** Component-specific colors for consistent theming. */
val LocalComponentColors = staticCompositionLocalOf { LightComponentColors }

/** Convenience accessor for component colors inside composables. */
val componentColors: ComponentColors
    @Composable get() = LocalComponentColors.current