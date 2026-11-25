package com.pokerlab

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object PC {
    val Bg = Color(0xFF0B0709)
    val P = Color(0xFF150D11)
    val P2 = Color(0xFF1D1318)
    val Ln = Color(0xFF2E1B22)
    val Tx = Color(0xFFF1E9EC)
    val Mu = Color(0xFFA08F96)
    val Ac = Color(0xFFE11D48)
    val Gd = Color(0xFF3DDC97)
    val Bd = Color(0xFFFF6B7D)
    val Wn = Color(0xFFE3B04B)
    val Gr2 = Color(0xFF34C88A)
    val Felt1 = Color(0xFF4A1424)
    val Felt2 = Color(0xFF1D0B12)
    val Rail = Color(0xFF4A2531)
}

private val scheme = darkColorScheme(
    primary = PC.Ac,
    onPrimary = Color.White,
    secondary = PC.Gd,
    background = PC.Bg,
    onBackground = PC.Tx,
    surface = PC.P,
    onSurface = PC.Tx,
    surfaceVariant = PC.P2,
    onSurfaceVariant = PC.Mu,
    outline = PC.Ln,
    outlineVariant = PC.Ln,
    error = PC.Bd,
    surfaceContainer = PC.P,
    surfaceContainerHigh = PC.P2,
    surfaceContainerHighest = PC.P2,
    surfaceContainerLow = PC.P,
    surfaceContainerLowest = PC.Bg
)

@Composable
fun PokerLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
