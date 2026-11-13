package `in`.campusone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Semantic Attendance Health Colors
val SafeGreen = Color(0xFF1B8755)
val SafeGreenContainer = Color(0xFFD3F9E3)
val EdgeAmber = Color(0xFFD97706)
val EdgeAmberContainer = Color(0xFFFEF3C7)
val DangerRed = Color(0xFFDC2626)
val DangerRedContainer = Color(0xFFFEE2E2)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0E6B4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEEDF),
    onPrimaryContainer = Color(0xFF06281D),
    secondary = Color(0xFF3E5F90),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE7F7),
    onSecondaryContainer = Color(0xFF10233F),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF17181A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17181A),
    surfaceVariant = Color(0xFFEDEEF0),
    onSurfaceVariant = Color(0xFF6E7278),
    outline = Color(0xFFC7C9CD),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onError = Color.White,
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5BD6A8),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF0E4A36),
    onPrimaryContainer = Color(0xFFB9EBD6),
    secondary = Color(0xFFA8C8FF),
    onSecondary = Color(0xFF04305F),
    secondaryContainer = Color(0xFF244777),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF9E9EA5),
    outline = Color(0xFF48484A),
    error = Color(0xFFFF6B60),
    errorContainer = Color(0xFF3A0B08),
    onError = Color(0xFF2B0503),
    onErrorContainer = Color(0xFFFEE2E2)
)

// iOS-adjacent continuous-feeling corner radii
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

// SF-inspired type scale: heavy confident titles, calm body, tight tracking
val ExpressiveTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.05).sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )
)

@Composable
fun CampusOneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        content = content
    )
}
