package io.github.halilozel1903.autokit.sample.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colors close to the Android Auto dark UI. */
object CarColors {
    val Screen = Color(0xFF000000)
    val Rail = Color(0xFF121314)
    val Card = Color(0xFF202124)
    val CardHigh = Color(0xFF2D2E31)
    val Divider = Color(0xFF3C4043)
    val Text = Color(0xFFF1F3F4)
    val TextSecondary = Color(0xFFB0B4BA)
    val Accent = Color(0xFF8AB4F8)
    val OnAccent = Color(0xFF0B1F3A)
    val Page = Color(0xFF0B0F14)
}

@Composable
fun PreviewTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CarColors.Accent,
            onPrimary = CarColors.OnAccent,
            background = CarColors.Page,
            onBackground = CarColors.Text,
            surface = CarColors.Card,
            onSurface = CarColors.Text,
            onSurfaceVariant = CarColors.TextSecondary,
            outlineVariant = CarColors.Divider,
        ),
        content = content,
    )
}
