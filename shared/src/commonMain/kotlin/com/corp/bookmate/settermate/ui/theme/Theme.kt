package com.corp.bookmate.settermate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary          = PrimaryGreen,
    onPrimary        = OnPrimaryGreen,
    background       = DarkBackground,
    onBackground     = OnBackground,
    surface          = DarkSurface,
    onSurface        = OnBackground,
    surfaceVariant   = DarkSurfaceVariant,
    onSurfaceVariant = MutedGreen,
    outline          = OutlineGreen,
    outlineVariant   = OutlineGreen,
)

@Composable
fun SetterMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content,
    )
}
