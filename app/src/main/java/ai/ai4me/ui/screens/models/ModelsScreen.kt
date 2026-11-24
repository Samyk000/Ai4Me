package ai.ai4me.ui.screens.models

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import ai.ai4me.data.models.DownloadProgress
import ai.ai4me.hardware.benchmark.BenchmarkMetrics
import ai.ai4me.hardware.benchmark.HardwareBenchmarkRunner
import ai.ai4me.data.models.DownloadState
import ai.ai4me.data.models.FeaturedModel
import ai.ai4me.data.models.HuggingFaceModelSummary
import ai.ai4me.data.models.InstalledModel
import ai.ai4me.data.models.ModelCatalogRepository
import ai.ai4me.data.models.ModelDownloadManager
import ai.ai4me.hardware.HardwareRecommendation
import ai.ai4me.intelligence.llm.ModelRuntimeManager
import ai.ai4me.intelligence.llm.ModelRuntimeState
import ai.ai4me.ui.components.SectionCard
import ai.ai4me.ui.components.StatusBadge
import ai.ai4me.ui.theme.Ai4MeTheme
import kotlinx.coroutines.launch
import java.io.File

/**
 * Model Library Screen (Document 3 §4.3 & Document 2 §3)
 * Full 4-tab model management interface with zero login requirements for public files.
 */
@Composable
fun ModelsScreen(
    catalogRepository: ModelCatalogRepository,
    downloadManager: ModelDownloadManager,
    runtimeManager: ModelRuntimeManager,
    recommendation: HardwareRecommendation,
    modifier: Modifier = Modifier
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Featured", "Browse HF", "Downloads", "Installed", "Benchmark")

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
                text = "Public GGUF discovery without login • On-device verification",
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.screenPadding)
        ) {
            when (selectedTabIndex) {
                0 -> FeaturedTab(
                    catalogRepository = catalogRepository,
                    downloadManager = downloadManager,
                    recommendation = recommendation
                )
                1 -> BrowseHfTab(
                    catalogRepository = catalogRepository,
                    downloadManager = downloadManager
                )
                2 -> DownloadsTab(
                    downloadManager = downloadManager
                )
                3 -> InstalledTab(
                    downloadManager = downloadManager,
                    runtimeManager = runtimeManager
                )
                4 -> BenchmarkTab(
                    runtimeManager = runtimeManager
                )
            }
        }
    }
}

@Composable
private fun FeaturedTab(
    catalogRepository: ModelCatalogRepository,
    downloadManager: ModelDownloadManager,
    recommendation: HardwareRecommendation
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    var featuredModels by remember { mutableStateOf<List<FeaturedModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        featuredModels = catalogRepository.getFeaturedModels()
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colors.primary)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            modifier = Modifier.fillMaxSize()
        ) {
            items(featuredModels) { model ->
                FeaturedModelCard(
                    model = model,
                    downloadManager = downloadManager,
                    recommendation = recommendation
                )
            }
        }
    }
}

@Composable
private fun FeaturedModelCard(
    model: FeaturedModel,
    downloadManager: ModelDownloadManager,
    recommendation: HardwareRecommendation
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val downloadProgressMap by downloadManager.downloadProgress.collectAsState()
    val currentDownload = downloadProgressMap[model.id]
    val sizeGb = String.format("%.2f", model.sizeBytes / (1024f * 1024f * 1024f))

    val isProfileMatch = model.supportedProfile.equals(
        recommendation.recommendedProfile.name,
        ignoreCase = true
    )

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.displayName,
                    style = Ai4MeTheme.typography.title,
                    color = colors.textPrimary
                )
                Text(
                    text = "${model.publisher} • ${model.parameterCount} • ${model.quantization}",
                    style = Ai4MeTheme.typography.caption,
                    color = colors.textSecondary
                )
            }

            if (isProfileMatch) {
                StatusBadge(
                    text = "Recommended",
                    containerColor = colors.success.copy(alpha = 0.15f),
                    contentColor = colors.success
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        Text(
            text = model.compatibilityNotes,
            style = Ai4MeTheme.typography.body,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$sizeGb GB • ${model.license}",
                    style = Ai4MeTheme.typography.caption,
                    color = colors.textPrimary
                )
                Text(
                    text = "Estimated RAM: ${model.estimatedRamMinGb} – ${model.estimatedRamMaxGb} GB",
                    style = Ai4MeTheme.typography.caption,
                    color = colors.textMuted
                )
            }

            if (currentDownload != null && currentDownload.state == DownloadState.DOWNLOADING) {
                StatusBadge(
                    text = "${currentDownload.progressPercent.toInt()}%",
                    containerColor = colors.primary.copy(alpha = 0.15f),
                    contentColor = colors.primary
                )
            } else {
                Button(
                    onClick = {
                        downloadManager.startDownload(
                            modelId = model.id,
                            fileName = model.fileName,
                            downloadUrl = model.downloadUrl,
                            expectedSizeBytes = model.sizeBytes
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = Ai4MeTheme.shapes.sm,
                    modifier = Modifier.heightIn(min = spacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Download",
                        style = Ai4MeTheme.typography.label,
                        modifier = Modifier.padding(start = spacing.xs)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseHfTab(
    catalogRepository: ModelCatalogRepository,
    downloadManager: ModelDownloadManager
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("qwen") }
    var searchResults by remember { mutableStateOf<List<HuggingFaceModelSummary>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search public Hugging Face models...", color = colors.textMuted)
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

            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        scope.launch {
                            isSearching = true
                            searchError = null
                            val res = catalogRepository.searchModels(searchQuery)
                            isSearching = false
                            if (res.isSuccess) {
                                searchResults = res.getOrDefault(emptyList())
                            } else {
                                searchError = res.exceptionOrNull()?.message ?: "Search failed"
                            }
                        }
                    }
                },
                shape = Ai4MeTheme.shapes.sm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                modifier = Modifier.heightIn(min = spacing.minTouchTarget)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
            }
        }

        if (isSearching) {
            Box(modifier = Modifier.fillMaxWidth().padding(spacing.xl), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else if (searchError != null) {
            Text(
                text = "Error: $searchError",
                style = Ai4MeTheme.typography.body,
                color = colors.error
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults) { item ->
                    SectionCard {
                        Text(
                            text = item.id,
                            style = Ai4MeTheme.typography.title,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Downloads: ${item.downloads} • Likes: ${item.likes}",
                            style = Ai4MeTheme.typography.caption,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsTab(
    downloadManager: ModelDownloadManager
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val downloadMap by downloadManager.downloadProgress.collectAsState()
    val downloads = downloadMap.values.toList()

    if (downloads.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No active downloads.",
                style = Ai4MeTheme.typography.body,
                color = colors.textMuted
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            modifier = Modifier.fillMaxSize()
        ) {
            items(downloads) { dl ->
                DownloadCard(download = dl, downloadManager = downloadManager)
            }
        }
    }
}

@Composable
private fun DownloadCard(
    download: DownloadProgress,
    downloadManager: ModelDownloadManager
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val mbDownloaded = download.bytesDownloaded / (1024 * 1024)
    val mbTotal = download.totalBytes / (1024 * 1024)
    val speedMb = String.format("%.1f", download.speedBytesPerSec / (1024f * 1024f))

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.fileName,
                    style = Ai4MeTheme.typography.title,
                    color = colors.textPrimary
                )
                Text(
                    text = "$mbDownloaded MB / $mbTotal MB • $speedMb MB/s",
                    style = Ai4MeTheme.typography.caption,
                    color = colors.textSecondary
                )
            }
            StatusBadge(
                text = download.state.name,
                containerColor = when (download.state) {
                    DownloadState.COMPLETED -> colors.success.copy(alpha = 0.15f)
                    DownloadState.FAILED -> colors.error.copy(alpha = 0.15f)
                    else -> colors.primary.copy(alpha = 0.15f)
                },
                contentColor = when (download.state) {
                    DownloadState.COMPLETED -> colors.success
                    DownloadState.FAILED -> colors.error
                    else -> colors.primary
                }
            )
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        LinearProgressIndicator(
            progress = { (download.progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(Ai4MeTheme.shapes.pill),
            color = colors.primary,
            trackColor = colors.surfaceVariant
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (download.state == DownloadState.DOWNLOADING) {
                OutlinedButton(
                    onClick = { downloadManager.pauseDownload(download.modelId) },
                    shape = Ai4MeTheme.shapes.sm,
                    modifier = Modifier.heightIn(min = spacing.minTouchTarget)
                ) {
                    Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause")
                    Text("Pause", modifier = Modifier.padding(start = spacing.xs))
                }
            }
            IconButton(
                onClick = { downloadManager.cancelAndDelete(download.modelId, download.fileName) },
                modifier = Modifier.size(spacing.minTouchTarget)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = colors.error)
            }
        }
    }
}

@Composable
private fun InstalledTab(
    downloadManager: ModelDownloadManager,
    runtimeManager: ModelRuntimeManager
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val scope = rememberCoroutineScope()
    var installedModels by remember { mutableStateOf<List<InstalledModel>>(emptyList()) }
    val runtimeState by runtimeManager.runtimeState.collectAsState()
    val activeModelFile = runtimeManager.loadedModelFile

    fun refresh() {
        installedModels = downloadManager.listInstalledModels()
    }

    LaunchedEffect(runtimeState) {
        refresh()
    }

    if (installedModels.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No models installed in app storage.",
                style = Ai4MeTheme.typography.body,
                color = colors.textMuted
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            modifier = Modifier.fillMaxSize()
        ) {
            items(installedModels) { model ->
                val isLoaded = activeModelFile?.name == model.fileName

                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.fileName,
                                style = Ai4MeTheme.typography.title,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${String.format("%.2f", model.sizeGb)} GB • ${model.quantization} • ${model.architecture}",
                                style = Ai4MeTheme.typography.caption,
                                color = colors.textSecondary
                            )
                        }

                        StatusBadge(
                            text = if (isLoaded) "Loaded" else "Installed",
                            containerColor = if (isLoaded) colors.success.copy(alpha = 0.15f) else colors.surfaceVariant,
                            contentColor = if (isLoaded) colors.success else colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoaded) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch { runtimeManager.unloadModel() }
                                },
                                shape = Ai4MeTheme.shapes.sm,
                                modifier = Modifier.heightIn(min = spacing.minTouchTarget)
                            ) {
                                Text("Unload")
                            }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        runtimeManager.loadModel(
                                            File(model.filePath),
                                            ai.ai4me.intelligence.llm.RuntimeConfig()
                                        )
                                    }
                                },
                                shape = Ai4MeTheme.shapes.sm,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                ),
                                modifier = Modifier.heightIn(min = spacing.minTouchTarget)
                            ) {
                                Text("Load Model")
                            }
                        }

                        IconButton(
                            onClick = {
                                downloadManager.cancelAndDelete(model.id, model.fileName)
                                refresh()
                            },
                            modifier = Modifier.size(spacing.minTouchTarget)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = colors.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BenchmarkTab(
    runtimeManager: ModelRuntimeManager
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val scope = rememberCoroutineScope()
    val runner = remember { HardwareBenchmarkRunner() }
    var isRunning by remember { mutableStateOf(false) }
    var metrics by remember { mutableStateOf<BenchmarkMetrics?>(null) }
    val activeState by runtimeManager.runtimeState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        SectionCard(title = "Hardware Performance Benchmark") {
            Text(
                text = "Measures on-device generation throughput (tokens/sec), prompt processing speed, memory footprint, and thermal status under local load.",
                style = Ai4MeTheme.typography.body,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            Button(
                onClick = {
                    scope.launch {
                        isRunning = true
                        val activeName = runtimeManager.loadedModelFile?.name ?: "Local-Hardware-Engine"
                        metrics = runner.runBenchmark(activeName, runtimeManager)
                        isRunning = false
                    }
                },
                enabled = !isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colors.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text("Running Benchmark...", color = colors.onPrimary)
                } else {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = colors.onPrimary)
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text("Run Benchmark", color = colors.onPrimary)
                }
            }
        }

        metrics?.let { m ->
            SectionCard(title = "Benchmark Results: ${m.modelName}") {
                MetricItem(label = "Generation Throughput", value = "${m.tokensPerSecond} tokens/sec")
                MetricItem(label = "Prompt Processing Speed", value = "${m.promptTokensPerSecond} tokens/sec")
                MetricItem(label = "Time To First Token (TTFT)", value = "${m.timeToFirstTokenMs} ms")
                MetricItem(label = "Peak Memory Footprint", value = "${m.peakMemoryMb} MB")
                MetricItem(label = "Steady-State Memory", value = "${m.steadyStateMemoryMb} MB")
                MetricItem(label = "Thermal Status", value = m.thermalState)
                MetricItem(label = "STT Chunk Latency", value = "${m.sttLatencyMs} ms")
                MetricItem(label = "TTS Synthesis Latency", value = "${m.ttsLatencyMs} ms")
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = Ai4MeTheme.typography.caption, color = colors.textSecondary)
        Text(text = value, style = Ai4MeTheme.typography.label, color = colors.primary)
    }
}
