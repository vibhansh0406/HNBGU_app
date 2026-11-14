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
 * Spacing is a 4dp-based scale. Elevation is flat-first: surfaces are
 * separated by tone and hairlines, shadows are reserved for truly floating
 * elements. All values are theme-independent primitives; color pairs live in
 * [ExtendedColors] so light and dark stay in sync.
 */
object C1Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

object C1Radius {
    val xs = RoundedCornerShape(10.dp)
    val sm = RoundedCornerShape(14.dp)
    val md = RoundedCornerShape(18.dp)
    val lg = RoundedCornerShape(24.dp)
    val xl = RoundedCornerShape(30.dp)
    val pill = RoundedCornerShape(50)
}

object C1Elevation {
    val flat = 0.dp
    val raised = 1.dp
    val floating = 4.dp
}

object C1IconSize {
    val inline = 18.dp
    val standard = 24.dp
    val large = 32.dp
}

object C1Motion {
    val fastMillis = 150
    val normalMillis = 250
    val slowMillis = 400
    val emphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standardEasing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)

    /** Subtle scale applied to pressable surfaces. */
    val pressScale = 0.97f
}

/** Hairline and emphasis border widths. */
object C1Border {
    val hairline = 1.dp
    val emphasis = 1.5.dp
    val focus = 2.dp
}

/** Fixed component heights so touch targets stay consistent across screens. */
object C1Height {
    val touch = 48.dp
    val button = 48.dp
    val field = 56.dp
    val chip = 32.dp
    val listRow = 56.dp
    val navBar = 80.dp
}

/** Interaction state opacities for pressed, selected and disabled surfaces. */
object C1State {
    const val pressedAlpha = 0.12f
    const val selectedAlpha = 0.10f
    const val disabledAlpha = 0.38f
    const val pressedIconScale = 0.92f
}

/** Minimum interactive touch target. */
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
    val hairline: Color
)

val LightExtendedColors = ExtendedColors(
    success = SafeGreen,
    onSuccess = Color.White,
    successContainer = SafeGreenContainer,
    onSuccessContainer = Color(0xFF0E3B27),
    warning = EdgeAmber,
    onWarning = Color.White,
    warningContainer = EdgeAmberContainer,
    onWarningContainer = Color(0xFF3F2405),
    info = Color(0xFF2C6E8A),
    onInfo = Color.White,
    infoContainer = Color(0xFFDCEAF0),
    onInfoContainer = Color(0xFF0E2E3B),
    hairline = Color(0xFFE6E6DC)
)

val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF8CC7A6),
    onSuccess = Color(0xFF0E2A1E),
    successContainer = Color(0xFF1E3B2E),
    onSuccessContainer = Color(0xFFC9E8D6),
    warning = Color(0xFFE5B36B),
    onWarning = Color(0xFF2E1F05),
    warningContainer = Color(0xFF3F2E14),
    onWarningContainer = Color(0xFFF7E3C1),
    info = Color(0xFF8FC3D9),
    onInfo = Color(0xFF0E2A33),
    infoContainer = Color(0xFF1C3A47),
    onInfoContainer = Color(0xFFD3EAF2),
    hairline = Color(0xFF2B3C32)
)
