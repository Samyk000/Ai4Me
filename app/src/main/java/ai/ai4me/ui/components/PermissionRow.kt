package ai.ai4me.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.ai4me.ui.theme.Ai4MeTheme

/**
 * Contextual Permission Row (Document 1 §3.1 & Document 3 §4.4)
 * Shows: What it enables, current status, why Ai4Me needs it, and action.
 * Never uses a generic "Grant All" button.
 */
@Composable
fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing

    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = Ai4MeTheme.typography.title,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = description,
                    style = Ai4MeTheme.typography.body,
                    color = colors.textSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(
                text = if (isGranted) "Granted" else "Not Granted",
                containerColor = if (isGranted) colors.success.copy(alpha = 0.15f) else colors.warning.copy(alpha = 0.15f),
                contentColor = if (isGranted) colors.success else colors.warning
            )

            OutlinedButton(
                onClick = onActionClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.primary
                ),
                shape = Ai4MeTheme.shapes.sm,
                modifier = Modifier.heightIn(min = spacing.minTouchTarget)
            ) {
                Text(
                    text = if (isGranted) "Settings" else "Configure",
                    style = Ai4MeTheme.typography.label
                )
            }
        }
    }
}
