package ai.ai4me.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Immutable

/**
 * Emil Kowalski Motion & Animation Standards (Document 3 §5)
 * Transitions are short, purposeful, interruptible, and avoid layout shifts or battery drain.
 */
@Immutable
object Ai4MeMotion {
    // Standard durations (ms)
    const val DurationInstant = 100
    const val DurationFast = 180
    const val DurationNormal = 250
    const val DurationDeliberate = 350

    // Purposeful easings
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardEasing: Easing = FastOutSlowInEasing

    // Restrained state indicators: pulse duration for idle orb breathing
    const val OrbBreathingDuration = 2800
}
