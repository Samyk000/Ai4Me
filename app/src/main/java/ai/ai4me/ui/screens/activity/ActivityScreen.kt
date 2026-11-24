package ai.ai4me.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.ai4me.data.db.DbTask
import ai.ai4me.data.db.DbTaskStep
import ai.ai4me.data.db.TaskDao
import ai.ai4me.intelligence.agent.AgentOrchestrator
import ai.ai4me.ui.components.SectionCard
import ai.ai4me.ui.components.StatusBadge
import ai.ai4me.ui.theme.Ai4MeTheme

/**
 * Activity & Task History Screen (Document 3 §4.5)
 * Displays: Task goal, status, time, step breakdown, and observable verification results.
 * Plain language, zero raw developer stack traces.
 */
@Composable
fun ActivityScreen(
    taskDao: TaskDao,
    orchestrator: AgentOrchestrator,
    modifier: Modifier = Modifier
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val executionState by orchestrator.executionState.collectAsState()

    var tasks by remember { mutableStateOf<List<DbTask>>(emptyList()) }

    fun refreshTasks() {
        tasks = taskDao.getAllTasks()
    }

    LaunchedEffect(executionState.isRunning) {
        refreshTasks()
    }

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

        // Live Running Task Indicator
        if (executionState.isRunning && executionState.currentTask != null) {
            val liveTask = executionState.currentTask!!
            SectionCard(title = "Task in Progress") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = liveTask.goal,
                            style = Ai4MeTheme.typography.title,
                            color = colors.textPrimary
                        )
                        Text(
                            text = executionState.activeStepText ?: "Processing...",
                            style = Ai4MeTheme.typography.caption,
                            color = colors.primary
                        )
                    }
                    CircularProgressIndicator(
                        modifier = Modifier.padding(start = spacing.sm),
                        color = colors.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.md))
        }

        // Recorded Task History
        if (tasks.isEmpty() && !executionState.isRunning) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                SectionCard(title = "No Tasks Recorded") {
                    Text(
                        text = "When the agent plans and executes tasks (e.g. system diagnostics, memory recall, notifications), steps and verification evidence will appear here.",
                        style = Ai4MeTheme.typography.body,
                        color = colors.textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tasks) { task ->
                    TaskHistoryCard(task = task, taskDao = taskDao)
                }
            }
        }
    }
}

@Composable
private fun TaskHistoryCard(
    task: DbTask,
    taskDao: TaskDao
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    var steps by remember { mutableStateOf<List<DbTaskStep>>(emptyList()) }

    LaunchedEffect(task.id) {
        steps = taskDao.getTaskSteps(task.id)
    }

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.goal,
                    style = Ai4MeTheme.typography.title,
                    color = colors.textPrimary
                )
                Text(
                    text = task.summary ?: "Outcome recorded",
                    style = Ai4MeTheme.typography.caption,
                    color = colors.textSecondary
                )
            }
            StatusBadge(
                text = task.status.replace("_", " ").uppercase(),
                containerColor = when (task.status) {
                    "completed" -> colors.success.copy(alpha = 0.15f)
                    "failed" -> colors.error.copy(alpha = 0.15f)
                    "needs_user_input" -> colors.warning.copy(alpha = 0.15f)
                    else -> colors.primary.copy(alpha = 0.15f)
                },
                contentColor = when (task.status) {
                    "completed" -> colors.success
                    "failed" -> colors.error
                    "needs_user_input" -> colors.warning
                    else -> colors.primary
                }
            )
        }

        if (steps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = "Steps Executed & Verified:",
                style = Ai4MeTheme.typography.label,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                steps.forEach { step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${step.stepOrder}. ${step.toolName}",
                            style = Ai4MeTheme.typography.caption,
                            color = colors.textPrimary
                        )
                        Text(
                            text = step.verificationEvidence ?: step.status,
                            style = Ai4MeTheme.typography.caption,
                            color = if (step.status == "completed") colors.success else colors.textMuted
                        )
                    }
                }
            }
        }
    }
}
