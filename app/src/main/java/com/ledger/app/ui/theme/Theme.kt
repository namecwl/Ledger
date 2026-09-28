package com.ledger.app.ui.theme

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F80ED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7F0FF),
    onPrimaryContainer = Color(0xFF123F8C),
    inversePrimary = Color(0xFF8BB8FF),
    secondary = Color(0xFF18A76D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F6ED),
    onSecondaryContainer = Color(0xFF075A38),
    tertiary = Color(0xFFF29A45),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEBD8),
    onTertiaryContainer = Color(0xFF6A3200),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF171A1F),
    surface = Color.White,
    onSurface = Color(0xFF171A1F),
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = Color(0xFF858B95),
    outline = Color(0xFFE1E4E8),
    outlineVariant = Color(0xFFEFF1F3),
    error = Color(0xFFF25B57),
    onError = Color.White,
    errorContainer = Color(0xFFFFE8E7),
    onErrorContainer = Color(0xFF711B18),
    scrim = Color(0x99000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BB8FF),
    onPrimary = Color(0xFF003063),
    primaryContainer = Color(0xFF164A88),
    onPrimaryContainer = Color(0xFFD8E7FF),
    inversePrimary = Color(0xFF2F80ED),
    secondary = Color(0xFF72D7A9),
    onSecondary = Color(0xFF003823),
    secondaryContainer = Color(0xFF075538),
    onSecondaryContainer = Color(0xFFB7F5D7),
    tertiary = Color(0xFFFFB77D),
    onTertiary = Color(0xFF542B00),
    tertiaryContainer = Color(0xFF783F00),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF101214),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF191C1F),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF25292E),
    onSurfaceVariant = Color(0xFFB6BBC3),
    outline = Color(0xFF3D4249),
    outlineVariant = Color(0xFF2A2E33),
    error = Color(0xFFFF7B76),
    onError = Color(0xFF4A0503),
    errorContainer = Color(0xFF6E1512),
    onErrorContainer = Color(0xFFFFDAD7),
    scrim = Color(0xCC000000)
)

private val LedgerTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontSize = 29.sp,
        lineHeight = 35.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontSize = 25.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontSize = 21.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium
    )
)

private val LedgerShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp)
)

/** 支出、收入、退款等金额语义色。 */
object AmountColors {
    val Expense = Color(0xFFF25B57)
    val Income = Color(0xFF18A76D)
    val Refund = Color(0xFF3478F6)
    val Transfer = Color(0xFF8A909B)
    val Warning = Color(0xFFF29A45)
}

@Composable
fun LedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = LedgerTypography,
        shapes = LedgerShapes,
        content = content
    )
}
