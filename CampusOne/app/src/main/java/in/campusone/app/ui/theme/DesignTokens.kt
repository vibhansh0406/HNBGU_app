package `in`.campusone.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * CampusOne design tokens — "Forest & Ivory".
 *
 * A premium, minimal, sophisticated visual language inspired by nature.
 * Deep forest greens, warm ivory neutrals, and refined accent tones.
 * Spacing is a 4dp-based scale with 8dp rhythm. Elevation is flat-first:
 * surfaces are separated by tone and hairlines, shadows reserved for truly
 * floating elements. All values are theme-independent primitives; color pairs
 * live in [ExtendedColors] so light and dark stay in sync.
 */
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
}

object C1Radius {
    val none = RoundedCornerShape(0.dp)
    val xs = RoundedCornerShape(8.dp)
    val sm = RoundedCornerShape(12.dp)
    val md = RoundedCornerShape(16.dp)
    val lg = RoundedCornerShape(20.dp)
    val xl = RoundedCornerShape(28.dp)
    val xxl = RoundedCornerShape(36.dp)
    val pill = RoundedCornerShape(50)
    val circle = RoundedCornerShape(50)
}

object C1Elevation {
    val flat = 0.dp
    val level1 = 1.dp
    val level2 = 4.dp
    val level3 = 8.dp
    val level4 = 16.dp
    val floating = 24.dp
}

object C1IconSize {
    val inline = 16.dp
    val small = 20.dp
    val standard = 24.dp
    val large = 32.dp
    val xl = 40.dp
    val xxl = 48.dp
}

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

    /** Subtle scale applied to pressable surfaces. */
    val pressScale = 0.98f
    val pressScaleStrong = 0.95f
}

object C1Border {
    val hairline = 0.5.dp
    val thin = 1.dp
    val emphasis = 1.5.dp
    val focus = 2.dp
}

object C1Height {
    val touch = 48.dp
    val button = 48.dp
    val buttonLarge = 56.dp
    val field = 56.dp
    val chip = 32.dp
    val listRow = 56.dp
    val listRowCompact = 48.dp
    val navBar = 80.dp
    val topBar = 64.dp
}

object C1State {
    const val pressedAlpha = 0.08f
    const val selectedAlpha = 0.10f
    const val disabledAlpha = 0.38f
    const val hoverAlpha = 0.04f
    const val focusAlpha = 0.12f
    const val pressedIconScale = 0.92f
}

val C1TouchTarget: Dp = 48.dp

/**
 * Theme-aware semantic colors that Material3's ColorScheme does not carry.
 * Every entry is a full contrast-safe pair per mode.
 */
@Immutable
data class ExtendedColors(
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
    val surfaceTint: Color,
)

/** Forest & Ivory — Light Extended Colors */
val LightExtendedColors = ExtendedColors(
    success = Color(0xFF1B7A4A),
    onSuccess = Color.White,
    successContainer = Color(0xFFD6F0E0),
    onSuccessContainer = Color(0xFF0A3D20),
    warning = Color(0xFFB86B00),
    onWarning = Color.White,
    warningContainer = Color(0xFFFFF0D9),
    onWarningContainer = Color(0xFF3D2600),
    info = Color(0xFF006E8A),
    onInfo = Color.White,
    infoContainer = Color(0xFFD6F0F7),
    onInfoContainer = Color(0xFF002E3A),
    hairline = Color(0xFFE8E6E0),
    divider = Color(0xFFE0DDD6),
    scrim = Color(0x99000000),
    overlay = Color(0xCC000000),
    surfaceTint = Color(0xFF1B4D3A),
)

/** Forest & Ivory — Dark Extended Colors */
val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF6FD69A),
    onSuccess = Color(0xFF052010),
    successContainer = Color(0xFF1A3D28),
    onSuccessContainer = Color(0xFFB8E8CE),
    warning = Color(0xFFFFB84D),
    onWarning = Color(0xFF2D1A00),
    warningContainer = Color(0xFF3D2A0A),
    onWarningContainer = Color(0xFFFFE8C0),
    info = Color(0xFF6FD6E8),
    onInfo = Color(0xFF002028),
    infoContainer = Color(0xFF1A3A40),
    onInfoContainer = Color(0xFFB8E8F0),
    hairline = Color(0xFF2D2B28),
    divider = Color(0xFF3A3733),
    scrim = Color(0xCC000000),
    overlay = Color(0xDD000000),
    surfaceTint = Color(0xFF8FD6A8),
)

/** Component-specific color roles for consistent theming */
@Immutable
data class ComponentColors(
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
    val badgeContent: Color,
)

val LightComponentColors = ComponentColors(
    cardBackground = Color.White,
    cardBorder = Color(0xFFE8E6E0),
    cardHover = Color(0xFFF5F3ED),
    cardPressed = Color(0xFFEDEBE4),
    surfaceElevated = Color.White,
    surfaceElevatedBorder = Color(0xFFE0DDD6),
    inputBackground = Color.White,
    inputBorder = Color(0xFFD8D6CE),
    inputBorderFocused = Color(0xFF1B4D3A),
    inputBorderError = Color(0xFFB82E1A),
    chipBackground = Color(0xFFF0EEE8),
    chipBackgroundSelected = Color(0xFF1B4D3A),
    chipBorder = Color(0xFFE0DDD6),
    chipBorderSelected = Color(0xFF1B4D3A),
    dividerColor = Color(0xFFE8E6E0),
    skeletonBase = Color(0xFFF0EEE8),
    skeletonHighlight = Color(0xFFFAF9F6),
    badgeBackground = Color(0xFF1B4D3A),
    badgeContent = Color.White,
)

val DarkComponentColors = ComponentColors(
    cardBackground = Color(0xFF1A1C1A),
    cardBorder = Color(0xFF2D2B28),
    cardHover = Color(0xFF222421),
    cardPressed = Color(0xFF2A2C29),
    surfaceElevated = Color(0xFF1E201D),
    surfaceElevatedBorder = Color(0xFF3A3733),
    inputBackground = Color(0xFF1A1C1A),
    inputBorder = Color(0xFF3A3733),
    inputBorderFocused = Color(0xFF6FD69A),
    inputBorderError = Color(0xFFFF6B5A),
    chipBackground = Color(0xFF2A2C29),
    chipBackgroundSelected = Color(0xFF6FD69A),
    chipBorder = Color(0xFF3A3733),
    chipBorderSelected = Color(0xFF6FD69A),
    dividerColor = Color(0xFF2D2B28),
    skeletonBase = Color(0xFF2A2C29),
    skeletonHighlight = Color(0xFF323431),
    badgeBackground = Color(0xFF8FD6A8),
    badgeContent = Color(0xFF052010),
)

/** Z-index layers for consistent layering */
object C1ZIndex {
    val base = 0
    val content = 10
    val dropdown = 100
    val sticky = 200
    val modal = 1000
    val toast = 1100
    val tooltip = 1200
}

/** Breakpoints for adaptive layouts */
object C1Breakpoint {
    val compact = 600.dp
    val medium = 840.dp
    val expanded = 1200.dp
    val large = 1600.dp
}

/** Semantic color roles for data visualization and status */
@Immutable
data class SemanticColors(
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
)

val LightSemanticColors = SemanticColors(
    primary = Color(0xFF1B4D3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F0E0),
    onPrimaryContainer = Color(0xFF0A2D1E),
    secondary = Color(0xFF3D5A4A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0EDE4),
    onSecondaryContainer = Color(0xFF1A3326),
    tertiary = Color(0xFF7A5C4A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0E4D9),
    onTertiaryContainer = Color(0xFF2E1E12),
    error = Color(0xFFB82E1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFE8E4),
    onErrorContainer = Color(0xFF4A0E08),
    background = Color(0xFFFAF9F6),
    onBackground = Color(0xFF1A1C1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFF0EEE8),
    onSurfaceVariant = Color(0xFF4A4844),
    outline = Color(0xFFD8D6CE),
    outlineVariant = Color(0xFFE8E6E0),
    inverseSurface = Color(0xFF2A2C29),
    inverseOnSurface = Color(0xFFF5F3ED),
    inversePrimary = Color(0xFF8FD6A8),
    surfaceTint = Color(0xFF1B4D3A),
)

val DarkSemanticColors = SemanticColors(
    primary = Color(0xFF8FD6A8),
    onPrimary = Color(0xFF052010),
    primaryContainer = Color(0xFF1A3D28),
    onPrimaryContainer = Color(0xFFC8E8D8),
    secondary = Color(0xFFB8D6C4),
    onSecondary = Color(0xFF0A2015),
    secondaryContainer = Color(0xFF2A3D32),
    onSecondaryContainer = Color(0xFFD8E8DC),
    tertiary = Color(0xFFD6B89A),
    onTertiary = Color(0xFF2E1E12),
    tertiaryContainer = Color(0xFF3D2E22),
    onTertiaryContainer = Color(0xFFF0E4D9),
    error = Color(0xFFFF6B5A),
    onError = Color(0xFF3B0A05),
    errorContainer = Color(0xFF4A1510),
    onErrorContainer = Color(0xFFFFE8E4),
    background = Color(0xFF121412),
    onBackground = Color(0xFFF5F3ED),
    surface = Color(0xFF1A1C1A),
    onSurface = Color(0xFFF5F3ED),
    surfaceVariant = Color(0xFF2A2C29),
    onSurfaceVariant = Color(0xFFB8B6B0),
    outline = Color(0xFF3A3733),
    outlineVariant = Color(0xFF2D2B28),
    inverseSurface = Color(0xFFF5F3ED),
    inverseOnSurface = Color(0xFF1A1C1A),
    inversePrimary = Color(0xFF1B4D3A),
    surfaceTint = Color(0xFF8FD6A8),
)