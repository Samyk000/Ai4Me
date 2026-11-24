package ai.ai4me.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun Ai4MeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkAi4MeColors else LightAi4MeColors
    val spacing = Ai4MeSpacing()
    val typography = Ai4MeTypography()
    val shapes = Ai4MeShapes()
    val elevation = Ai4MeElevation()

    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            secondary = colors.secondary,
            onSecondary = colors.onSecondary,
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceVariant,
            outline = colors.outline,
            error = colors.error
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            secondary = colors.secondary,
            onSecondary = colors.onSecondary,
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceVariant,
            outline = colors.outline,
            error = colors.error
        )
    }

    CompositionLocalProvider(
        LocalAi4MeColors provides colors,
        LocalAi4MeSpacing provides spacing,
        LocalAi4MeTypography provides typography,
        LocalAi4MeShapes provides shapes,
        LocalAi4MeElevation provides elevation
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}

object Ai4MeTheme {
    val colors: Ai4MeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAi4MeColors.current

    val spacing: Ai4MeSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAi4MeSpacing.current

    val typography: Ai4MeTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAi4MeTypography.current

    val shapes: Ai4MeShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalAi4MeShapes.current

    val elevation: Ai4MeElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalAi4MeElevation.current
}
