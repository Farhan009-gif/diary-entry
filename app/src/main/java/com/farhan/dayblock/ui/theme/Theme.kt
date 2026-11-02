package com.farhan.dayblock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val GrassGreen = Color(0xFF4E9B47)
val GrassLight = Color(0xFF7CB95C)
val DirtBrown = Color(0xFF8A5A33)
val StoneGrey = Color(0xFF6F6F6F)
val CreamPaper = Color(0xFFFFF8EC)
val WarmInk = Color(0xFF2B2620)
val DiamondBlue = Color(0xFF3FA7C4)
val GoldYellow = Color(0xFFF2B33D)

private val LightColors = lightColorScheme(
    primary = GrassGreen,
    onPrimary = Color.White,
    secondary = DirtBrown,
    onSecondary = Color.White,
    tertiary = DiamondBlue,
    background = CreamPaper,
    onBackground = WarmInk,
    surface = Color(0xFFFFFDF6),
    onSurface = WarmInk,
    surfaceVariant = Color(0xFFF0E7D3),
    onSurfaceVariant = Color(0xFF5A5148)
)

private val DarkColors = darkColorScheme(
    primary = GrassLight,
    onPrimary = Color(0xFF17300F),
    secondary = Color(0xFFD2A679),
    onSecondary = Color(0xFF3A2410),
    tertiary = DiamondBlue,
    background = Color(0xFF191713),
    onBackground = Color(0xFFF3ECDD),
    surface = Color(0xFF232019),
    onSurface = Color(0xFFF3ECDD),
    surfaceVariant = Color(0xFF353126),
    onSurfaceVariant = Color(0xFFCFC5AE)
)

/** Slightly squared corners everywhere: the blocky, pixel-built feel. */
private val BlockShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp)
)

@Composable
fun DayblockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = BlockShapes,
        content = content
    )
}
