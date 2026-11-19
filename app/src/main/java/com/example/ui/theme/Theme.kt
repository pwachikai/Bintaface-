package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AuroraLightScheme = lightColorScheme(
    primary = AuroraPrimary,
    onPrimary = AuroraOnPrimary,
    primaryContainer = AuroraPrimaryContainer,
    onPrimaryContainer = AuroraOnPrimaryContainer,
    secondary = AuroraSecondary,
    onSecondary = AuroraOnSecondary,
    secondaryContainer = AuroraSecondaryContainer,
    onSecondaryContainer = AuroraOnSecondaryContainer,
    tertiary = AuroraTertiary,
    onTertiary = AuroraOnTertiary,
    tertiaryContainer = AuroraTertiaryContainer,
    onTertiaryContainer = AuroraOnTertiaryContainer,
    background = AuroraLightBackground,
    onBackground = AuroraLightOnSurface,
    surface = AuroraLightSurface,
    onSurface = AuroraLightOnSurface,
    surfaceVariant = AuroraLightSurfaceContainer,
    onSurfaceVariant = AuroraLightOnSurfaceVariant,
    surfaceContainerLow = AuroraLightSurfaceContainer,
    surfaceContainer = AuroraLightSurfaceContainer,
    surfaceContainerHigh = AuroraLightSurfaceContainerHigh,
    outline = AuroraLightOutline,
    outlineVariant = AuroraLightSurfaceContainerHigh
)

private val AuroraDarkScheme = darkColorScheme(
    primary = AuroraDarkPrimary,
    onPrimary = AuroraDarkOnPrimary,
    primaryContainer = AuroraDarkPrimaryContainer,
    onPrimaryContainer = AuroraDarkOnPrimaryContainer,
    secondary = AuroraDarkSecondary,
    onSecondary = AuroraDarkOnSecondary,
    secondaryContainer = AuroraDarkSecondaryContainer,
    onSecondaryContainer = AuroraDarkOnSecondaryContainer,
    tertiary = AuroraDarkTertiary,
    onTertiary = AuroraDarkOnTertiary,
    tertiaryContainer = AuroraDarkTertiaryContainer,
    onTertiaryContainer = AuroraDarkOnTertiaryContainer,
    background = AuroraDarkBackground,
    onBackground = AuroraDarkOnSurface,
    surface = AuroraDarkSurface,
    onSurface = AuroraDarkOnSurface,
    surfaceVariant = AuroraDarkSurfaceContainer,
    onSurfaceVariant = AuroraDarkOnSurface.copy(alpha = 0.74f),
    surfaceContainerLow = AuroraDarkSurfaceContainer,
    surfaceContainer = AuroraDarkSurfaceContainer,
    surfaceContainerHigh = AuroraDarkSurfaceContainerHigh,
    outline = AuroraDarkOutline,
    outlineVariant = AuroraDarkSurfaceContainerHigh
)

private val SunsetLightScheme = lightColorScheme(
    primary = SunsetPrimary,
    onPrimary = SunsetOnPrimary,
    primaryContainer = SunsetPrimaryContainer,
    onPrimaryContainer = SunsetOnPrimaryContainer,
    secondary = SunsetSecondary,
    onSecondary = SunsetOnSecondary,
    secondaryContainer = SunsetSecondaryContainer,
    onSecondaryContainer = SunsetOnSecondaryContainer,
    tertiary = SunsetTertiary,
    onTertiary = SunsetOnTertiary,
    tertiaryContainer = SunsetTertiaryContainer,
    onTertiaryContainer = SunsetOnTertiaryContainer,
    background = SunsetLightBackground,
    onBackground = SunsetLightOnSurface,
    surface = SunsetLightSurface,
    onSurface = SunsetLightOnSurface,
    surfaceVariant = SunsetLightSurfaceContainer,
    onSurfaceVariant = SunsetLightOnSurfaceVariant,
    surfaceContainerLow = SunsetLightSurfaceContainer,
    surfaceContainer = SunsetLightSurfaceContainer,
    surfaceContainerHigh = SunsetLightSurfaceContainerHigh,
    outline = SunsetLightOutline,
    outlineVariant = SunsetLightSurfaceContainerHigh
)

private val SunsetDarkScheme = darkColorScheme(
    primary = SunsetDarkPrimary,
    onPrimary = SunsetDarkOnPrimary,
    primaryContainer = SunsetDarkPrimaryContainer,
    onPrimaryContainer = SunsetDarkOnPrimaryContainer,
    secondary = SunsetDarkSecondary,
    onSecondary = SunsetDarkOnSecondary,
    secondaryContainer = SunsetDarkSecondaryContainer,
    onSecondaryContainer = SunsetDarkOnSecondaryContainer,
    tertiary = SunsetDarkTertiary,
    onTertiary = SunsetDarkOnTertiary,
    tertiaryContainer = SunsetDarkTertiaryContainer,
    onTertiaryContainer = SunsetDarkOnTertiaryContainer,
    background = SunsetDarkBackground,
    onBackground = SunsetDarkOnSurface,
    surface = SunsetDarkSurface,
    onSurface = SunsetDarkOnSurface,
    surfaceVariant = SunsetDarkSurfaceContainer,
    onSurfaceVariant = SunsetDarkOnSurface.copy(alpha = 0.74f),
    surfaceContainerLow = SunsetDarkSurfaceContainer,
    surfaceContainer = SunsetDarkSurfaceContainer,
    surfaceContainerHigh = SunsetDarkSurfaceContainerHigh,
    outline = SunsetDarkOutline,
    outlineVariant = SunsetDarkSurfaceContainerHigh
)

private val OceanLightScheme = lightColorScheme(
    primary = OceanPrimary,
    onPrimary = OceanOnPrimary,
    primaryContainer = OceanPrimaryContainer,
    onPrimaryContainer = OceanOnPrimaryContainer,
    secondary = OceanSecondary,
    onSecondary = OceanOnSecondary,
    secondaryContainer = OceanSecondaryContainer,
    onSecondaryContainer = OceanOnSecondaryContainer,
    tertiary = OceanTertiary,
    onTertiary = OceanOnTertiary,
    tertiaryContainer = OceanTertiaryContainer,
    onTertiaryContainer = OceanOnTertiaryContainer,
    background = OceanLightBackground,
    onBackground = OceanLightOnSurface,
    surface = OceanLightSurface,
    onSurface = OceanLightOnSurface,
    surfaceVariant = OceanLightSurfaceContainer,
    onSurfaceVariant = OceanLightOnSurfaceVariant,
    surfaceContainerLow = OceanLightSurfaceContainer,
    surfaceContainer = OceanLightSurfaceContainer,
    surfaceContainerHigh = OceanLightSurfaceContainerHigh,
    outline = OceanLightOutline,
    outlineVariant = OceanLightSurfaceContainerHigh
)

private val OceanDarkScheme = darkColorScheme(
    primary = OceanDarkPrimary,
    onPrimary = OceanDarkOnPrimary,
    primaryContainer = OceanDarkPrimaryContainer,
    onPrimaryContainer = OceanDarkOnPrimaryContainer,
    secondary = OceanDarkSecondary,
    onSecondary = OceanDarkOnSecondary,
    secondaryContainer = OceanDarkSecondaryContainer,
    onSecondaryContainer = OceanDarkOnSecondaryContainer,
    tertiary = OceanDarkTertiary,
    onTertiary = OceanDarkOnTertiary,
    tertiaryContainer = OceanDarkTertiaryContainer,
    onTertiaryContainer = OceanDarkOnTertiaryContainer,
    background = OceanDarkBackground,
    onBackground = OceanDarkOnSurface,
    surface = OceanDarkSurface,
    onSurface = OceanDarkOnSurface,
    surfaceVariant = OceanDarkSurfaceContainer,
    onSurfaceVariant = OceanDarkOnSurface.copy(alpha = 0.74f),
    surfaceContainerLow = OceanDarkSurfaceContainer,
    surfaceContainer = OceanDarkSurfaceContainer,
    surfaceContainerHigh = OceanDarkSurfaceContainerHigh,
    outline = OceanDarkOutline,
    outlineVariant = OceanDarkSurfaceContainerHigh
)

private val EmeraldLightScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = EmeraldSecondary,
    onSecondary = EmeraldOnSecondary,
    secondaryContainer = EmeraldSecondaryContainer,
    onSecondaryContainer = EmeraldOnSecondaryContainer,
    tertiary = EmeraldTertiary,
    onTertiary = EmeraldOnTertiary,
    tertiaryContainer = EmeraldTertiaryContainer,
    onTertiaryContainer = EmeraldOnTertiaryContainer,
    background = EmeraldLightBackground,
    onBackground = EmeraldLightOnSurface,
    surface = EmeraldLightSurface,
    onSurface = EmeraldLightOnSurface,
    surfaceVariant = EmeraldLightSurfaceContainer,
    onSurfaceVariant = EmeraldLightOnSurfaceVariant,
    surfaceContainerLow = EmeraldLightSurfaceContainer,
    surfaceContainer = EmeraldLightSurfaceContainer,
    surfaceContainerHigh = EmeraldLightSurfaceContainerHigh,
    outline = EmeraldLightOutline,
    outlineVariant = EmeraldLightSurfaceContainerHigh
)

private val EmeraldDarkScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = EmeraldDarkOnPrimary,
    primaryContainer = EmeraldDarkPrimaryContainer,
    onPrimaryContainer = EmeraldDarkOnPrimaryContainer,
    secondary = EmeraldDarkSecondary,
    onSecondary = EmeraldDarkOnSecondary,
    secondaryContainer = EmeraldDarkSecondaryContainer,
    onSecondaryContainer = EmeraldDarkOnSecondaryContainer,
    tertiary = EmeraldDarkTertiary,
    onTertiary = EmeraldDarkOnTertiary,
    tertiaryContainer = EmeraldDarkTertiaryContainer,
    onTertiaryContainer = EmeraldDarkOnTertiaryContainer,
    background = EmeraldDarkBackground,
    onBackground = EmeraldDarkOnSurface,
    surface = EmeraldDarkSurface,
    onSurface = EmeraldDarkOnSurface,
    surfaceVariant = EmeraldDarkSurfaceContainer,
    onSurfaceVariant = EmeraldDarkOnSurface.copy(alpha = 0.74f),
    surfaceContainerLow = EmeraldDarkSurfaceContainer,
    surfaceContainer = EmeraldDarkSurfaceContainer,
    surfaceContainerHigh = EmeraldDarkSurfaceContainerHigh,
    outline = EmeraldDarkOutline,
    outlineVariant = EmeraldDarkSurfaceContainerHigh
)

@Immutable
data class BintafaceAccents(
    val heroGradientColors: List<Color>,
    val bannerGradientColors: List<Color>,
    val feedTabColor: Color,
    val watchTabColor: Color,
    val liveTabColor: Color,
    val chatsTabColor: Color,
    val profileTabColor: Color,
    val isDark: Boolean
) {
    val heroBrush: Brush get() = Brush.linearGradient(heroGradientColors)
    val bannerBrush: Brush get() = Brush.linearGradient(bannerGradientColors)
}

val LocalBintafaceAccents = staticCompositionLocalOf {
    BintafaceAccents(
        heroGradientColors = listOf(AuroraPrimary, AuroraSecondary, AuroraTertiary),
        bannerGradientColors = listOf(AuroraPrimaryContainer, AuroraSecondaryContainer, AuroraTertiaryContainer),
        feedTabColor = AuroraPrimary,
        watchTabColor = AuroraSecondary,
        liveTabColor = RadioAccentColor,
        chatsTabColor = AuroraTertiary,
        profileTabColor = SuperlikeGold,
        isDark = false
    )
}

fun previewSwatchesForPreset(preset: AppThemePreset): List<Color> = when (preset) {
    AppThemePreset.AURORA_POP -> listOf(AuroraPrimary, AuroraSecondary, AuroraTertiary)
    AppThemePreset.TROPICAL_SUNSET -> listOf(SunsetPrimary, SunsetSecondary, SunsetTertiary)
    AppThemePreset.ELECTRIC_OCEAN -> listOf(OceanPrimary, OceanSecondary, OceanTertiary)
    AppThemePreset.ROYAL_EMERALD -> listOf(EmeraldPrimary, EmeraldSecondary, EmeraldTertiary)
}

fun colorSchemeFor(preset: AppThemePreset, darkTheme: Boolean): ColorScheme = when (preset) {
    AppThemePreset.AURORA_POP -> if (darkTheme) AuroraDarkScheme else AuroraLightScheme
    AppThemePreset.TROPICAL_SUNSET -> if (darkTheme) SunsetDarkScheme else SunsetLightScheme
    AppThemePreset.ELECTRIC_OCEAN -> if (darkTheme) OceanDarkScheme else OceanLightScheme
    AppThemePreset.ROYAL_EMERALD -> if (darkTheme) EmeraldDarkScheme else EmeraldLightScheme
}

val KindredShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun KindredTheme(
    preset: AppThemePreset = AppThemePreset.AURORA_POP,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val targetScheme = colorSchemeFor(preset, darkTheme)
    val animSpec = tween<Color>(durationMillis = 300)

    val primary by animateColorAsState(targetScheme.primary, animSpec, label = "primary")
    val onPrimary by animateColorAsState(targetScheme.onPrimary, animSpec, label = "onPrimary")
    val primaryContainer by animateColorAsState(targetScheme.primaryContainer, animSpec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(targetScheme.onPrimaryContainer, animSpec, label = "onPrimaryContainer")
    val secondary by animateColorAsState(targetScheme.secondary, animSpec, label = "secondary")
    val onSecondary by animateColorAsState(targetScheme.onSecondary, animSpec, label = "onSecondary")
    val secondaryContainer by animateColorAsState(targetScheme.secondaryContainer, animSpec, label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(targetScheme.onSecondaryContainer, animSpec, label = "onSecondaryContainer")
    val tertiary by animateColorAsState(targetScheme.tertiary, animSpec, label = "tertiary")
    val onTertiary by animateColorAsState(targetScheme.onTertiary, animSpec, label = "onTertiary")
    val tertiaryContainer by animateColorAsState(targetScheme.tertiaryContainer, animSpec, label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(targetScheme.onTertiaryContainer, animSpec, label = "onTertiaryContainer")
    val background by animateColorAsState(targetScheme.background, animSpec, label = "background")
    val onBackground by animateColorAsState(targetScheme.onBackground, animSpec, label = "onBackground")
    val surface by animateColorAsState(targetScheme.surface, animSpec, label = "surface")
    val onSurface by animateColorAsState(targetScheme.onSurface, animSpec, label = "onSurface")
    val surfaceVariant by animateColorAsState(targetScheme.surfaceVariant, animSpec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(targetScheme.onSurfaceVariant, animSpec, label = "onSurfaceVariant")
    val surfaceContainerLow by animateColorAsState(targetScheme.surfaceContainerLow, animSpec, label = "surfaceContainerLow")
    val surfaceContainer by animateColorAsState(targetScheme.surfaceContainer, animSpec, label = "surfaceContainer")
    val surfaceContainerHigh by animateColorAsState(targetScheme.surfaceContainerHigh, animSpec, label = "surfaceContainerHigh")
    val outline by animateColorAsState(targetScheme.outline, animSpec, label = "outline")
    val outlineVariant by animateColorAsState(targetScheme.outlineVariant, animSpec, label = "outlineVariant")

    val animatedScheme = targetScheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        outline = outline,
        outlineVariant = outlineVariant
    )

    val accents = BintafaceAccents(
        heroGradientColors = listOf(primary, primary.copy(alpha = 0.82f)),
        bannerGradientColors = listOf(
            surfaceContainer,
            primaryContainer.copy(alpha = if (darkTheme) 0.38f else 0.45f)
        ),
        feedTabColor = primary,
        watchTabColor = primary,
        liveTabColor = primary,
        chatsTabColor = primary,
        profileTabColor = primary,
        isDark = darkTheme
    )

    CompositionLocalProvider(LocalBintafaceAccents provides accents) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = KindredTypography,
            shapes = KindredShapes,
            content = content
        )
    }
}
