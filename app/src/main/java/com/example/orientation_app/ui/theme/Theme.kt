package com.example.orientation_app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = DarkBackground,
    primaryContainer = TealDark,
    secondary = TealDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextGray,
    outline = DarkCardBorder,
    outlineVariant = DarkCardBorder
)

/**
 * Root theme wrapper for Unicompass.
 * Forces RTL layout direction and applies the dark colour scheme.
 */
@Composable
fun UnicompassTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
