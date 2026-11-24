package ai.ai4me.automation.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactResolverTest {

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

    private val resolver = ContactResolver(mockDirectory)

    @Test
    fun `single matching contact returns Resolved`() {
        val result = resolver.resolveContact("Alice")

        assertTrue(result is ContactResolutionResult.Resolved)
        val resolved = result as ContactResolutionResult.Resolved
        assertEquals("Alice Smith", resolved.contact.displayName)
        assertEquals("+1122334455", resolved.contact.phoneNumber)
    }

    @Test
    fun `multiple matching contacts return Ambiguous`() {
        // Document 1 §3.4: "If more than one Rahul exists, ask which one."
        val result = resolver.resolveContact("Rahul")

        assertTrue(result is ContactResolutionResult.Ambiguous)
        val ambiguous = result as ContactResolutionResult.Ambiguous
        assertEquals("Rahul", ambiguous.query)
        assertEquals(2, ambiguous.candidates.size)
        assertTrue(ambiguous.candidates.any { it.displayName == "Rahul Sharma" })
        assertTrue(ambiguous.candidates.any { it.displayName == "Rahul Verma" })
    }

    @Test
    fun `non-existent contact returns NotFound`() {
        val result = resolver.resolveContact("Unknown")

        assertTrue(result is ContactResolutionResult.NotFound)
        assertEquals("Unknown", (result as ContactResolutionResult.NotFound).query)
    }

    @Test
    fun `blank contact query returns NotFound`() {
        val result = resolver.resolveContact("   ")

        assertTrue(result is ContactResolutionResult.NotFound)
    }
}
