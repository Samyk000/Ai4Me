package ai.ai4me.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import ai.ai4me.hardware.DeviceHardwareSpec
import ai.ai4me.hardware.HardwareRecommendation
import ai.ai4me.ui.components.PermissionRow
import ai.ai4me.ui.components.SectionCard
import ai.ai4me.ui.components.StatusBadge
import ai.ai4me.ui.theme.Ai4MeTheme
import ai.ai4me.voice.service.WakeWordForegroundService

/**
 * Settings Screen (Document 3 §4.4)
 * Organized by user goal:
 * 1. Hardware & System Diagnostics
 * 2. Device Permissions (Microphone, Notifications, Accessibility)
 * 3. Autonomy & Privacy
 */
@Composable
fun SettingsScreen(
    hardwareSpec: DeviceHardwareSpec,
    recommendation: HardwareRecommendation,
    modifier: Modifier = Modifier
) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing
    val context = LocalContext.current

    val hasMicPermission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.lg)
    ) {
        Text(
            text = "Settings & Diagnostics",
            style = Ai4MeTheme.typography.headline,
            color = colors.textPrimary
        )

        // Hardware Diagnostics Section (Document 1 §2 & Document 3 §4.4)
        SectionCard(title = "Hardware Telemetry (Live)") {
            HardwareMetricRow(
                label = "Installed RAM",
                value = "${String.format("%.1f", hardwareSpec.memory.totalRamGb)} GB (${String.format("%.1f", hardwareSpec.memory.availableRamGb)} GB Free)"
            )
            HardwareMetricRow(
                label = "CPU Architecture",
                value = "${hardwareSpec.cpuCoreCount} Cores (${if (hardwareSpec.is64Bit) "64-bit ABI" else "32-bit ABI"})"
            )
            HardwareMetricRow(
                label = "Internal Storage",
                value = "${String.format("%.1f", hardwareSpec.storage.freeGb)} GB Free of ${String.format("%.1f", hardwareSpec.storage.totalGb)} GB"
            )
            HardwareMetricRow(
                label = "Thermal Status",
                value = hardwareSpec.thermalState.label
            )
            HardwareMetricRow(
                label = "Battery",
                value = "${hardwareSpec.battery.levelPercent}% (${if (hardwareSpec.battery.isCharging) "Charging" else "On Battery"})"
            )
            HardwareMetricRow(
                label = "Android API",
                value = "API ${hardwareSpec.androidApiLevel}"
            )

            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = "Recommended Profile: ${recommendation.recommendedProfile.title}",
                style = Ai4MeTheme.typography.label,
                color = colors.primary
            )
            Text(
                text = recommendation.recommendedProfile.description,
                style = Ai4MeTheme.typography.caption,
                color = colors.textSecondary
            )
        }

        // Background Wake-Word Detection (Document 1 §3.1, §8)
        SectionCard(title = "Background Wake-Word Detection") {
            var isWakeWordActive by remember { mutableStateOf(WakeWordForegroundService.isRunning) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Text(
                            text = "Keyword Spotting (\"Hey Ai4Me\")",
                            style = Ai4MeTheme.typography.label,
                            color = colors.textPrimary
                        )
                        StatusBadge(
                            text = if (isWakeWordActive) "Active" else "Stopped",
                            containerColor = if (isWakeWordActive) colors.success.copy(alpha = 0.15f) else colors.surfaceVariant,
                            contentColor = if (isWakeWordActive) colors.success else colors.textSecondary
                        )
                    }
                    Text(
                        text = "Listens locally for activation keyword using low-power audio framing without cloud models.",
                        style = Ai4MeTheme.typography.caption,
                        color = colors.textSecondary
                    )
                }

                Switch(
                    checked = isWakeWordActive,
                    onCheckedChange = { enable ->
                        if (enable) {
                            if (hasMicPermission) {
                                WakeWordForegroundService.start(context)
                                isWakeWordActive = true
                            } else {
                                openAppSettings(context)
                            }
                        } else {
                            WakeWordForegroundService.stop(context)
                            isWakeWordActive = false
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = "Android 14+ Lifecycle Behavior: A persistent notification with a quick-stop action will appear. Android shows a visible green microphone privacy indicator while active. The OS may terminate background services under severe thermal or memory pressure.",
                style = Ai4MeTheme.typography.caption,
                color = colors.textMuted
            )
        }

        // Contextual Permissions (Document 3 §4.4)
        Text(
            text = "Device Permissions",
            style = Ai4MeTheme.typography.title,
            color = colors.textPrimary
        )

        PermissionRow(
            title = "Microphone (Local Voice)",
            description = "Enables offline local speech recognition (STT) and wake-word spotting. Audio is processed strictly on-device.",
            isGranted = hasMicPermission,
            onActionClick = {
                openAppSettings(context)
            }
        )

        PermissionRow(
            title = "Notifications",
            description = "Allows the persistent foreground service notification required by Android for background wake-word listening.",
            isGranted = hasNotificationPermission,
            onActionClick = {
                openAppSettings(context)
            }
        )

        PermissionRow(
            title = "Accessibility Automation",
            description = "Permits the agent to assist with on-screen navigation and structured interactions. Always asks confirmation for sensitive actions.",
            isGranted = false,
            onActionClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }
        )

        // Privacy & Autonomy Policies
        SectionCard(title = "Privacy & Data Policies") {
            Text(
                text = "• Zero Cloud AI Inference: All thinking, reasoning, and speech synthesis occur entirely on-device.",
                style = Ai4MeTheme.typography.body,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = "• Zero Analytics Telemetry: No user prompts, conversations, or device actions are transmitted to any server.",
                style = Ai4MeTheme.typography.body,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun HardwareMetricRow(label: String, value: String) {
    val colors = Ai4MeTheme.colors
    val spacing = Ai4MeTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = Ai4MeTheme.typography.caption,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = Ai4MeTheme.typography.caption,
            color = colors.textPrimary
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
