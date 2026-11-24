package ai.ai4me.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ai.ai4me.ui.theme.Ai4MeTheme

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val shapes = Ai4MeTheme.shapes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.md)
            .background(colors.surface)
            .border(width = 1.dp, color = colors.outline.copy(alpha = 0.5f), shape = shapes.md)
            .padding(spacing.lg)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = Ai4MeTheme.typography.title,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = spacing.sm)
            )
        }
        content()
    }
}
