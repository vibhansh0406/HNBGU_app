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

// Forest & Ivory — Light Color Scheme (using semantic colors)
private val LightColorScheme = lightColorScheme(
    primary = LightSemanticColors.primary,
    onPrimary = LightSemanticColors.onPrimary,
    primaryContainer = LightSemanticColors.primaryContainer,
    onPrimaryContainer = LightSemanticColors.onPrimaryContainer,
    secondary = LightSemanticColors.secondary,
    onSecondary = LightSemanticColors.onSecondary,
    secondaryContainer = LightSemanticColors.secondaryContainer,
    onSecondaryContainer = LightSemanticColors.onSecondaryContainer,
    tertiary = LightSemanticColors.tertiary,
    onTertiary = LightSemanticColors.onTertiary,
    tertiaryContainer = LightSemanticColors.tertiaryContainer,
    onTertiaryContainer = LightSemanticColors.onTertiaryContainer,
    error = LightSemanticColors.error,
    errorContainer = LightSemanticColors.errorContainer,
    onError = LightSemanticColors.onError,
    onErrorContainer = LightSemanticColors.onErrorContainer,
    background = LightSemanticColors.background,
    onBackground = LightSemanticColors.onBackground,
    surface = LightSemanticColors.surface,
    onSurface = LightSemanticColors.onSurface,
    surfaceVariant = LightSemanticColors.surfaceVariant,
    onSurfaceVariant = LightSemanticColors.onSurfaceVariant,
    outline = LightSemanticColors.outline,
    outlineVariant = LightSemanticColors.outlineVariant,
    scrim = LightSemanticColors.scrim,
    inverseSurface = LightSemanticColors.inverseSurface,
    inverseOnSurface = LightSemanticColors.inverseOnSurface,
    inversePrimary = LightSemanticColors.inversePrimary,
    surfaceTint = LightSemanticColors.surfaceTint,
)

// Forest & Ivory — Dark Color Scheme (using semantic colors)
private val DarkColorScheme = darkColorScheme(
    primary = DarkSemanticColors.primary,
    onPrimary = DarkSemanticColors.onPrimary,
    primaryContainer = DarkSemanticColors.primaryContainer,
    onPrimaryContainer = DarkSemanticColors.onPrimaryContainer,
    secondary = DarkSemanticColors.secondary,
    onSecondary = DarkSemanticColors.onSecondary,
    secondaryContainer = DarkSemanticColors.secondaryContainer,
    onSecondaryContainer = DarkSemanticColors.onSecondaryContainer,
    tertiary = DarkSemanticColors.tertiary,
    onTertiary = DarkSemanticColors.onTertiary,
    tertiaryContainer = DarkSemanticColors.tertiaryContainer,
    onTertiaryContainer = DarkSemanticColors.onTertiaryContainer,
    error = DarkSemanticColors.error,
    errorContainer = DarkSemanticColors.errorContainer,
    onError = DarkSemanticColors.onError,
    onErrorContainer = DarkSemanticColors.onErrorContainer,
    background = DarkSemanticColors.background,
    onBackground = DarkSemanticColors.onBackground,
    surface = DarkSemanticColors.surface,
    onSurface = DarkSemanticColors.onSurface,
    surfaceVariant = DarkSemanticColors.surfaceVariant,
    onSurfaceVariant = DarkSemanticColors.onSurfaceVariant,
    outline = DarkSemanticColors.outline,
    outlineVariant = DarkSemanticColors.outlineVariant,
    scrim = DarkSemanticColors.scrim,
    inverseSurface = DarkSemanticColors.inverseSurface,
    inverseOnSurface = DarkSemanticColors.inverseOnSurface,
    inversePrimary = DarkSemanticColors.inversePrimary,
    surfaceTint = DarkSemanticColors.surfaceTint,
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