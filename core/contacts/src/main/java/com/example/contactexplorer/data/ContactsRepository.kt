package com.example.contactexplorer.data

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {
    private val resolver = context.contentResolver

    /**
     * Observes address book modifications with a 300ms debounce window.
     * Prevents query storms and cursor contention during bulk synchronization or multi-contact imports.
     */
    @OptIn(FlowPreview::class)
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
    }.debounce(300.milliseconds)

    /**
     * Retrieves the total count of contacts recorded in the device address book.
     */
    suspend fun getContactCount(): Int = withContext(Dispatchers.IO) {
        val projection = arrayOf(ContactsContract.Contacts._ID)
        resolver.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            cursor.count
        } ?: 0
    }

    /**
     * Loads contacts with explicit pagination boundaries.
     * Prevents OutOfMemory (OOM) exceptions and SQLite CursorWindow exhaustion on enterprise address books
     * containing >2,000 contacts by capping cursor spans and batching Data-table queries in 500-item chunks.
     *
     * @param limit Maximum number of contacts to fetch in this page boundary (defaults to [DEFAULT_PAGE_LIMIT]).
     * @param offset Starting record index for pagination.
     */
    suspend fun loadContacts(
        limit: Int = DEFAULT_PAGE_LIMIT,
        offset: Int = 0
    ): List<Contact> = withContext(Dispatchers.IO) {
        val base = linkedMapOf<String, Contact>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP,
            ContactsContract.Contacts.TIMES_CONTACTED,
        )

        // Resilient query supporting Android O+ bundle args, SQL LIMIT/OFFSET, and in-memory cursor boundaries
        val cursor = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val bundle = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                    )
                }
                resolver.query(ContactsContract.Contacts.CONTENT_URI, projection, bundle, null)
            } else null
        }.getOrNull() ?: runCatching {
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME} ASC LIMIT $limit OFFSET $offset"
            )
        }.getOrNull() ?: resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )

        cursor?.use { c ->
            val idIndex = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val keyIndex = c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val nameIndex = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            val updatedIndex = c.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
            val timesIndex = c.getColumnIndex(ContactsContract.Contacts.TIMES_CONTACTED)

            var currentPosition = 0
            while (c.moveToNext()) {
                if (currentPosition < offset) {
                    currentPosition++
                    continue
                }
                val id = c.getString(idIndex) ?: continue
                base[id] = Contact(
                    id = id,
                    lookupKey = c.getString(keyIndex).orEmpty(),
                    name = c.getString(nameIndex).orEmpty().ifBlank { "Unnamed contact" },
                    lastModified = if (updatedIndex >= 0 && !c.isNull(updatedIndex)) c.getLong(updatedIndex) else 0L,
                    timesContacted = if (timesIndex >= 0 && !c.isNull(timesIndex)) c.getInt(timesIndex) else 0,
                )
                currentPosition++
                if (base.size >= limit) {
                    break
                }
            }
        }

        if (base.isEmpty()) {
            return@withContext emptyList()
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

        // Query Data table in bounded 500-item chunks to respect SQLite IN-clause limits (999) & avoid CursorWindow memory spikes
        val idChunks = base.keys.chunked(DATA_BATCH_CHUNK_SIZE)
        for (chunk in idChunks) {
            val selection = "${ContactsContract.Data.CONTACT_ID} IN (${chunk.joinToString(",") { "?" }})"
            val selectionArgs = chunk.toTypedArray()

            resolver.query(ContactsContract.Data.CONTENT_URI, dataProjection, selection, selectionArgs, null)?.use { dataCursor ->
                val idIndex = dataCursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
                val mimeIndex = dataCursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
                val data1Index = dataCursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
                val data2Index = dataCursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
                val data3Index = dataCursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3)

                while (dataCursor.moveToNext()) {
                    val contactId = dataCursor.getString(idIndex) ?: continue
                    if (contactId !in base) continue
                    val mime = dataCursor.getString(mimeIndex) ?: continue
                    val data1 = dataCursor.getString(data1Index).orEmpty()
                    val data2 = if (dataCursor.isNull(data2Index)) 0 else dataCursor.getInt(data2Index)
                    val data3 = dataCursor.getString(data3Index)

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

    companion object {
        const val DEFAULT_PAGE_LIMIT = 2000
        const val DATA_BATCH_CHUNK_SIZE = 500
    }
}
