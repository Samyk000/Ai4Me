package ai.ai4me.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ai.ai4me.ui.components.SectionCard
import ai.ai4me.ui.theme.Ai4MeTheme

/**
 * Activity & Task History Screen (Document 3 §4.5)
 * Shows: Task name, current status, time, steps completed, verification result.
 * Plain language, no raw developer stack traces.
 */
@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(spacing.screenPadding)
    ) {
        Text(
            text = "Activity & Audit",
            style = Ai4MeTheme.typography.headline,
            color = colors.textPrimary,
            modifier = Modifier.padding(bottom = spacing.md)
        )

        SectionCard(title = "Task Execution History") {
            Text(
                text = "No recorded tasks yet.",
                style = Ai4MeTheme.typography.body,
                color = colors.textMuted
            )
            Text(
                text = "When the agent plans and executes actions (Milestone 3 & 6), every step, tool observation, and verification result will be logged here with complete user visibility.",
                style = Ai4MeTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.xs)
            )
        }
    }
}
