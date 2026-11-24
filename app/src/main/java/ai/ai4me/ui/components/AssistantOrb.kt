package ai.ai4me.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.ai4me.ui.theme.Ai4MeMotion
import ai.ai4me.ui.theme.Ai4MeTheme

enum class AssistantState(val label: String) {
    IDLE("Ready"),
    LISTENING("Listening"),
    PROCESSING("Processing"),
    SPEAKING("Speaking"),
    ERROR("Error")
}

/**
 * Functional Assistant Orb (Document 3 §4.1)
 * Restrained visual state indicator with mandatory TalkBack content description.
 */
@Composable
fun AssistantOrb(
    state: AssistantState,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    onClick: (() -> Unit)? = null
) {
    val colors = Ai4MeTheme.colors

    val targetColor: Color = when (state) {
        AssistantState.IDLE -> colors.primary
        AssistantState.LISTENING -> colors.listening
        AssistantState.PROCESSING -> colors.processing
        AssistantState.SPEAKING -> colors.speaking
        AssistantState.ERROR -> colors.error
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = Ai4MeMotion.DurationFast),
        label = "OrbColorAnimation"
    )

    // Restrained breathing animation (Document 3 §5)
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) 1.05f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AssistantState.LISTENING) 1200 else Ai4MeMotion.OrbBreathingDuration,
                easing = Ai4MeMotion.StandardEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    val contentDesc = "Ai4Me Assistant Status: ${state.label}"
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = contentDesc
                role = Role.Button
            }
            .then(clickModifier)
    ) {
        Canvas(modifier = Modifier.size(size * pulseScale)) {
            val radius = (this.size.minDimension / 2f) * 0.78f
            val centerOffset = center

            // Outer soft ambient ring
            drawCircle(
                color = animatedColor.copy(alpha = 0.15f),
                radius = radius * 1.18f,
                center = centerOffset
            )

            // Dynamic boundary stroke
            drawCircle(
                color = animatedColor.copy(alpha = 0.5f),
                radius = radius,
                center = centerOffset,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Inner solid core
            drawCircle(
                color = animatedColor.copy(alpha = 0.85f),
                radius = radius * 0.72f,
                center = centerOffset
            )
        }
    }
}
