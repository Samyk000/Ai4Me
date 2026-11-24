package ai.ai4me.automation.messaging

import ai.ai4me.automation.contacts.ContactDirectory
import ai.ai4me.automation.contacts.ContactEntry
import ai.ai4me.automation.contacts.ContactResolver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SendSmsToolTest {

    private val mockDirectory = object : ContactDirectory {
        private val contacts = listOf(
            ContactEntry(id = "1", displayName = "Rahul Sharma", phoneNumber = "+1234567890"),
            ContactEntry(id = "2", displayName = "Rahul Verma", phoneNumber = "+1987654321"),
            ContactEntry(id = "3", displayName = "Alice Smith", phoneNumber = "+1122334455")
        )

        override fun searchContacts(query: String): List<ContactEntry> {
            return contacts.filter { it.displayName.contains(query, ignoreCase = true) }
        }
    }

    private class MockSmsDispatcher : SmsDispatcher {
        var lastNumber: String? = null
        var lastMessage: String? = null
        var dispatchSuccess: Boolean = true

        override fun dispatchSms(phoneNumber: String, message: String): Boolean {
            lastNumber = phoneNumber
            lastMessage = message
            return dispatchSuccess
        }
    }

    private val resolver = ContactResolver(mockDirectory)
    private val dispatcher = MockSmsDispatcher()
    private val tool = SendSmsTool(resolver, dispatcher)

    @Test
    fun `ambiguous contact returns needs_user_input without dispatching sms`() = runTest {
        // Document 1 §3.4: "If more than one Rahul exists, ask which one."
        val args = """{"recipientName":"Rahul","message":"I will call tomorrow"}"""
        val result = tool.execute(args)

        assertFalse(result.success)
        assertEquals("needs_user_input", result.status)
        assertTrue(result.errorMessage?.contains("Multiple contacts found") == true)
        assertTrue(result.errorMessage?.contains("Rahul Sharma") == true)
        assertTrue(result.errorMessage?.contains("Rahul Verma") == true)
        // Ensure no SMS was dispatched
        assertEquals(null, dispatcher.lastNumber)
    }

    @Test
    fun `single contact resolves and reports unverified delivery status`() = runTest {
        // Document 1 §3.4: "If delivery cannot be verified, report 'sent action attempted; delivery unverified,' not 'delivered.'"
        val args = """{"recipientName":"Alice","message":"See you soon"}"""
        val result = tool.execute(args)

        assertTrue(result.success)
        assertEquals("unverified", result.status)
        assertEquals("+1122334455", dispatcher.lastNumber)
        assertEquals("See you soon", dispatcher.lastMessage)
        assertEquals("sent action attempted; delivery unverified", result.verificationEvidence)
    }

    @Test
    fun `not found contact returns needs_user_input`() = runTest {
        val args = """{"recipientName":"Bob","message":"Hello"}"""
        val result = tool.execute(args)

        assertFalse(result.success)
        assertEquals("needs_user_input", result.status)
        assertTrue(result.errorMessage?.contains("could not be found") == true)
    }

    @Test
    fun `invalid json parameters returns failed`() = runTest {
        val result = tool.execute("invalid-json")

        assertFalse(result.success)
        assertEquals("failed", result.status)
    }
}
