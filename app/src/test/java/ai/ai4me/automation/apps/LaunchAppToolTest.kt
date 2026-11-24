package ai.ai4me.automation.apps

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchAppToolTest {

    private val mockLauncher = object : AppLauncher {
        override fun launchApp(appName: String): Result<String> {
            return when (appName.lowercase()) {
                "clock" -> Result.success("com.google.android.deskclock")
                "settings" -> Result.success("com.android.settings")
                else -> Result.failure(Exception("App not found"))
            }
        }
    }

    private val tool = LaunchAppTool(mockLauncher)

    @Test
    fun `launching existing app succeeds with package name`() = runTest {
        val result = tool.execute("""{"appName":"Clock"}""")

        assertTrue(result.success)
        assertEquals("completed", result.status)
        assertTrue(result.verificationEvidence.contains("com.google.android.deskclock"))
    }

    @Test
    fun `launching non-existent app fails gracefully`() = runTest {
        val result = tool.execute("""{"appName":"NonExistentGame"}""")

        assertFalse(result.success)
        assertEquals("failed", result.status)
        assertTrue(result.errorMessage?.contains("Could not launch app") == true)
    }

    @Test
    fun `empty app name fails validation`() = runTest {
        val result = tool.execute("""{"appName":""}""")

        assertFalse(result.success)
        assertEquals("failed", result.status)
    }
}
