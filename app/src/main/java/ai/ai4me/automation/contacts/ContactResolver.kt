package ai.ai4me.automation.contacts

import android.content.Context
import android.provider.ContactsContract

data class ContactEntry(
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val email: String? = null
)

sealed class ContactResolutionResult {
    data class Resolved(val contact: ContactEntry) : ContactResolutionResult()
    data class Ambiguous(val query: String, val candidates: List<ContactEntry>) : ContactResolutionResult()
    data class NotFound(val query: String) : ContactResolutionResult()
}

interface ContactDirectory {
    fun searchContacts(query: String): List<ContactEntry>
}

class SystemContactDirectory(private val context: Context) : ContactDirectory {
    override fun searchContacts(query: String): List<ContactEntry> {
        val results = mutableListOf<ContactEntry>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getString(idIdx) else ""
                    val name = if (nameIdx >= 0) it.getString(nameIdx) else ""
                    val num = if (numIdx >= 0) it.getString(numIdx) else ""
                    if (name.isNotBlank() && num.isNotBlank()) {
                        results.add(ContactEntry(id = id, displayName = name, phoneNumber = num))
                    }
                }
            }
        } catch (_: Exception) {
            // Permission not granted or query failure handled gracefully
        }
        return results
    }
}

/**
 * Contact Resolver (Document 1 §3.4)
 * "Tell Rahul I'll call tomorrow."
 * If more than one Rahul exists, ask which one.
 */
class ContactResolver(private val directory: ContactDirectory) {

    fun resolveContact(query: String): ContactResolutionResult {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return ContactResolutionResult.NotFound(query)

        val candidates = directory.searchContacts(trimmed)
        return when {
            candidates.isEmpty() -> ContactResolutionResult.NotFound(query)
            candidates.size == 1 -> ContactResolutionResult.Resolved(candidates.first())
            else -> ContactResolutionResult.Ambiguous(query, candidates)
        }
    }
}
