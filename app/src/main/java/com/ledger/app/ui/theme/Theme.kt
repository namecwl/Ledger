package com.ledger.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Light = lightColorScheme(
    primary = Color(0xFF00A870),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F5E6),
    onPrimaryContainer = Color(0xFF003820),
    secondary = Color(0xFF5B8DEF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE7FF),
    onSecondaryContainer = Color(0xFF0D2A5E),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF5F6368),
    outline = Color(0xFFD8DBE0),
    outlineVariant = Color(0xFFEBEDF0),
    error = Color(0xFFE5484D),
    onError = Color.White
)

private val Dark = darkColorScheme(
    primary = Color(0xFF4CD99A),
    onPrimary = Color(0xFF00381F),
    primaryContainer = Color(0xFF00522E),
    onPrimaryContainer = Color(0xFFB4F4D4),
    secondary = Color(0xFF8FB2FF),
    background = Color(0xFF111315),
    onBackground = Color(0xFFE2E4E8),
    surface = Color(0xFF1A1D21),
    onSurface = Color(0xFFE2E4E8),
    surfaceVariant = Color(0xFF262A2E),
    onSurfaceVariant = Color(0xFFB0B5BD),
    outline = Color(0xFF3A3F45),
    outlineVariant = Color(0xFF2A2E33),
    error = Color(0xFFFF6B6E)
)

/** 支出/收入/结余的语义色，供全局使用 */
object AmountColors {
    val Expense = Color(0xFFE5484D)
    val Income = Color(0xFF00A870)
    val Refund = Color(0xFF5B8DEF)
    val Transfer = Color(0xFF8E8E93)
}

@Composable
fun LedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content
    )
}