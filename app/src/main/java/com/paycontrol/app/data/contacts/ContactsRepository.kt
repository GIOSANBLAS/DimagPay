package com.paycontrol.app.data.contacts

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContact(
    val id: Long,
    val name: String,
    val phone: String,
    val sourceLabel: String
)

/**
 * Contactos del dispositivo, incluyendo cuentas Google sincronizadas.
 * Solo lee DISPLAY_NAME y NUMBER; no modifica la agenda.
 */
class ContactsRepository(
    private val contentResolver: ContentResolver
) {

    suspend fun searchContacts(query: String = "", limit: Int = 200): List<DeviceContact> =
        withContext(Dispatchers.IO) {
            val safeLimit = limit.coerceIn(1, 500)
            val results = linkedMapOf<Long, DeviceContact>()

            val selection: String?
            val args: Array<String>?
            if (query.isBlank()) {
                selection = null
                args = null
            } else {
                val sanitized = query.trim().take(64).replace("%", "").replace("_", "")
                selection =
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ? OR " +
                        "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
                val pattern = "%$sanitized%"
                args = arrayOf(pattern, pattern)
            }

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                args,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (idIdx < 0 || nameIdx < 0 || phoneIdx < 0) return@use

                while (cursor.moveToNext() && results.size < safeLimit) {
                    val id = cursor.getLongOrNull(idIdx) ?: continue
                    if (results.containsKey(id)) continue
                    val name = cursor.getStringOrEmpty(nameIdx).trim()
                    if (name.isBlank()) continue
                    val phone = cursor.getStringOrEmpty(phoneIdx).trim()
                    results[id] = DeviceContact(
                        id = id,
                        name = name.take(80),
                        phone = phone.take(32),
                        sourceLabel = "Agenda"
                    )
                }
            }

            // Resolve account labels only for the limited result set (avoids full RawContacts scan).
            val accountTypes = loadAccountTypesFor(results.keys)
            results.map { (id, contact) ->
                contact.copy(sourceLabel = accountLabel(accountTypes[id]))
            }
        }

    private fun loadAccountTypesFor(contactIds: Set<Long>): Map<Long, String?> {
        if (contactIds.isEmpty()) return emptyMap()
        val map = HashMap<Long, String?>()
        // Cap lookups; IN clauses with huge sets are avoided by iterating small result sets only.
        contactIds.take(500).forEach { contactId ->
            contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.ACCOUNT_TYPE),
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )?.use { cursor ->
                val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                while (cursor.moveToNext()) {
                    val type = if (typeIdx >= 0) cursor.getString(typeIdx) else null
                    val existing = map[contactId]
                    if (existing == null || type?.contains("google", ignoreCase = true) == true) {
                        map[contactId] = type
                    }
                }
            }
        }
        return map
    }

    fun accountLabel(accountType: String?): String = when {
        accountType.isNullOrBlank() -> "Local"
        accountType.contains("google", ignoreCase = true) -> "Google"
        accountType.contains("whatsapp", ignoreCase = true) -> "WhatsApp"
        else -> "Sincronizado"
    }

    private fun Cursor.getLongOrNull(index: Int): Long? =
        if (isNull(index)) null else getLong(index)

    private fun Cursor.getStringOrEmpty(index: Int): String =
        if (isNull(index)) "" else getString(index).orEmpty()
}
