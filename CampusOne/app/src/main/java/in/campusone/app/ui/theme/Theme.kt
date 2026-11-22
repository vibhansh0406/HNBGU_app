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
// MATERIAL3 COLOR SCHEME — Forest & Ivory
// ============================================================================

private val LightColorScheme = ColorScheme(
    primary = Color(0xFF183D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7DF),
    onPrimaryContainer = Color(0xFF102D25),
    secondary = Color(0xFF315C4B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EAE4),
    onSecondaryContainer = Color(0xFF102D25),
    tertiary = Color(0xFF7A5C50),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0E4DE),
    onTertiaryContainer = Color(0xFF4A3028),
    error = Color(0xFFB42318),
    onError = Color.White,
    errorContainer = Color(0xFFFBE5E2),
    onErrorContainer = Color(0xFF4A0E08),
    background = Color(0xFFF7F6F0),
    onBackground = Color(0xFF202923),
    surface = Color.White,
    onSurface = Color(0xFF202923),
    surfaceVariant = Color(0xFFEFEEE6),
    onSurfaceVariant = Color(0xFF5E665E),
    inverseSurface = Color(0xFF202923),
    inverseOnSurface = Color(0xFFF7F6F0),
    inversePrimary = Color(0xFF83A995),
    surfaceTint = Color(0xFF183D32),
    outline = Color(0xFFD8D8CC),
    outlineVariant = Color(0xFFE6E6DC),
    scrim = Color(0x99000000),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE7E6DC),
    surfaceContainer = Color(0xFFEDECE3),
    surfaceContainerHigh = Color(0xFFE7E6DC),
    surfaceContainerHighest = Color(0xFFE1E0D5),
    surfaceContainerLow = Color(0xFFF2F1E9),
    surfaceContainerLowest = Color.White
)

private val DarkColorScheme = ColorScheme(
    primary = Color(0xFF83A995),
    onPrimary = Color(0xFF102D25),
    primaryContainer = Color(0xFF234236),
    onPrimaryContainer = Color(0xFFDCE7DF),
    secondary = Color(0xFFA9C4B2),
    onSecondary = Color(0xFF102D25),
    secondaryContainer = Color(0xFF315343),
    onSecondaryContainer = Color(0xFFE2EAE4),
    tertiary = Color(0xFFC9A18E),
    onTertiary = Color(0xFF321E16),
    tertiaryContainer = Color(0xFF51382D),
    onTertiaryContainer = Color(0xFFF0E4DE),
    error = Color(0xFFFF8A7A),
    onError = Color(0xFF4A0E08),
    errorContainer = Color(0xFF4A1510),
    onErrorContainer = Color(0xFFFBE5E2),
    background = Color(0xFF101B17),
    onBackground = Color(0xFFF1F2EA),
    surface = Color(0xFF182720),
    onSurface = Color(0xFFF1F2EA),
    surfaceVariant = Color(0xFF24372D),
    onSurfaceVariant = Color(0xFFC2CDC4),
    inverseSurface = Color(0xFFF1F2EA),
    inverseOnSurface = Color(0xFF182720),
    inversePrimary = Color(0xFF183D32),
    surfaceTint = Color(0xFF83A995),
    outline = Color(0xFF53685B),
    outlineVariant = Color(0xFF2B3C32),
    scrim = Color(0xCC000000),
    surfaceBright = Color(0xFF24372D),
    surfaceDim = Color(0xFF101B17),
    surfaceContainer = Color(0xFF1E3027),
    surfaceContainerHigh = Color(0xFF24372D),
    surfaceContainerHighest = Color(0xFF2B3C32),
    surfaceContainerLow = Color(0xFF182720),
    surfaceContainerLowest = Color(0xFF101B17)
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
