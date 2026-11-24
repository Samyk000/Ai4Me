package ai.ai4me

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        val orchestrator = AgentOrchestrator(
            taskDao = taskDao,
            toolAuditDao = toolAuditDao,
            memoryRetriever = memoryRetriever
        ).apply {
            registerTool(GetDeviceTelemetryTool(scanner))
            registerTool(SearchMemoryTool(memoryRetriever))
            registerTool(StoreMemoryTool(memoryDao))
            registerTool(ReadNotificationsTool())
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
