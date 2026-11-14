package com.example.contactexplorer.feature.calling

import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactEvent

data class CallLogEntry(
    val id: Long,
    val contactId: String?,
    val contactName: String,
    val action: String,
    val details: String,
    val timestamp: Long
)

/**
 * Enterprise Call Log & Telephony Audit Trail Manager.
 * Filters, records, and transforms call interaction events within the local SQLite data vault.
 */
object CallLogManager {

    const val ACTION_CALLED = "CALLED"
    const val ACTION_SMS = "SMS"

    fun filterCallEvents(events: List<ContactEvent>): List<CallLogEntry> {
        return events
            .filter { it.action == ACTION_CALLED }
            .map {
                CallLogEntry(
                    id = it.id,
                    contactId = it.contactId,
                    contactName = it.contactName,
                    action = it.action,
                    details = it.details,
                    timestamp = it.occurredAt
                )
            }
    }

    fun createCallEvent(
        contact: Contact,
        details: String = "Call started.",
        timestamp: Long = System.currentTimeMillis()
    ): ContactEvent {
        return ContactEvent(
            contactId = contact.id,
            contactName = contact.name,
            action = ACTION_CALLED,
            details = details,
            occurredAt = timestamp
        )
    }

    fun createSmsEvent(
        contact: Contact,
        details: String = "Message dispatched.",
        timestamp: Long = System.currentTimeMillis()
    ): ContactEvent {
        return ContactEvent(
            contactId = contact.id,
            contactName = contact.name,
            action = ACTION_SMS,
            details = details,
            occurredAt = timestamp
        )
    }
}
