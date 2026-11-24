package ai.ai4me.voice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ai.ai4me.MainActivity
import ai.ai4me.voice.wakeword.LocalWakeWordDetector
import ai.ai4me.voice.wakeword.WakeWordEngine

/**
 * Foreground Wake-Word Listening Service (Document 1 §3.1, §8 & Document 2 §8)
 * Strictly complies with Android 14+ background microphone restrictions:
 * - Uses FOREGROUND_SERVICE_TYPE_MICROPHONE
 * - Displays a persistent notification with visible listening state
 * - Provides an immediate [STOP] action to revoke microphone use
 */
class WakeWordForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "ai4me_wake_word_channel"
        const val NOTIFICATION_ID = 4040
        const val ACTION_START = "ai.ai4me.action.START_WAKE_WORD"
        const val ACTION_STOP = "ai.ai4me.action.STOP_WAKE_WORD"

        private var _isRunning = false
        val isRunning: Boolean get() = _isRunning

        fun start(context: Context) {
            val intent = Intent(context, WakeWordForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WakeWordForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val wakeWordEngine: WakeWordEngine = LocalWakeWordDetector()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopListeningAndExit()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startListeningInForeground()
            }
            else -> {
                startListeningInForeground()
            }
        }
        return START_STICKY
    }

    private fun startListeningInForeground() {
        _isRunning = true
        val notification = buildPersistentNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        wakeWordEngine.startListening {
            // Keyword spotted: Launch or signal MainActivity to open voice session
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("trigger_voice", true)
            }
            startActivity(launchIntent)
        }
    }

    private fun stopListeningAndExit() {
        _isRunning = false
        wakeWordEngine.stopListening()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildPersistentNotification(): Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, WakeWordForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ai4Me Active: Listening for Wake-Word")
            .setContentText("Say \"Hey Ai4Me\" to activate • Running locally")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(appPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Listening",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ai4Me Background Listening",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active listening status when wake-word detection is enabled."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        _isRunning = false
        wakeWordEngine.stopListening()
    }
}
