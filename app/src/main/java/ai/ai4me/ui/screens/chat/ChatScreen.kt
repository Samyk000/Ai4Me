package ai.ai4me.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.ai4me.ui.components.StatusBadge
import ai.ai4me.ui.theme.Ai4MeTheme

/**
 * Chat Screen (Document 3 §4.2)
 * Clean conversation container ready for Milestone 2 streaming inference.
 */
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenPadding, vertical = spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Conversation",
                    style = Ai4MeTheme.typography.title,
                    color = colors.textPrimary
                )
                Text(
                    text = "Local offline session",
                    style = Ai4MeTheme.typography.caption,
                    color = colors.textSecondary
                )
            }
            StatusBadge(
                text = "Model Not Loaded",
                containerColor = colors.warning.copy(alpha = 0.15f),
                contentColor = colors.warning
            )
        }

        // Empty state container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(spacing.screenPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No Active Conversation",
                    style = Ai4MeTheme.typography.title,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = "Download and load a local model in the Models tab to begin offline text and voice conversations.",
                    style = Ai4MeTheme.typography.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = spacing.lg)
                )
            }
        }

        // Input bar (IME-safe container)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = spacing.screenPadding, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Message Ai4Me...",
                        style = Ai4MeTheme.typography.body,
                        color = colors.textMuted
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = spacing.minTouchTarget),
                shape = Ai4MeTheme.shapes.sm,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.outline,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            IconButton(
                onClick = { /* Send handled in M2 */ },
                enabled = inputText.isNotBlank(),
                modifier = Modifier.size(spacing.minTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = if (inputText.isNotBlank()) colors.primary else colors.disabled
                )
            }
        }
    }
}
