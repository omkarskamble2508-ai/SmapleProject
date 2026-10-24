package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = SovereignNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E6F2),
    onPrimaryContainer = SovereignNavyDark,
    secondary = CivicTeal,
    onSecondary = Color.White,
    secondaryContainer = CivicTealContainer,
    onSecondaryContainer = OnCivicTealContainer,
    tertiary = EmblemSaffron,
    onTertiary = Color.White,
    tertiaryContainer = EmblemSaffronLight,
    onTertiaryContainer = Color(0xFF451A03),
    error = ContaminatedRed,
    onError = Color.White,
    errorContainer = ContaminatedRedBg,
    onErrorContainer = Color(0xFF450A0A),
    background = PortalBackgroundLight,
    onBackground = InkPrimaryLight,
    surface = PortalSurfaceLight,
    onSurface = InkPrimaryLight,
    surfaceVariant = PortalSurfaceVariantLight,
    onSurfaceVariant = InkSecondaryLight,
    outline = PortalOutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = SovereignNavyDark,
    primaryContainer = SovereignNavy,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = CivicTealContainer,
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = EmblemSaffronLight,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = ContaminatedRedBg,
    background = PortalBackgroundDark,
    onBackground = InkPrimaryDark,
    surface = PortalSurfaceDark,
    onSurface = InkPrimaryDark,
    surfaceVariant = PortalSurfaceVariantDark,
    onSurfaceVariant = InkSecondaryDark,
    outline = PortalOutlineDark
)

val PortalShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun JalSurakshaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PortalShapes,
        content = content
    )
}
