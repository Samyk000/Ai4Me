package ai.ai4me.ui.screens.models

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ai.ai4me.ui.components.SectionCard
import ai.ai4me.ui.theme.Ai4MeTheme

/**
 * Model Library Screen (Document 3 §4.3 & Document 2 §3)
 * 4 Tabs: Featured, Browse, Downloads, Installed.
 * Enforces no-login public browsing contract.
 */
@Composable
fun ModelsScreen(
    modifier: Modifier = Modifier
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Featured", "Browse HF", "Downloads", "Installed")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Screen Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenPadding, vertical = spacing.md)
        ) {
            Text(
                text = "Model Library",
                style = Ai4MeTheme.typography.headline,
                color = colors.textPrimary
            )
            Text(
                text = "Public GGUF discovery without login • Verified local execution",
                style = Ai4MeTheme.typography.caption,
                color = colors.textSecondary
            )
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = colors.surface,
            contentColor = colors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = colors.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = Ai4MeTheme.typography.label,
                            color = if (selectedTabIndex == index) colors.primary else colors.textSecondary
                        )
                    }
                )
            }
        }

        // Tab Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.screenPadding),
            contentAlignment = Alignment.TopStart
        ) {
            when (selectedTabIndex) {
                0 -> FeaturedModelsPlaceholder()
                1 -> BrowseHuggingFacePlaceholder()
                2 -> DownloadsPlaceholder()
                3 -> InstalledModelsPlaceholder()
            }
        }
    }
}

@Composable
private fun FeaturedModelsPlaceholder() {
    val spacing = Ai4MeTheme.spacing
    val colors = Ai4MeTheme.colors

    Column {
        SectionCard(title = "Starter Models (Curated)") {
            Text(
                text = "Tested and verified for detected hardware profiles.",
                style = Ai4MeTheme.typography.body,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = "Milestone 2 will provide direct one-tap download of tested 3B-4B starter models with verified SHA-256 checksums.",
                style = Ai4MeTheme.typography.caption,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun BrowseHuggingFacePlaceholder() {
    val colors = Ai4MeTheme.colors
    SectionCard(title = "Hugging Face Hub Discovery") {
        Text(
            text = "Search public GGUF model repositories without requiring an account or API token.",
            style = Ai4MeTheme.typography.body,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun DownloadsPlaceholder() {
    val colors = Ai4MeTheme.colors
    SectionCard(title = "Download Queue") {
        Text(
            text = "No active downloads.",
            style = Ai4MeTheme.typography.body,
            color = colors.textMuted
        )
    }
}

@Composable
private fun InstalledModelsPlaceholder() {
    val colors = Ai4MeTheme.colors
    SectionCard(title = "Local Models") {
        Text(
            text = "No local models currently installed in app storage.",
            style = Ai4MeTheme.typography.body,
            color = colors.textMuted
        )
    }
}
