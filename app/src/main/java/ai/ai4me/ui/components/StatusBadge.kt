package ai.ai4me.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ai.ai4me.ui.theme.Ai4MeTheme

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color = Ai4MeTheme.colors.surfaceVariant,
    contentColor: Color = Ai4MeTheme.colors.textPrimary,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = containerColor,
                shape = Ai4MeTheme.shapes.pill
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics {
                contentDescription = "Status: $text"
            }
    ) {
        Text(
            text = text,
            style = Ai4MeTheme.typography.caption,
            color = contentColor
        )
    }
}
