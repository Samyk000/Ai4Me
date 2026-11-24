package ai.ai4me.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.ai4me.hardware.DeviceHardwareSpec
import ai.ai4me.hardware.HardwareRecommendation
import ai.ai4me.ui.components.AssistantOrb
import ai.ai4me.ui.components.AssistantState
import ai.ai4me.ui.components.SectionCard
import ai.ai4me.ui.components.StatusBadge
import ai.ai4me.ui.theme.Ai4MeTheme

/**
 * Home Screen (Document 3 §4.1)
 * Purpose: Central interaction surface, unmistakable assistant state,
 * genuine hardware readiness indicator, and quick actions.
 */
@Composable
fun HomeScreen(
    hardwareSpec: DeviceHardwareSpec,
    recommendation: HardwareRecommendation,
    onNavigateToChat: () -> Unit,
    onNavigateToModels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    var assistantState by remember { mutableStateOf(AssistantState.IDLE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenPadding, vertical = spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.lg)
    ) {
        // App header & privacy badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ai4Me",
                    style = Ai4MeTheme.typography.headline,
                    color = colors.textPrimary
                )
                Text(
                    text = "Private Offline Personal Agent",
                    style = Ai4MeTheme.typography.caption,
                    color = colors.textSecondary
                )
            }
            StatusBadge(
                text = "100% Local Runtime",
                containerColor = colors.success.copy(alpha = 0.12f),
                contentColor = colors.success
            )
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        // Functional State Orb (Document 3 §4.1)
        AssistantOrb(
            state = assistantState,
            size = 140.dp,
            onClick = {
                assistantState = when (assistantState) {
                    AssistantState.IDLE -> AssistantState.LISTENING
                    AssistantState.LISTENING -> AssistantState.PROCESSING
                    AssistantState.PROCESSING -> AssistantState.SPEAKING
                    AssistantState.SPEAKING -> AssistantState.IDLE
                    AssistantState.ERROR -> AssistantState.IDLE
                }
            }
        )

        Text(
            text = assistantState.label,
            style = Ai4MeTheme.typography.title,
            color = colors.textPrimary
        )

        // Mic interaction button (Enforces minimum 48dp touch target)
        Button(
            onClick = {
                assistantState = if (assistantState == AssistantState.LISTENING) AssistantState.IDLE else AssistantState.LISTENING
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            ),
            shape = Ai4MeTheme.shapes.sm,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.minTouchTarget)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Input",
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = if (assistantState == AssistantState.LISTENING) "Stop Listening" else "Hold or Tap to Speak",
                style = Ai4MeTheme.typography.label,
                modifier = Modifier.padding(start = spacing.sm)
            )
        }

        // Hardware Profile Readiness Card
        SectionCard(title = "Device Profile & Headroom") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.recommendedProfile.title,
                        style = Ai4MeTheme.typography.title,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${String.format("%.1f", hardwareSpec.memory.availableRamGb)} GB RAM available • ${hardwareSpec.cpuCoreCount} Cores",
                        style = Ai4MeTheme.typography.caption,
                        color = colors.textSecondary
                    )
                }
                StatusBadge(
                    text = if (recommendation.canRunLocalModels) "Ready" else "Unsupported",
                    containerColor = if (recommendation.canRunLocalModels) colors.success.copy(alpha = 0.15f) else colors.error.copy(alpha = 0.15f),
                    contentColor = if (recommendation.canRunLocalModels) colors.success else colors.error
                )
            }
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = recommendation.reasoning,
                style = Ai4MeTheme.typography.body,
                color = colors.textSecondary
            )
        }

        // Recent Task Summary Card (Document 3 §4.1)
        SectionCard(title = "Recent Activity") {
            Text(
                text = "No recent tasks executed.",
                style = Ai4MeTheme.typography.body,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = "When tasks are executed (e.g. notifications, calendar, messaging), observable verification steps will appear here.",
                style = Ai4MeTheme.typography.caption,
                color = colors.textSecondary
            )
        }
    }
}
