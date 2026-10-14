package com.example.contactexplorer.data

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {
    private val resolver = context.contentResolver

    fun observeContactsChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(Unit)
            }
        }
        val registered = try {
            resolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
            true
        } catch (_: Exception) {
            false
        }
        awaitClose {
            if (registered) {
                try {
                    resolver.unregisterContentObserver(observer)
                } catch (_: Exception) {
                }
            }
        }
    }

    suspend fun loadContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val base = linkedMapOf<String, Contact>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP,
            ContactsContract.Contacts.TIMES_CONTACTED,
        )
        resolver.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val keyIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            val updatedIndex = cursor.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
            val timesIndex = cursor.getColumnIndex(ContactsContract.Contacts.TIMES_CONTACTED)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex) ?: continue
                base[id] = Contact(
                    id = id,
                    lookupKey = cursor.getString(keyIndex).orEmpty(),
                    name = cursor.getString(nameIndex).orEmpty().ifBlank { "Unnamed contact" },
                    lastModified = if (updatedIndex >= 0 && !cursor.isNull(updatedIndex)) cursor.getLong(updatedIndex) else 0L,
                    timesContacted = if (timesIndex >= 0 && !cursor.isNull(timesIndex)) cursor.getInt(timesIndex) else 0,
                )
            }
        }

        val dataProjection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA3,
        )

        val contactPhones = mutableMapOf<String, MutableList<LabeledValue>>()
        val contactEmails = mutableMapOf<String, MutableList<LabeledValue>>()
        val contactCompanies = mutableMapOf<String, String>()
        val contactAddresses = mutableMapOf<String, String>()
        val contactNotes = mutableMapOf<String, String>()

        resolver.query(ContactsContract.Data.CONTENT_URI, dataProjection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val mimeIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val data1Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            val data2Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
            val data3Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3)

            while (cursor.moveToNext()) {
                val contactId = cursor.getString(idIndex) ?: continue
                if (contactId !in base) continue
                val mime = cursor.getString(mimeIndex) ?: continue
                val data1 = cursor.getString(data1Index).orEmpty()
                val data2 = if (cursor.isNull(data2Index)) 0 else cursor.getInt(data2Index)
                val data3 = cursor.getString(data3Index)

                when (mime) {
                    Phone.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            val label = Phone.getTypeLabel(context.resources, data2, data3).toString()
                            val list = contactPhones.getOrPut(contactId) { mutableListOf() }
                            if (list.none { it.value == data1 }) {
                                list.add(LabeledValue(value = data1, label = label, type = data2, customLabel = data3))
                            }
                        }
                    }
                    Email.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            val label = Email.getTypeLabel(context.resources, data2, data3).toString()
                            val list = contactEmails.getOrPut(contactId) { mutableListOf() }
                            if (list.none { it.value.equals(data1, ignoreCase = true) }) {
                                list.add(LabeledValue(value = data1, label = label, type = data2, customLabel = data3))
                            }
                        }
                    }
                    Organization.CONTENT_ITEM_TYPE -> {
                        if (contactCompanies[contactId].isNullOrBlank() && data1.isNotBlank()) {
                            contactCompanies[contactId] = data1
                        }
                    }
                    StructuredPostal.CONTENT_ITEM_TYPE -> {
                        if (contactAddresses[contactId].isNullOrBlank() && data1.isNotBlank()) {
                            contactAddresses[contactId] = data1
                        }
                    }
                    Note.CONTENT_ITEM_TYPE -> {
                        if (contactNotes[contactId].isNullOrBlank() && data1.isNotBlank()) {
                            contactNotes[contactId] = data1
                        }
                    }
                }
            }
        }

        base.mapValues { (id, contact) ->
            val phones = contactPhones[id].orEmpty()
            val emails = contactEmails[id].orEmpty()
            contact.copy(
                phone = phones.firstOrNull()?.value.orEmpty(),
                email = emails.firstOrNull()?.value.orEmpty(),
                company = contactCompanies[id].orEmpty(),
                address = contactAddresses[id].orEmpty(),
                note = contactNotes[id].orEmpty(),
                phones = phones,
                emails = emails,
            )
        }.values.toList()
    }

    suspend fun saveContact(contactId: String?, draft: ContactDraft): String = withContext(Dispatchers.IO) {
        require(draft.name.isNotBlank()) { "A contact name is required." }
        val accountRawId = if (contactId == null) null else findWritableRawContact(contactId)
        if (contactId != null && accountRawId == null) throw IllegalStateException("This contact has no editable address-book entry.")
        val operations = ArrayList<ContentProviderOperation>()
        if (accountRawId == null) {
            operations += ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
            appendContactRows(operations, 0, draft)
        } else {
            val ownedTypes = arrayOf(
                StructuredName.CONTENT_ITEM_TYPE,
                Phone.CONTENT_ITEM_TYPE,
                Email.CONTENT_ITEM_TYPE,
                Organization.CONTENT_ITEM_TYPE,
                StructuredPostal.CONTENT_ITEM_TYPE,
                Note.CONTENT_ITEM_TYPE,
            )
            operations += ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(
                    "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE} IN (${ownedTypes.joinToString(",") { "?" }})",
                    arrayOf(accountRawId.toString(), *ownedTypes),
                ).build()
            appendContactRows(operations, accountRawId, draft)
        }
        val results = resolver.applyBatch(ContactsContract.AUTHORITY, operations)
        contactId ?: results.firstOrNull()?.uri?.let { rawUri ->
            val rawId = ContentUris.parseId(rawUri)
            resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                "${ContactsContract.RawContacts._ID}=?",
                arrayOf(rawId.toString()),
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        } ?: throw IllegalStateException("The contact was saved but could not be reloaded yet.")
    }

    private fun findWritableRawContact(contactId: String): Long? {
        val rawProjection = arrayOf(ContactsContract.RawContacts._ID)
        resolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            rawProjection,
            "${ContactsContract.RawContacts.CONTACT_ID}=? AND ${ContactsContract.RawContacts.DELETED}=0",
            arrayOf(contactId),
            null,
        )?.use { cursor -> if (cursor.moveToFirst()) return cursor.getLong(0) }
        return null
    }

    private fun appendContactRows(operations: MutableList<ContentProviderOperation>, rawId: Long, draft: ContactDraft) {
        fun row(mime: String, values: Map<String, Any?>) {
            val builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.MIMETYPE, mime)
            if (rawId == 0L) builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            else builder.withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
            values.forEach { (key, value) -> builder.withValue(key, value) }
            operations += builder.build()
        }

        row(StructuredName.CONTENT_ITEM_TYPE, mapOf(StructuredName.DISPLAY_NAME to draft.name))

        draft.effectivePhones.filter { it.value.isNotBlank() }.distinctBy { it.value }.forEach { p ->
            val map = mutableMapOf<String, Any?>(
                Phone.NUMBER to p.value,
                Phone.TYPE to (if (p.type != 0) p.type else Phone.TYPE_MOBILE)
            )
            if (p.type == Phone.TYPE_CUSTOM && !p.customLabel.isNullOrBlank()) {
                map[Phone.LABEL] = p.customLabel
            }
            row(Phone.CONTENT_ITEM_TYPE, map)
        }

        draft.effectiveEmails.filter { it.value.isNotBlank() }.distinctBy { it.value.lowercase() }.forEach { e ->
            val map = mutableMapOf<String, Any?>(
                Email.ADDRESS to e.value,
                Email.TYPE to (if (e.type != 0) e.type else Email.TYPE_HOME)
            )
            if (e.type == Email.TYPE_CUSTOM && !e.customLabel.isNullOrBlank()) {
                map[Email.LABEL] = e.customLabel
            }
            row(Email.CONTENT_ITEM_TYPE, map)
        }

        if (draft.company.isNotBlank()) row(Organization.CONTENT_ITEM_TYPE, mapOf(Organization.COMPANY to draft.company, Organization.TYPE to Organization.TYPE_WORK))
        if (draft.address.isNotBlank()) row(StructuredPostal.CONTENT_ITEM_TYPE, mapOf(StructuredPostal.FORMATTED_ADDRESS to draft.address, StructuredPostal.TYPE to StructuredPostal.TYPE_HOME))
        if (draft.note.isNotBlank()) row(Note.CONTENT_ITEM_TYPE, mapOf(Note.NOTE to draft.note))
    }

    suspend fun deleteContact(contactId: String): Int = withContext(Dispatchers.IO) {
        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
        resolver.delete(uri, null, null)
    }
}
