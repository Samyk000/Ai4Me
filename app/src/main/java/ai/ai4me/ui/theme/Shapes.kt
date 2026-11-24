package ai.ai4me.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Small, consistent radius scale (Document 3 §2.4)
 */
@Immutable
data class Ai4MeShapes(
    val xs: Shape = RoundedCornerShape(4.dp),
    val sm: Shape = RoundedCornerShape(8.dp),
    val md: Shape = RoundedCornerShape(12.dp),
    val lg: Shape = RoundedCornerShape(16.dp),
    val pill: Shape = RoundedCornerShape(999.dp)
)

val LocalAi4MeShapes = staticCompositionLocalOf { Ai4MeShapes() }
