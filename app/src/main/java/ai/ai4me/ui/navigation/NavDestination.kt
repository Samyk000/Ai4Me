package ai.ai4me.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Default.GraphicEq),
    CHAT("chat", "Chat", Icons.Default.ChatBubbleOutline),
    MODELS("models", "Models", Icons.Default.DownloadDone),
    ACTIVITY("activity", "Activity", Icons.Default.History),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}
