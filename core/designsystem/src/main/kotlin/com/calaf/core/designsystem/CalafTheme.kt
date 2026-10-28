package com.calaf.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF155E4B), onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EDE1), onPrimaryContainer = Color(0xFF10392D),
    secondary = Color(0xFF755822), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E6C4), onSecondaryContainer = Color(0xFF30240D),
    background = Color(0xFFFAF8F2), onBackground = Color(0xFF192E27),
    surface = Color(0xFFFAF8F2), onSurface = Color(0xFF192E27),
    surfaceVariant = Color(0xFFE4E9DF), onSurfaceVariant = Color(0xFF46564C),
    outline = Color(0xFF707C71),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA4D5BE), onPrimary = Color(0xFF073B2C),
    primaryContainer = Color(0xFF215340), onPrimaryContainer = Color(0xFFD7EDE1),
    secondary = Color(0xFFE5C482), onSecondary = Color(0xFF40300E),
    secondaryContainer = Color(0xFF58451E), onSecondaryContainer = Color(0xFFF6E6C4),
    background = Color(0xFF101C17), onBackground = Color(0xFFE2E9DF),
    surface = Color(0xFF101C17), onSurface = Color(0xFFE2E9DF),
    surfaceVariant = Color(0xFF34443B), onSurfaceVariant = Color(0xFFC4CEC2),
    outline = Color(0xFF8E9C91),
)

object CalafSpacing {
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val section = 48.dp
}
object CalafDimensions {
    val touchTarget = 48.dp
    val icon = 24.dp
    val contentWidth = 560.dp
    val cardElevation = 2.dp
}
object CalafMotion { const val shortMillis = 150; const val mediumMillis = 300 }

private val CalafTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 40.sp, lineHeight = 46.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 30.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
)

@Composable
fun CalafTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CalafTypography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
