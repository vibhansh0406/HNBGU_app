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

// Semantic attendance health colors (forest-harmonized)
val SafeGreen = Color(0xFF2E7D5B)
val SafeGreenContainer = Color(0xFFDDEBE2)
val EdgeAmber = Color(0xFFB45309)
val EdgeAmberContainer = Color(0xFFF7ECD9)
val DangerRed = Color(0xFFB42318)
val DangerRedContainer = Color(0xFFFBE5E2)

// Forest & Ivory — light
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF183D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7DF),
    onPrimaryContainer = Color(0xFF102D25),
    secondary = Color(0xFF315C4B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2EAE4),
    onSecondaryContainer = Color(0xFF102D25),
    tertiary = Color(0xFF7A5C50),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0E4DE),
    onTertiaryContainer = Color(0xFF2E1B14),
    background = Color(0xFFF7F6F0),
    onBackground = Color(0xFF202923),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202923),
    surfaceVariant = Color(0xFFEFEEE6),
    onSurfaceVariant = Color(0xFF5E665E),
    outline = Color(0xFFE6E6DC),
    outlineVariant = Color(0xFFD8D8CC),
    error = Color(0xFFB42318),
    errorContainer = Color(0xFFFBE5E2),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF4A0E08)
)

// Forest & Ivory — dark
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF83A995),
    onPrimary = Color(0xFF102D25),
    primaryContainer = Color(0xFF24443A),
    onPrimaryContainer = Color(0xFFC9DCD1),
    secondary = Color(0xFF9DB8AA),
    onSecondary = Color(0xFF102D25),
    secondaryContainer = Color(0xFF2A463B),
    onSecondaryContainer = Color(0xFFD5E4DB),
    tertiary = Color(0xFFD3B8AC),
    onTertiary = Color(0xFF2E1B14),
    tertiaryContainer = Color(0xFF4A352C),
    onTertiaryContainer = Color(0xFFF0E4DE),
    background = Color(0xFF101B17),
    onBackground = Color(0xFFF1F2EA),
    surface = Color(0xFF182720),
    onSurface = Color(0xFFF1F2EA),
    surfaceVariant = Color(0xFF20332A),
    onSurfaceVariant = Color(0xFFA6B0A7),
    outline = Color(0xFF2B3C32),
    outlineVariant = Color(0xFF24352C),
    error = Color(0xFFFF8A7A),
    errorContainer = Color(0xFF4A1510),
    onError = Color(0xFF3B0A05),
    onErrorContainer = Color(0xFFFBE5E2)
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
