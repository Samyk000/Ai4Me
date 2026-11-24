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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import ai.ai4me.intelligence.llm.ModelRuntimeManager
import ai.ai4me.intelligence.llm.ModelRuntimeState
import ai.ai4me.intelligence.llm.SamplingParams
import ai.ai4me.ui.components.SectionCard
import ai.ai4me.ui.components.StatusBadge
import ai.ai4me.ui.theme.Ai4MeTheme
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val isStreaming: Boolean = false,
    val metrics: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    ASSISTANT
}

/**
 * Chat Screen (Document 3 §4.2 & Document 1 §3.2)
 * Supports live token streaming, stop generation, retry, and copy.
 */
@Composable
fun ChatScreen(
    runtimeManager: ModelRuntimeManager,
    onNavigateToModels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val runtimeState by runtimeManager.runtimeState.collectAsState()
    val loadedModelFile = runtimeManager.loadedModelFile

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    val isGenerating = runtimeState == ModelRuntimeState.GENERATING

    // Auto-scroll when new messages or tokens arrive
    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isGenerating) return

        messages.add(ChatMessage(sender = MessageSender.USER, text = text))
        inputText = ""

        val assistantMessageIndex = messages.size
        messages.add(
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "",
                isStreaming = true
            )
        )

        scope.launch {
            try {
                var fullResponse = ""
                var lastTps = 0f
                var ttft = 0L

                runtimeManager.generateStream(text, SamplingParams()).collect { chunk ->
                    if (chunk.token.isNotEmpty()) {
                        fullResponse += chunk.token
                    }
                    if (chunk.tokensPerSecond > 0f) lastTps = chunk.tokensPerSecond
                    if (chunk.timeToFirstTokenMs > 0L) ttft = chunk.timeToFirstTokenMs

                    val metricsString = if (lastTps > 0f) {
                        "${String.format("%.1f", lastTps)} t/s • TTFT ${ttft}ms"
                    } else null

                    messages[assistantMessageIndex] = messages[assistantMessageIndex].copy(
                        text = fullResponse,
                        isStreaming = !chunk.isFinished,
                        metrics = metricsString
                    )
                }
            } catch (e: Exception) {
                messages[assistantMessageIndex] = messages[assistantMessageIndex].copy(
                    text = "Inference error: ${e.message}",
                    isStreaming = false
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Chat Header with real model status badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenPadding, vertical = spacing.sm),
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
                    text = if (loadedModelFile != null) loadedModelFile.name else "No model active",
                    style = Ai4MeTheme.typography.caption,
                    color = colors.textSecondary
                )
            }
            StatusBadge(
                text = when (runtimeState) {
                    ModelRuntimeState.LOADED -> "Model Ready"
                    ModelRuntimeState.GENERATING -> "Generating"
                    ModelRuntimeState.LOADING -> "Loading Model"
                    else -> "No Model"
                },
                containerColor = when (runtimeState) {
                    ModelRuntimeState.LOADED -> colors.success.copy(alpha = 0.15f)
                    ModelRuntimeState.GENERATING -> colors.primary.copy(alpha = 0.15f)
                    ModelRuntimeState.LOADING -> colors.processing.copy(alpha = 0.15f)
                    else -> colors.warning.copy(alpha = 0.15f)
                },
                contentColor = when (runtimeState) {
                    ModelRuntimeState.LOADED -> colors.success
                    ModelRuntimeState.GENERATING -> colors.primary
                    ModelRuntimeState.LOADING -> colors.processing
                    else -> colors.warning
                }
            )
        }

        // Messages list or Empty state
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(spacing.screenPadding),
                contentAlignment = Alignment.Center
            ) {
                if (loadedModelFile == null) {
                    SectionCard(title = "Local Model Required") {
                        Text(
                            text = "To run private text and voice conversations, you need to load a model in the Model Library.",
                            style = Ai4MeTheme.typography.body,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(spacing.md))
                        Button(
                            onClick = onNavigateToModels,
                            shape = Ai4MeTheme.shapes.sm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            modifier = Modifier.heightIn(min = spacing.minTouchTarget)
                        ) {
                            Text("Open Model Library")
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Model Loaded & Ready",
                            style = Ai4MeTheme.typography.title,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "Ask a question or test prompt to verify local inference.",
                            style = Ai4MeTheme.typography.body,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                items(messages) { message ->
                    ChatMessageBubble(
                        message = message,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(message.text))
                        },
                        onRetry = {
                            if (message.sender == MessageSender.USER) {
                                sendMessage(message.text)
                            }
                        }
                    )
                }
            }
        }

        // Input Bar
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
                        text = if (loadedModelFile != null) "Message Ai4Me..." else "Load a model to chat...",
                        color = colors.textMuted
                    )
                },
                enabled = loadedModelFile != null && !isGenerating,
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

            if (isGenerating) {
                IconButton(
                    onClick = { runtimeManager.cancelGeneration() },
                    modifier = Modifier.size(spacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Generation",
                        tint = colors.error
                    )
                }
            } else {
                IconButton(
                    onClick = { sendMessage(inputText) },
                    enabled = loadedModelFile != null && inputText.isNotBlank(),
                    modifier = Modifier.size(spacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (loadedModelFile != null && inputText.isNotBlank()) colors.primary else colors.disabled
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    if (isUser) {
                        Ai4MeTheme.shapes.md
                    } else {
                        Ai4MeTheme.shapes.md
                    }
                )
                .background(
                    if (isUser) colors.primary else colors.surface
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) colors.primary else colors.outline.copy(alpha = 0.5f),
                    shape = Ai4MeTheme.shapes.md
                )
                .padding(spacing.md)
        ) {
            Column {
                Text(
                    text = if (message.text.isEmpty() && message.isStreaming) "..." else message.text,
                    style = Ai4MeTheme.typography.body,
                    color = if (isUser) colors.onPrimary else colors.textPrimary
                )

                if (!isUser && message.metrics != null) {
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = message.metrics,
                        style = Ai4MeTheme.typography.caption,
                        color = colors.textMuted
                    )
                }
            }
        }

        // Action icons for assistant message
        if (!isUser && !message.isStreaming && message.text.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = spacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Response",
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
