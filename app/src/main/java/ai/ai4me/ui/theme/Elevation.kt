package ai.ai4me.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Functional elevation tokens (Document 3 §2.4)
 */
@Immutable
data class Ai4MeElevation(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp
)

val LocalAi4MeElevation = staticCompositionLocalOf { Ai4MeElevation() }
