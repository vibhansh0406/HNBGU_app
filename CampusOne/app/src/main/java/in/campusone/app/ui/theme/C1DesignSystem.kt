package `in`.campusone.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.campusone.app.R

/**
 * CampusOne Design System — "Campus Spectrum"
 *
 * A complete, production-grade design language with:
 * - Responsive typography that scales with viewport
 * - Adaptive spacing based on window size class
 * - Semantic color roles with light/dark pairs
 * - Motion standards for transitions and micro-interactions
 * - Breakpoint-driven layout behavior
 */

// ============================================================================
// WINDOW SIZE CLASSES — Responsive breakpoints
// ============================================================================

/**
 * Window size class for responsive layout decisions.
 * Based on actual available width dp, not device assumptions.
 */
enum class C1WindowSize {
    /** Phones in portrait, small foldables. < 600dp width */
    COMPACT,
    /** Large phones portrait, foldables portrait, small tablets. 600-840dp */
    MEDIUM,
    /** Tablets portrait, foldables landscape. 840-1200dp */
    LARGE,
    /** Tablets landscape, desktop, large foldables. > 1200dp */
    EXPANDED
}

object C1Breakpoint {
    val compact = 600.dp
    val medium = 840.dp
    val expanded = 1200.dp
}

/**
 * Convert width dp to window size class.
 */
fun C1WindowSize.fromWidthDp(widthDp: Dp): C1WindowSize {
    val w = widthDp.value
    return when {
        w < 600f -> C1WindowSize.COMPACT
        w < 840f -> C1WindowSize.MEDIUM
        w < 1200f -> C1WindowSize.LARGE
        else -> C1WindowSize.EXPANDED
    }
}

// ============================================================================
// SPACING SYSTEM — 4dp base, adaptive multipliers
// ============================================================================

object C1Spacing {
    val none = 0.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val xxxxl = 40.dp
    val xxxxxl = 48.dp
    val xxxxxx = 64.dp

    /** Adaptive horizontal padding based on window size. */
    fun pagePadding(windowSize: C1WindowSize): Dp = when (windowSize) {
        C1WindowSize.COMPACT -> 16.dp
        C1WindowSize.MEDIUM -> 20.dp
        C1WindowSize.LARGE -> 28.dp
        C1WindowSize.EXPANDED -> 40.dp
    }

    /** Adaptive section gap based on window size. */
    fun sectionGap(windowSize: C1WindowSize): Dp = when (windowSize) {
        C1WindowSize.COMPACT -> 16.dp
        C1WindowSize.MEDIUM -> 20.dp
        C1WindowSize.LARGE -> 28.dp
        C1WindowSize.EXPANDED -> 36.dp
    }

    /** Adaptive card gap based on window size. */
    fun cardGap(windowSize: C1WindowSize): Dp = when (windowSize) {
        C1WindowSize.COMPACT -> 12.dp
        C1WindowSize.MEDIUM -> 16.dp
        C1WindowSize.LARGE -> 20.dp
        C1WindowSize.EXPANDED -> 24.dp
    }

    /** Number of grid columns based on window size. */
    fun gridColumns(windowSize: C1WindowSize): Int = when (windowSize) {
        C1WindowSize.COMPACT -> 1
        C1WindowSize.MEDIUM -> 2
        C1WindowSize.LARGE -> 3
        C1WindowSize.EXPANDED -> 4
    }

    /** Grid item width based on window size and column count. */
    fun gridItemWidth(windowSize: C1WindowSize, totalWidth: Dp): Dp {
        val cols = gridColumns(windowSize)
        val gap = cardGap(windowSize) * (cols - 1)
        val padding = pagePadding(windowSize) * 2
        return (totalWidth - padding - gap) / cols
    }
}

// ============================================================================
// RADIUS — Component-specific corner radii
// ============================================================================

object C1Radius {
    val none = RoundedCornerShape(0.dp)
    val xs = RoundedCornerShape(10.dp)
    val sm = RoundedCornerShape(14.dp)
    val md = RoundedCornerShape(18.dp)
    val lg = RoundedCornerShape(24.dp)
    val xl = RoundedCornerShape(30.dp)
    val xxl = RoundedCornerShape(30.dp)
    val pill = RoundedCornerShape(50)
    val circle = RoundedCornerShape(50)
}

// ============================================================================
// ELEVATION — Flat-first, shadows only for floating elements
// ============================================================================

object C1Elevation {
    val flat = 0.dp
    val level1 = 1.dp
    val level2 = 4.dp
    val level3 = 8.dp
    val level4 = 16.dp
    val floating = 24.dp
}

// ============================================================================
// ICON SIZES
// ============================================================================

object C1IconSize {
    val inline = 16.dp
    val small = 20.dp
    val standard = 24.dp
    val large = 32.dp
    val xl = 40.dp
    val xxl = 48.dp
}

// ============================================================================
// MOTION — Spring and ease curves
// ============================================================================

object C1Motion {
    val instantMillis = 80
    val fastMillis = 150
    val normalMillis = 250
    val slowMillis = 400
    val slowerMillis = 600
    val emphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standardEasing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)
    val decelerateEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    val accelerateEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    val bounceEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
    val gentleEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    val premiumEasing = CubicBezierEasing(0.23f, 1.0f, 0.32f, 1.0f)

    val pressScale = 0.96f
    val pressScaleStrong = 0.92f

    // Spring animations for premium feel
    val springDampingRatio = Spring.DampingRatioMediumBouncy
    val springStiffness = Spring.StiffnessMedium
    val springFastStiffness = Spring.StiffnessHigh
    val springSlowStiffness = Spring.StiffnessLow
}

// ============================================================================
// BORDER
// ============================================================================

object C1Border {
    val hairline = 0.5.dp
    val thin = 1.dp
    val emphasis = 1.5.dp
    val focus = 2.dp
}

// ============================================================================
// HEIGHTS — Touch targets and component heights
// ============================================================================

object C1Height {
    val touch = 48.dp
    val button = 48.dp
    val buttonSmall = 40.dp
    val buttonLarge = 56.dp
    val field = 56.dp
    val chip = 32.dp
    val listRow = 56.dp
    val listRowCompact = 48.dp
    val navBar = 80.dp
    val topBar = 64.dp
    val bottomSheetHandle = 4.dp
}

val C1TouchTarget: Dp = 48.dp

/**
 * Button height based on size variant.
 */
fun C1ButtonHeight(size: C1ButtonSize): Dp = when (size) {
    C1ButtonSize.SMALL -> 40.dp
    C1ButtonSize.MEDIUM -> 48.dp
    C1ButtonSize.LARGE -> 56.dp
}

/**
 * Button horizontal padding based on size variant.
 */
fun C1ButtonPadding(size: C1ButtonSize): Dp = when (size) {
    C1ButtonSize.SMALL -> 16.dp
    C1ButtonSize.MEDIUM -> 24.dp
    C1ButtonSize.LARGE -> 32.dp
}

// ============================================================================
// STATE OPACITIES
// ============================================================================

object C1State {
    const val pressedAlpha = 0.08f
    const val selectedAlpha = 0.10f
    const val disabledAlpha = 0.38f
    const val hoverAlpha = 0.04f
    const val focusAlpha = 0.12f
}

// ============================================================================
// SEMANTIC COLORS — Light & Dark
// ============================================================================

@Immutable
data class C1SemanticColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
    val surfaceTint: Color,
    val scrim: Color
)

val LightSemanticColors = C1SemanticColors(
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
    outline = Color(0xFFC8CDDA),
    outlineVariant = Color(0xFFE3E6EF),
    inverseSurface = Color(0xFF1B2030),
    inverseOnSurface = Color(0xFFF6F7FB),
    inversePrimary = Color(0xFFA9B9FF),
    surfaceTint = Color(0xFF3156D9),
    scrim = Color(0x99000000)
)

val DarkSemanticColors = C1SemanticColors(
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
    outline = Color(0xFF66708A),
    outlineVariant = Color(0xFF30374B),
    inverseSurface = Color(0xFFF3F4FA),
    inverseOnSurface = Color(0xFF191D2B),
    inversePrimary = Color(0xFF3156D9),
    surfaceTint = Color(0xFFA9B9FF),
    scrim = Color(0xCC000000)
)

// ============================================================================
// EXTENDED COLORS — Semantic roles beyond Material3 ColorScheme
// ============================================================================

@Immutable
data class C1ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val hairline: Color,
    val divider: Color,
    val scrim: Color,
    val overlay: Color,
    val surfaceTint: Color
)

val LightExtendedColors = C1ExtendedColors(
    success = Color(0xFF2E7D5B),
    onSuccess = Color.White,
    successContainer = Color(0xFFDDEBE2),
    onSuccessContainer = Color(0xFF143A2B),
    warning = Color(0xFFB45309),
    onWarning = Color.White,
    warningContainer = Color(0xFFF7ECD9),
    onWarningContainer = Color(0xFF4A2B08),
    info = Color(0xFF087EAC),
    onInfo = Color.White,
    infoContainer = Color(0xFFD8F1FA),
    onInfoContainer = Color(0xFF07354B),
    hairline = Color(0xFFE3E6EF),
    divider = Color(0xFFC8CDDA),
    scrim = Color(0x99000000),
    overlay = Color(0xCC000000),
    surfaceTint = Color(0xFF3156D9)
)

val DarkExtendedColors = C1ExtendedColors(
    success = Color(0xFF8CC7A6),
    onSuccess = Color(0xFF143A2B),
    successContainer = Color(0xFF1E3B2E),
    onSuccessContainer = Color(0xFFDDEBE2),
    warning = Color(0xFFE5B36B),
    onWarning = Color(0xFF3F2E14),
    warningContainer = Color(0xFF3F2E14),
    onWarningContainer = Color(0xFFF7ECD9),
    info = Color(0xFF8DDCF1),
    onInfo = Color(0xFF07354B),
    infoContainer = Color(0xFF173D4B),
    onInfoContainer = Color(0xFFD8F1FA),
    hairline = Color(0xFF30374B),
    divider = Color(0xFF3B4257),
    scrim = Color(0xCC000000),
    overlay = Color(0xDD000000),
    surfaceTint = Color(0xFFA9B9FF)
)

// ============================================================================
// COMPONENT COLORS
// ============================================================================

@Immutable
data class C1ComponentColors(
    val cardBackground: Color,
    val cardBorder: Color,
    val cardHover: Color,
    val cardPressed: Color,
    val surfaceElevated: Color,
    val surfaceElevatedBorder: Color,
    val inputBackground: Color,
    val inputBorder: Color,
    val inputBorderFocused: Color,
    val inputBorderError: Color,
    val chipBackground: Color,
    val chipBackgroundSelected: Color,
    val chipBorder: Color,
    val chipBorderSelected: Color,
    val dividerColor: Color,
    val skeletonBase: Color,
    val skeletonHighlight: Color,
    val badgeBackground: Color,
    val badgeContent: Color
)

val LightComponentColors = C1ComponentColors(
    cardBackground = Color.White,
    cardBorder = Color(0xFFE3E6EF),
    cardHover = Color(0xFFF0F2F8),
    cardPressed = Color(0xFFE7EAF2),
    surfaceElevated = Color.White,
    surfaceElevatedBorder = Color(0xFFC8CDDA),
    inputBackground = Color.White,
    inputBorder = Color(0xFFC8CDDA),
    inputBorderFocused = Color(0xFF3156D9),
    inputBorderError = Color(0xFFB42318),
    chipBackground = Color(0xFFECEEF5),
    chipBackgroundSelected = Color(0xFF3156D9),
    chipBorder = Color(0xFFC8CDDA),
    chipBorderSelected = Color(0xFF3156D9),
    dividerColor = Color(0xFFE3E6EF),
    skeletonBase = Color(0xFFE7EAF2),
    skeletonHighlight = Color(0xFFF6F7FB),
    badgeBackground = Color(0xFF3156D9),
    badgeContent = Color.White
)

val DarkComponentColors = C1ComponentColors(
    cardBackground = Color(0xFF191D2B),
    cardBorder = Color(0xFF30374B),
    cardHover = Color(0xFF242A3A),
    cardPressed = Color(0xFF30374B),
    surfaceElevated = Color(0xFF20263A),
    surfaceElevatedBorder = Color(0xFF3B4257),
    inputBackground = Color(0xFF191D2B),
    inputBorder = Color(0xFF3B4257),
    inputBorderFocused = Color(0xFFA9B9FF),
    inputBorderError = Color(0xFFFF8A7A),
    chipBackground = Color(0xFF242A3A),
    chipBackgroundSelected = Color(0xFFA9B9FF),
    chipBorder = Color(0xFF3B4257),
    chipBorderSelected = Color(0xFFA9B9FF),
    dividerColor = Color(0xFF30374B),
    skeletonBase = Color(0xFF242A3A),
    skeletonHighlight = Color(0xFF3B4257),
    badgeBackground = Color(0xFFA9B9FF),
    badgeContent = Color(0xFF172B70)
)

// ============================================================================
// TYPOGRAPHY — Fluid, adaptive scale
// ============================================================================

@Immutable
data class C1Typography(
    val displayLarge: androidx.compose.ui.text.TextStyle,
    val displayMedium: androidx.compose.ui.text.TextStyle,
    val displaySmall: androidx.compose.ui.text.TextStyle,
    val headlineLarge: androidx.compose.ui.text.TextStyle,
    val headlineMedium: androidx.compose.ui.text.TextStyle,
    val headlineSmall: androidx.compose.ui.text.TextStyle,
    val titleLarge: androidx.compose.ui.text.TextStyle,
    val titleMedium: androidx.compose.ui.text.TextStyle,
    val titleSmall: androidx.compose.ui.text.TextStyle,
    val bodyLarge: androidx.compose.ui.text.TextStyle,
    val bodyMedium: androidx.compose.ui.text.TextStyle,
    val bodySmall: androidx.compose.ui.text.TextStyle,
    val labelLarge: androidx.compose.ui.text.TextStyle,
    val labelMedium: androidx.compose.ui.text.TextStyle,
    val labelSmall: androidx.compose.ui.text.TextStyle
)

val CampusOneDisplayFamily = FontFamily(
    Font(R.font.manrope_variable, FontWeight.Normal),
    Font(R.font.manrope_variable, FontWeight.Medium),
    Font(R.font.manrope_variable, FontWeight.SemiBold),
    Font(R.font.manrope_variable, FontWeight.Bold)
)

val CampusOneTextFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
    Font(R.font.inter_variable, FontWeight.Bold)
)

private fun c1Type(
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
 * Adaptive typography based on window size.
 * On larger screens, headings scale up slightly; body text stays readable.
 */
fun c1Typography(windowSize: C1WindowSize): C1Typography {
    val scale = when (windowSize) {
        C1WindowSize.COMPACT -> 1.0f
        C1WindowSize.MEDIUM -> 1.05f
        C1WindowSize.LARGE -> 1.1f
        C1WindowSize.EXPANDED -> 1.15f
    }

    fun scaled(size: Int): Float = (size * scale).coerceAtLeast(12f)

    return C1Typography(
        displayLarge = c1Type(CampusOneDisplayFamily, FontWeight.Bold, 57, 64, -1.5),
        displayMedium = c1Type(CampusOneDisplayFamily, FontWeight.Bold, 45, 52, -1.0),
        displaySmall = c1Type(CampusOneDisplayFamily, FontWeight.Bold, 36, 44, -0.8),
        headlineLarge = c1Type(CampusOneDisplayFamily, FontWeight.Bold, 32, 40, -0.6),
        headlineMedium = c1Type(CampusOneDisplayFamily, FontWeight.SemiBold, 28, 36, -0.4),
        headlineSmall = c1Type(CampusOneDisplayFamily, FontWeight.SemiBold, 24, 32, -0.3),
        titleLarge = c1Type(CampusOneDisplayFamily, FontWeight.SemiBold, 22, 28, -0.2),
        titleMedium = c1Type(CampusOneTextFamily, FontWeight.SemiBold, 16, 24, -0.1),
        titleSmall = c1Type(CampusOneTextFamily, FontWeight.SemiBold, 14, 20, -0.1),
        bodyLarge = c1Type(CampusOneTextFamily, FontWeight.Normal, 16, 24, -0.1),
        bodyMedium = c1Type(CampusOneTextFamily, FontWeight.Normal, 14, 21, 0.0),
        bodySmall = c1Type(CampusOneTextFamily, FontWeight.Normal, 12, 18, 0.0),
        labelLarge = c1Type(CampusOneTextFamily, FontWeight.SemiBold, 14, 20, 0.0),
        labelMedium = c1Type(CampusOneTextFamily, FontWeight.Medium, 12, 16, 0.1),
        labelSmall = c1Type(CampusOneTextFamily, FontWeight.Medium, 11, 14, 0.2)
    )
}

// ============================================================================
// COMPONENT STYLE VARIANTS
// ============================================================================

enum class C1ButtonIntent {
    PRIMARY, TONAL, OUTLINED, DANGER, GHOST
}

enum class C1ButtonSize {
    SMALL, MEDIUM, LARGE
}

enum class C1CardVariant {
    FLAT, ELEVATED, BORDERED, FILLED
}

@Immutable
data class C1ZIndex(
    val base: Int = 0,
    val content: Int = 10,
    val dropdown: Int = 100,
    val sticky: Int = 200,
    val modal: Int = 1000,
    val toast: Int = 1100,
    val tooltip: Int = 1200
)

// ============================================================================
// ADAPTIVE HELPERS
// ============================================================================

/**
 * Adaptive content width based on window size.
 * Prevents content from stretching too wide on large screens.
 */
fun C1MaxContentWidth(windowSize: C1WindowSize): Dp = when (windowSize) {
    C1WindowSize.COMPACT -> Dp(Float.MAX_VALUE)
    C1WindowSize.MEDIUM -> Dp(Float.MAX_VALUE)
    C1WindowSize.LARGE -> 900.dp
    C1WindowSize.EXPANDED -> 1200.dp
}

/**
 * Adaptive horizontal padding for content that should be constrained.
 */
fun C1ContentPadding(windowSize: C1WindowSize): Dp = when (windowSize) {
    C1WindowSize.COMPACT -> 16.dp
    C1WindowSize.MEDIUM -> 20.dp
    C1WindowSize.LARGE -> 32.dp
    C1WindowSize.EXPANDED -> 48.dp
}