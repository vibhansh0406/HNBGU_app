package `in`.campusone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
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

// ============================================================================
// MATERIAL3 COLOR SCHEME — Custom CampusOne palette (Slate & Amber)
// ============================================================================

private val LightColorScheme = ColorScheme(
    primary = Color(0xFF1E293B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF022C22),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF3B0764),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A),
    background = Color(0xFFFAFAF9),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFFAFAF9),
    inversePrimary = Color(0xFFCBD5E1),
    surfaceTint = Color(0xFF1E293B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    scrim = Color(0x99000000),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFF1F5F9),
    surfaceContainer = Color(0xFFF8FAFC),
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    surfaceContainerLow = Color(0xFFFAFAF9),
    surfaceContainerLowest = Color.White
)

private val DarkColorScheme = ColorScheme(
    primary = Color(0xFFCBD5E1),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFCBD5E1),
    secondary = Color(0xFF14B8A6),
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF0F766E),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFA855F7),
    onTertiary = Color(0xFF3B0764),
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Color(0xFFEDE9FE),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF020617),
    onBackground = Color(0xFFFAFAF9),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFFAFAF9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    inverseSurface = Color(0xFFFAFAF9),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = Color(0xFF1E293B),
    surfaceTint = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    scrim = Color(0xCC000000),
    surfaceBright = Color(0xFF0F172A),
    surfaceDim = Color(0xFF0B1120),
    surfaceContainer = Color(0xFF1E293B),
    surfaceContainerHigh = Color(0xFF1E293B),
    surfaceContainerHighest = Color(0xFF334155),
    surfaceContainerLow = Color(0xFF0F172A),
    surfaceContainerLowest = Color(0xFF020617)
)

// ============================================================================
// SHAPES
// ============================================================================

val CampusOneShapes = Shapes(
    extraSmall = C1Radius.xs,
    small = C1Radius.sm,
    medium = C1Radius.md,
    large = C1Radius.lg,
    extraLarge = C1Radius.xxl
)

// ============================================================================
// TYPOGRAPHY — Adaptive based on window size
// ============================================================================

/**
 * Create Material3 Typography from adaptive C1Typography.
 */
fun campusTypography(windowSize: C1WindowSize): Typography {
    val c1 = c1Typography(windowSize)
    return Typography(
        displayLarge = c1.displayLarge,
        displayMedium = c1.displayMedium,
        displaySmall = c1.displaySmall,
        headlineLarge = c1.headlineLarge,
        headlineMedium = c1.headlineMedium,
        headlineSmall = c1.headlineSmall,
        titleLarge = c1.titleLarge,
        titleMedium = c1.titleMedium,
        titleSmall = c1.titleSmall,
        bodyLarge = c1.bodyLarge,
        bodyMedium = c1.bodyMedium,
        bodySmall = c1.bodySmall,
        labelLarge = c1.labelLarge,
        labelMedium = c1.labelMedium,
        labelSmall = c1.labelSmall
    )
}

// ============================================================================
// THEME COMPOSABLE
// ============================================================================

@Composable
fun CampusOneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    windowSize: C1WindowSize = C1WindowSize.COMPACT,
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
            typography = campusTypography(windowSize),
            shapes = CampusOneShapes,
            content = content
        )
    }
}

// ============================================================================
// COMPOSITION LOCALS
// ============================================================================

/** Theme-aware semantic colors: success/warning/info pairs and the hairline. */
val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Convenience accessor for semantic colors inside composables. */
val extendedColors: C1ExtendedColors
    @Composable get() = LocalExtendedColors.current

/** Component-specific colors for consistent theming. */
val LocalComponentColors = staticCompositionLocalOf { LightComponentColors }

/** Convenience accessor for component colors inside composables. */
val componentColors: C1ComponentColors
    @Composable get() = LocalComponentColors.current

/** Window size class for responsive layout decisions. */
val LocalWindowSize = staticCompositionLocalOf { C1WindowSize.COMPACT }

/** Convenience accessor for window size inside composables. */
val windowSize: C1WindowSize
    @Composable get() = LocalWindowSize.current
