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
    primary = Color(0xFF16A36A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF6E9),
    onPrimaryContainer = Color(0xFF075C3B),
    inversePrimary = Color(0xFF69D9A5),
    secondary = Color(0xFF3E6B58),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEBE3),
    onSecondaryContainer = Color(0xFF173B2D),
    tertiary = Color(0xFFF08C45),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE5D2),
    onTertiaryContainer = Color(0xFF6A2C00),
    background = Color(0xFFF4F7F5),
    onBackground = Color(0xFF18201C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18201C),
    surfaceVariant = Color(0xFFEDF3EF),
    onSurfaceVariant = Color(0xFF68756E),
    outline = Color(0xFFCAD5CF),
    outlineVariant = Color(0xFFE3EAE6),
    error = Color(0xFFE85D55),
    onError = Color.White,
    errorContainer = Color(0xFFFFE2DF),
    onErrorContainer = Color(0xFF6E1512),
    scrim = Color(0x99000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6DDBA8),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF075538),
    onPrimaryContainer = Color(0xFFB7F5D7),
    inversePrimary = Color(0xFF16A36A),
    secondary = Color(0xFFB6CEBF),
    onSecondary = Color(0xFF20372C),
    secondaryContainer = Color(0xFF354D40),
    onSecondaryContainer = Color(0xFFD2E9DB),
    tertiary = Color(0xFFFFB77D),
    onTertiary = Color(0xFF542B00),
    tertiaryContainer = Color(0xFF783F00),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF101613),
    onBackground = Color(0xFFE1E8E4),
    surface = Color(0xFF171E1A),
    onSurface = Color(0xFFE1E8E4),
    surfaceVariant = Color(0xFF26312B),
    onSurfaceVariant = Color(0xFFB8C5BE),
    outline = Color(0xFF46534C),
    outlineVariant = Color(0xFF2D3933),
    error = Color(0xFFFF7B73),
    onError = Color(0xFF410003),
    errorContainer = Color(0xFF6E1512),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xCC000000)
)

private val LedgerTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 36.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 23.sp,
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
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** 支出、收入、退款等金额语义色。 */
object AmountColors {
    val Expense = Color(0xFFE85D55)
    val Income = Color(0xFF16A36A)
    val Refund = Color(0xFF4E7DF2)
    val Transfer = Color(0xFF8B949E)
    val Warning = Color(0xFFF09A3E)
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