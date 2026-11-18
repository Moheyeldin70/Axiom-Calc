package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemePreference {
    SYSTEM,
    DARK,
    LIGHT
}

private val AxiomDarkColorScheme = darkColorScheme(
    primary = EngineeringOrange,
    onPrimary = Color.White,
    primaryContainer = EngineeringOrangeContainerDark,
    onPrimaryContainer = Color(0xFFFFB496),
    secondary = PrecisionTeal,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = PrecisionTealContainerDark,
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = ElectricCyan,
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = ElectricCyanContainerDark,
    onTertiaryContainer = Color(0xFFBAE6FD),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceMuted,
    surfaceContainerLowest = Color(0xFF080A0D),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceElevated,
    surfaceContainerHighest = Color(0xFF2B3444),
    outline = DarkOutline,
    outlineVariant = DarkOutlineSubtle,
    error = ErrorCrimson,
    onError = Color.White
)

private val AxiomLightColorScheme = lightColorScheme(
    primary = EngineeringOrangeDark,
    onPrimary = Color.White,
    primaryContainer = EngineeringOrangeContainerLight,
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = PrecisionTealDark,
    onSecondary = Color.White,
    secondaryContainer = PrecisionTealContainerLight,
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = ElectricCyanDark,
    onTertiary = Color.White,
    tertiaryContainer = ElectricCyanContainerLight,
    onTertiaryContainer = Color(0xFF0C4A6E),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceMuted,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = LightSurfaceElevated,
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = Color(0xFFDDE3EC),
    surfaceContainerHighest = Color(0xFFD0D8E4),
    outline = LightOutline,
    outlineVariant = LightOutlineSubtle,
    error = ErrorCrimsonDark,
    onError = Color.White
)

val AxiomShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AxiomCalcTheme(
    themePreference: ThemePreference = ThemePreference.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }

    val colorScheme = if (darkTheme) AxiomDarkColorScheme else AxiomLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AxiomTypography,
        shapes = AxiomShapes,
        content = content
    )
}
