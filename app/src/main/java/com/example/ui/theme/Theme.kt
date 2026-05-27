package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class PastelTheme(val displayName: String) {
    MINT("Mint Sage"),
    ROSE("Rose Blush"),
    LAVENDER("Lavender Mist"),
    PEACH("Peach Sorbet")
}

private val MintLight = lightColorScheme(
    primary = MintPrimaryLight,
    secondary = MintSecondaryLight,
    tertiary = MintTertiaryLight,
    background = MintBackgroundLight,
    surface = MintSurfaceLight
)

private val MintDark = darkColorScheme(
    primary = MintPrimaryDark,
    secondary = MintSecondaryDark,
    tertiary = MintTertiaryDark,
    background = MintBackgroundDark,
    surface = MintSurfaceDark
)

private val RoseLight = lightColorScheme(
    primary = RosePrimaryLight,
    secondary = RoseSecondaryLight,
    tertiary = RoseTertiaryLight,
    background = RoseBackgroundLight,
    surface = RoseSurfaceLight
)

private val RoseDark = darkColorScheme(
    primary = RosePrimaryDark,
    secondary = RoseSecondaryDark,
    tertiary = RoseTertiaryDark,
    background = RoseBackgroundDark,
    surface = RoseSurfaceDark
)

private val LavenderLight = lightColorScheme(
    primary = LavenderPrimaryLight,
    secondary = LavenderSecondaryLight,
    tertiary = LavenderTertiaryLight,
    background = LavenderBackgroundLight,
    surface = LavenderSurfaceLight
)

private val LavenderDark = darkColorScheme(
    primary = LavenderPrimaryDark,
    secondary = LavenderSecondaryDark,
    tertiary = LavenderTertiaryDark,
    background = LavenderBackgroundDark,
    surface = LavenderSurfaceDark
)

private val PeachLight = lightColorScheme(
    primary = PeachPrimaryLight,
    secondary = PeachSecondaryLight,
    tertiary = PeachTertiaryLight,
    background = PeachBackgroundLight,
    surface = PeachSurfaceLight
)

private val PeachDark = darkColorScheme(
    primary = PeachPrimaryDark,
    secondary = PeachSecondaryDark,
    tertiary = PeachTertiaryDark,
    background = PeachBackgroundDark,
    surface = PeachSurfaceDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean,
    appTheme: PastelTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        PastelTheme.MINT -> if (darkTheme) MintDark else MintLight
        PastelTheme.ROSE -> if (darkTheme) RoseDark else RoseLight
        PastelTheme.LAVENDER -> if (darkTheme) LavenderDark else LavenderLight
        PastelTheme.PEACH -> if (darkTheme) PeachDark else PeachLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
