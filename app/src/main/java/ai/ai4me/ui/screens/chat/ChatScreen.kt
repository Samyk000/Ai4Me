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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
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
import ai.ai4me.intelligence.agent.AgentOrchestrator
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
    val toolActivity: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    ASSISTANT
}

/**
 * Chat Screen (Document 3 §4.2 & Document 1 §5.6)
 * Supports live token streaming, tool activity indicators, stop generation, and retry.
 */
@Composable
fun ChatScreen(
    runtimeManager: ModelRuntimeManager,
    orchestrator: AgentOrchestrator,
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
    val executionState by orchestrator.executionState.collectAsState()

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    val isGenerating = runtimeState == ModelRuntimeState.GENERATING || executionState.isRunning

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
        val isTaskRequest = text.contains("status", ignoreCase = true) ||
                text.contains("remember", ignoreCase = true) ||
                text.contains("notification", ignoreCase = true) ||
                text.contains("check", ignoreCase = true)

        messages.add(
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "",
                isStreaming = true,
                toolActivity = if (isTaskRequest) "Analyzing task & tools..." else null
            )
        )

        scope.launch {
            try {
                if (isTaskRequest) {
                    val task = orchestrator.executeTask(text, runtimeManager)
                    val summary = task.summary ?: "Task execution finished with status: ${task.status}"
                    messages[assistantMessageIndex] = messages[assistantMessageIndex].copy(
                        text = summary,
                        isStreaming = false,
                        toolActivity = "Verified (${task.status})"
                    )
                } else {
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
                            metrics = metricsString,
                            toolActivity = null
                        )
                    }
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
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenPadding, vertical = spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Conversation & Tools",
                    style = Ai4MeTheme.typography.title,
                    color = colors.textPrimary
                )
                Text(
                    text = if (loadedModelFile != null) loadedModelFile.name else "Autonomous local agent",
                    style = Ai4MeTheme.typography.caption,
                    color = colors.textSecondary
                )
            }
            StatusBadge(
                text = when {
                    executionState.isRunning -> "Executing Tool"
                    runtimeState == ModelRuntimeState.GENERATING -> "Generating"
                    runtimeState == ModelRuntimeState.LOADED -> "Model Ready"
                    else -> "Ready (Tools Active)"
                },
                containerColor = when {
                    executionState.isRunning -> colors.processing.copy(alpha = 0.15f)
                    runtimeState == ModelRuntimeState.LOADED -> colors.success.copy(alpha = 0.15f)
                    else -> colors.primary.copy(alpha = 0.15f)
                },
                contentColor = when {
                    executionState.isRunning -> colors.processing
                    runtimeState == ModelRuntimeState.LOADED -> colors.success
                    else -> colors.primary
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ai4Me Agent Ready",
                        style = Ai4MeTheme.typography.title,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = "Try asking: \"Check device status\", \"Remember that I prefer concise answers\", or \"Check my notifications\".",
                        style = Ai4MeTheme.typography.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = spacing.lg)
                    )
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
                        text = "Message Ai4Me or request a task...",
                        color = colors.textMuted
                    )
                },
                enabled = !isGenerating,
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
                        contentDescription = "Stop",
                        tint = colors.error
                    )
                }
            } else {
                IconButton(
                    onClick = { sendMessage(inputText) },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier.size(spacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) colors.primary else colors.disabled
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Tool Activity Badge (Document 3 §4.2)
        if (!isUser && message.toolActivity != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = spacing.xxs)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = " ${message.toolActivity}",
                    style = Ai4MeTheme.typography.caption,
                    color = colors.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(Ai4MeTheme.shapes.md)
                .background(if (isUser) colors.primary else colors.surface)
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

        if (!isUser && !message.isStreaming && message.text.isNotEmpty()) {
            Row(modifier = Modifier.padding(top = spacing.xxs)) {
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
