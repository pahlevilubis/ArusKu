package com.arusku.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Mint = Color(0xFF3DDC97)
val Gold = Color(0xFFFFC857)
val Rose = Color(0xFFFF6B6B)
val Sky = Color(0xFF6CB4FF)

private val ArusColors = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF00281A),
    primaryContainer = Color(0xFF0E3A2A),
    onPrimaryContainer = Color(0xFFB8F5D9),
    secondary = Gold,
    onSecondary = Color(0xFF2B1D00),
    secondaryContainer = Color(0xFF3A2C0A),
    onSecondaryContainer = Color(0xFFFFE6B0),
    tertiary = Sky,
    onTertiary = Color(0xFF00213F),
    error = Rose,
    onError = Color(0xFF2B0606),
    errorContainer = Color(0xFF3D1414),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0C0F0E),
    onBackground = Color(0xFFE8EDEA),
    surface = Color(0xFF0C0F0E),
    onSurface = Color(0xFFE8EDEA),
    surfaceVariant = Color(0xFF1F2624),
    onSurfaceVariant = Color(0xFF9FAAA5),
    surfaceContainerLowest = Color(0xFF080A09),
    surfaceContainerLow = Color(0xFF111614),
    surfaceContainer = Color(0xFF151B19),
    surfaceContainerHigh = Color(0xFF1B2220),
    surfaceContainerHighest = Color(0xFF232B28),
    outline = Color(0xFF3E4844),
    outlineVariant = Color(0xFF2A322F),
)

@Composable
fun ArusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ArusColors, content = content)
}

private val CatPalette = listOf(
    Gold, Mint, Sky,
    Color(0xFFB98CFF), Color(0xFFFF8FA3), Color(0xFF7FD6E0),
    Color(0xFFFFA45B), Color(0xFFA6E06C),
)

/** Warna tetap per kategori, supaya mudah dikenali di semua layar. */
fun catColor(name: String): Color {
    val n = name.lowercase()
    return when {
        n.startsWith("makan") -> Gold
        n.startsWith("transport") -> Sky
        n.startsWith("belanja") -> Color(0xFFFF8FA3)
        n.startsWith("tagihan") -> Color(0xFFB98CFF)
        n.startsWith("hiburan") -> Color(0xFFFFA45B)
        n.startsWith("kesehatan") -> Color(0xFF7FD6E0)
        n.startsWith("sosial") -> Color(0xFFA6E06C)
        else -> CatPalette[Math.floorMod(n.hashCode(), CatPalette.size)]
    }
}
