package ai.ai4me.automation.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import ai.ai4me.intelligence.agent.PromptInjectionGuard

data class CapturedNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val isOngoing: Boolean
)

interface NotificationProvider {
    val isListenerActive: Boolean
    fun getCapturedNotifications(): List<CapturedNotification>
    fun getUntrustedNotificationEnvelope(): String
}

class NotificationMonitorService : NotificationListenerService(), NotificationProvider {

    companion object {
        var activeInstance: NotificationMonitorService? = null
            private set
    }

    override val isListenerActive: Boolean
        get() = activeInstance != null

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeInstance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (activeInstance == this) {
            activeInstance = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeInstance == this) {
            activeInstance = null
        }
    }

    override fun getCapturedNotifications(): List<CapturedNotification> {
        val sbns: Array<StatusBarNotification> = try {
            activeNotifications ?: emptyArray()
        } catch (_: Exception) {
            emptyArray()
        }

        return sbns.mapNotNull { sbn ->
            val extras = sbn.notification.extras ?: return@mapNotNull null
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

            if (title.isBlank() && text.isBlank()) null
            else CapturedNotification(
                key = sbn.key ?: "${sbn.packageName}_${sbn.postTime}",
                packageName = sbn.packageName.orEmpty(),
                title = title,
                text = text,
                postTime = sbn.postTime,
                isOngoing = sbn.isOngoing
            )
        }
    }

    override fun getUntrustedNotificationEnvelope(): String {
        val notifications = getCapturedNotifications()
        if (notifications.isEmpty()) {
            return PromptInjectionGuard.wrapObservation(
                source = "NotificationListenerService",
                rawContent = "No active notifications"
            )
        }

        val rawSummary = notifications.joinToString("\n") { n ->
            "App: ${n.packageName} | Title: ${n.title} | Text: ${n.text}"
        }

        return PromptInjectionGuard.wrapObservation(
            source = "NotificationListenerService",
            rawContent = rawSummary
        )
    }
}
