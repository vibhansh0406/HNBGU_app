package `in`.campusone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
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
// MATERIAL3 COLOR SCHEME — Derived from semantic colors
// ============================================================================

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
    onError = LightSemanticColors.onError,
    errorContainer = LightSemanticColors.errorContainer,
    onErrorContainer = LightSemanticColors.onErrorContainer,
    background = LightSemanticColors.background,
    onBackground = LightSemanticColors.onBackground,
    surface = LightSemanticColors.surface,
    onSurface = LightSemanticColors.onSurface,
    surfaceVariant = LightSemanticColors.surfaceVariant,
    onSurfaceVariant = LightSemanticColors.onSurfaceVariant,
    outline = LightSemanticColors.outline,
    outlineVariant = LightSemanticColors.outlineVariant,
    surfaceTint = LightSemanticColors.surfaceTint,
    scrim = LightSemanticColors.scrim
)

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
    onError = DarkSemanticColors.onError,
    errorContainer = DarkSemanticColors.errorContainer,
    onErrorContainer = DarkSemanticColors.onErrorContainer,
    background = DarkSemanticColors.background,
    onBackground = DarkSemanticColors.onBackground,
    surface = DarkSemanticColors.surface,
    onSurface = DarkSemanticColors.onSurface,
    surfaceVariant = DarkSemanticColors.surfaceVariant,
    onSurfaceVariant = DarkSemanticColors.onSurfaceVariant,
    outline = DarkSemanticColors.outline,
    outlineVariant = DarkSemanticColors.outlineVariant,
    surfaceTint = DarkSemanticColors.surfaceTint,
    scrim = DarkSemanticColors.scrim
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
