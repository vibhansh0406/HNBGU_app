package `in`.campusone.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.campusone.app.R

// ============================================================================
// MATERIAL3 COLOR SCHEME — Evergreen Studio
// ============================================================================

private val LightColorScheme = ColorScheme(
    primary = Color(0xFF1E634B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4F0E7),
    onPrimaryContainer = Color(0xFF163D2E),
    secondary = Color(0xFF476C78),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EEF0),
    onSecondaryContainer = Color(0xFF163D2E),
    tertiary = Color(0xFFB97937),
    onTertiary = Color(0xFF3B2615),
    tertiaryContainer = Color(0xFFF5E8D4),
    onTertiaryContainer = Color(0xFF4E341B),
    error = Color(0xFFB42318),
    onError = Color.White,
    errorContainer = Color(0xFFFBE5E2),
    onErrorContainer = Color(0xFF4A0E08),
    background = Color(0xFFF6F7F2),
    onBackground = Color(0xFF17231F),
    surface = Color.White,
    onSurface = Color(0xFF17231F),
    surfaceVariant = Color(0xFFEAF0E9),
    onSurfaceVariant = Color(0xFF53665A),
    inverseSurface = Color(0xFF17231F),
    inverseOnSurface = Color(0xFFF6F7F2),
    inversePrimary = Color(0xFFA9D6BC),
    surfaceTint = Color(0xFF1E634B),
    outline = Color(0xFFCED7CE),
    outlineVariant = Color(0xFFE2E8E1),
    scrim = Color(0x99000000),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFD9E2D8),
    surfaceContainer = Color(0xFFEDF1EA),
    surfaceContainerHigh = Color(0xFFD9E2D8),
    surfaceContainerHighest = Color(0xFFDCE4DA),
    surfaceContainerLow = Color(0xFFF2F4EF),
    surfaceContainerLowest = Color.White
)

private val DarkColorScheme = ColorScheme(
    primary = Color(0xFFA9D6BC),
    onPrimary = Color(0xFF163D2E),
    primaryContainer = Color(0xFF294633),
    onPrimaryContainer = Color(0xFFE4F0E7),
    secondary = Color(0xFFB4D3D9),
    onSecondary = Color(0xFF163D2E),
    secondaryContainer = Color(0xFF29404A),
    onSecondaryContainer = Color(0xFFD6E6E8),
    tertiary = Color(0xFFE7BD8A),
    onTertiary = Color(0xFF4E341B),
    tertiaryContainer = Color(0xFF493927),
    onTertiaryContainer = Color(0xFFF0D8B7),
    error = Color(0xFFFF8A7A),
    onError = Color(0xFF4A0E08),
    errorContainer = Color(0xFF4A1510),
    onErrorContainer = Color(0xFFFBE5E2),
    background = Color(0xFF0F1613),
    onBackground = Color(0xFFF6F7F2),
    surface = Color(0xFF17251C),
    onSurface = Color(0xFFF6F7F2),
    surfaceVariant = Color(0xFF1E3024),
    onSurfaceVariant = Color(0xFFB8C9BF),
    inverseSurface = Color(0xFFF6F7F2),
    inverseOnSurface = Color(0xFF17231F),
    inversePrimary = Color(0xFF1E634B),
    surfaceTint = Color(0xFF1E634B),
    outline = Color(0xFF61796A),
    outlineVariant = Color(0xFF344B3D),
    scrim = Color(0xCC000000),
    surfaceBright = Color(0xFF294633),
    surfaceDim = Color(0xFF0F1613),
    surfaceContainer = Color(0xFF1A2921),
    surfaceContainerHigh = Color(0xFF1E3024),
    surfaceContainerHighest = Color(0xFF294633),
    surfaceContainerLow = Color(0xFF17251C),
    surfaceContainerLowest = Color(0xFF0F1613)
)

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

@Composable
private fun CampusOneSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    val window = LocalContext.current.findActivity()?.window
    val barColor = MaterialTheme.colorScheme.background
    if (window != null && !view.isInEditMode) {
        SideEffect {
            window.statusBarColor = barColor.toArgb()
            window.navigationBarColor = barColor.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
}

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
        LocalComponentColors provides componentColors,
        LocalWindowSize provides windowSize
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = campusTypography(windowSize),
            shapes = CampusOneShapes
        ) {
            CampusOneSystemBars(darkTheme = darkTheme)
            content()
        }
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
