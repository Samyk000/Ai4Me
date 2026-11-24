package ai.ai4me

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ai.ai4me.automation.apps.AppLauncher
import ai.ai4me.automation.apps.LaunchAppTool
import ai.ai4me.automation.contacts.ContactResolver
import ai.ai4me.automation.contacts.SystemContactDirectory
import ai.ai4me.automation.messaging.SendSmsTool
import ai.ai4me.automation.messaging.SmsDispatcher
import ai.ai4me.data.db.Ai4MeDatabaseHelper
import ai.ai4me.data.db.ConversationDao
import ai.ai4me.data.db.MemoryDao
import ai.ai4me.data.db.TaskDao
import ai.ai4me.data.db.ToolAuditDao
import ai.ai4me.data.models.ModelCatalogRepository
import ai.ai4me.data.models.ModelDownloadManager
import ai.ai4me.hardware.HardwareCapabilityScanner
import ai.ai4me.intelligence.agent.AgentOrchestrator
import ai.ai4me.intelligence.agent.tools.GetDeviceTelemetryTool
import ai.ai4me.intelligence.agent.tools.ReadNotificationsTool
import ai.ai4me.intelligence.agent.tools.SearchMemoryTool
import ai.ai4me.intelligence.agent.tools.StoreMemoryTool
import ai.ai4me.intelligence.llm.ModelRuntimeManager
import ai.ai4me.intelligence.memory.MemoryRetriever
import ai.ai4me.ui.navigation.Ai4MeNavHost
import ai.ai4me.ui.theme.Ai4MeTheme
import ai.ai4me.voice.focus.AudioFocusController
import ai.ai4me.voice.loop.VoiceConversationCoordinator
import ai.ai4me.voice.stt.LocalSpeechRecognizer
import ai.ai4me.voice.tts.LocalTextToSpeechEngine

class MainActivity : ComponentActivity() {

    private lateinit var voiceCoordinator: VoiceConversationCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val scanner = HardwareCapabilityScanner(applicationContext)
        val spec = scanner.scanDevice()
        val recommendation = scanner.evaluateRecommendation(spec)

        val catalogRepository = ModelCatalogRepository(applicationContext)
        val downloadManager = ModelDownloadManager(applicationContext)
        val runtimeManager = ModelRuntimeManager()

        // Database and DAOs (Document 1 §4.7 & Document 2 §5.1)
        val dbHelper = Ai4MeDatabaseHelper(applicationContext)
        val taskDao = TaskDao(dbHelper)
        val toolAuditDao = ToolAuditDao(dbHelper)
        val memoryDao = MemoryDao(dbHelper)
        val memoryRetriever = MemoryRetriever(memoryDao)

        // Agent Orchestrator & Tool Registry (Document 1 §5.6 & Document 2 §6)
        val contactResolver = ContactResolver(SystemContactDirectory(applicationContext))
        val smsDispatcher = object : SmsDispatcher {
            override fun dispatchSms(phoneNumber: String, message: String): Boolean {
                return try {
                    val uri = Uri.parse("smsto:$phoneNumber")
                    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                        putExtra("sms_body", message)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    applicationContext.startActivity(intent)
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }
        val appLauncher = object : AppLauncher {
            override fun launchApp(appName: String): Result<String> {
                val pm = packageManager
                val intent = pm.getLaunchIntentForPackage(appName)
                    ?: pm.getInstalledApplications(0).firstOrNull {
                        pm.getApplicationLabel(it).toString().contains(appName, ignoreCase = true)
                    }?.let { pm.getLaunchIntentForPackage(it.packageName) }

                return if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    Result.success(intent.component?.packageName ?: appName)
                } else {
                    Result.failure(Exception("No launchable application found for '$appName'"))
                }
            }
        }

        val orchestrator = AgentOrchestrator(
            taskDao = taskDao,
            toolAuditDao = toolAuditDao,
            memoryRetriever = memoryRetriever
        ).apply {
            registerTool(GetDeviceTelemetryTool(scanner))
            registerTool(SearchMemoryTool(memoryRetriever))
            registerTool(StoreMemoryTool(memoryDao))
            registerTool(ReadNotificationsTool())
            registerTool(SendSmsTool(contactResolver, smsDispatcher))
            registerTool(LaunchAppTool(appLauncher))
        }

        // Voice Engine & Interruption Loop (Document 1 §3.2 & Document 2 §8)
        val sttProvider = LocalSpeechRecognizer(applicationContext)
        val ttsProvider = LocalTextToSpeechEngine(applicationContext)
        val audioFocusController = AudioFocusController(applicationContext) {
            voiceCoordinator.stopVoiceSession()
        }

        voiceCoordinator = VoiceConversationCoordinator(
            sttProvider = sttProvider,
            ttsProvider = ttsProvider,
            audioFocusController = audioFocusController,
            runtimeManager = runtimeManager,
            orchestrator = orchestrator
        )

        setContent {
            Ai4MeTheme {
                Ai4MeNavHost(
                    hardwareSpec = spec,
                    recommendation = recommendation,
                    catalogRepository = catalogRepository,
                    downloadManager = downloadManager,
                    runtimeManager = runtimeManager,
                    orchestrator = orchestrator,
                    taskDao = taskDao,
                    voiceCoordinator = voiceCoordinator
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceCoordinator.stopVoiceSession()
    }
}
