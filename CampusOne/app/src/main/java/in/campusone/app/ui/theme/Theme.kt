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
// MATERIAL3 COLOR SCHEME — Campus Spectrum
// ============================================================================

private val LightColorScheme = ColorScheme(
    primary = Color(0xFF3156D9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5EAFE),
    onPrimaryContainer = Color(0xFF172B70),
    secondary = Color(0xFF6849C8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEE9FF),
    onSecondaryContainer = Color(0xFF172B70),
    tertiary = Color(0xFFD65F4B),
    onTertiary = Color(0xFF32100B),
    tertiaryContainer = Color(0xFFFFE7E1),
    onTertiaryContainer = Color(0xFF5A2118),
    error = Color(0xFFB42318),
    onError = Color.White,
    errorContainer = Color(0xFFFBE5E2),
    onErrorContainer = Color(0xFF4A0E08),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF1B2030),
    surface = Color.White,
    onSurface = Color(0xFF1B2030),
    surfaceVariant = Color(0xFFECEEF5),
    onSurfaceVariant = Color(0xFF596176),
    inverseSurface = Color(0xFF1B2030),
    inverseOnSurface = Color(0xFFF6F7FB),
    inversePrimary = Color(0xFFA9B9FF),
    surfaceTint = Color(0xFF3156D9),
    outline = Color(0xFFC8CDDA),
    outlineVariant = Color(0xFFE3E6EF),
    scrim = Color(0x99000000),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDDE1EC),
    surfaceContainer = Color(0xFFE7EAF2),
    surfaceContainerHigh = Color(0xFFDDE1EC),
    surfaceContainerHighest = Color(0xFFD6DAE6),
    surfaceContainerLow = Color(0xFFF0F2F8),
    surfaceContainerLowest = Color.White
)

private val DarkColorScheme = ColorScheme(
    primary = Color(0xFFA9B9FF),
    onPrimary = Color(0xFF172B70),
    primaryContainer = Color(0xFF263B85),
    onPrimaryContainer = Color(0xFFE5EAFE),
    secondary = Color(0xFFC9B8FF),
    onSecondary = Color(0xFF172B70),
    secondaryContainer = Color(0xFF40316C),
    onSecondaryContainer = Color(0xFFEEE9FF),
    tertiary = Color(0xFFFFB5A5),
    onTertiary = Color(0xFF4C160F),
    tertiaryContainer = Color(0xFF733B31),
    onTertiaryContainer = Color(0xFFFFE7E1),
    error = Color(0xFFFF8A7A),
    onError = Color(0xFF4A0E08),
    errorContainer = Color(0xFF4A1510),
    onErrorContainer = Color(0xFFFBE5E2),
    background = Color(0xFF11131D),
    onBackground = Color(0xFFF3F4FA),
    surface = Color(0xFF191D2B),
    onSurface = Color(0xFFF3F4FA),
    surfaceVariant = Color(0xFF242A3A),
    onSurfaceVariant = Color(0xFFD3D8E7),
    inverseSurface = Color(0xFFF3F4FA),
    inverseOnSurface = Color(0xFF191D2B),
    inversePrimary = Color(0xFF3156D9),
    surfaceTint = Color(0xFFA9B9FF),
    outline = Color(0xFF66708A),
    outlineVariant = Color(0xFF30374B),
    scrim = Color(0xCC000000),
    surfaceBright = Color(0xFF242A3A),
    surfaceDim = Color(0xFF11131D),
    surfaceContainer = Color(0xFF20263A),
    surfaceContainerHigh = Color(0xFF242A3A),
    surfaceContainerHighest = Color(0xFF30374B),
    surfaceContainerLow = Color(0xFF191D2B),
    surfaceContainerLowest = Color(0xFF11131D)
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
