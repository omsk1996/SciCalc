package com.example.scicalc.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Palette {
    val BgTop = Color(0xFF161E2B)
    val BgBottom = Color(0xFF080B10)
    val Surface = Color(0xFF1D2633)
    val SurfaceHi = Color(0xFF283445)
    val FnKey = Color(0xFF141B26)
    val FnKeyHi = Color(0xFF1A2330)
    val TextPrimary = Color(0xFFF3F6FA)
    val TextSecondary = Color(0xFF8C99AB)
    val TextDim = Color(0xFF4A5566)
    val Accent = Color(0xFFFFB547)
    val AccentDeep = Color(0xFFFF7A45)
    val Teal = Color(0xFF6FE3D0)
    val Coral = Color(0xFFFF6B6B)
    val Sheet = Color(0xFF131A24)
}

@Composable
fun SciCalcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Accent,
            secondary = Palette.Teal,
            background = Palette.BgBottom,
            surface = Palette.Sheet,
            onSurface = Palette.TextPrimary,
            onBackground = Palette.TextPrimary,
            error = Palette.Coral,
        ),
        content = content,
    )
}
