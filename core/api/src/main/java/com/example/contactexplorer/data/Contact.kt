package com.example.contactexplorer.data

data class LabeledValue(
    val value: String,
    val label: String = "Mobile",
    val type: Int = 2,
    val customLabel: String? = null,
)

data class Contact(
    val id: String,
    val lookupKey: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val company: String = "",
    val address: String = "",
    val note: String = "",
    val lastModified: Long = 0L,
    val timesContacted: Int = 0,
    val phones: List<LabeledValue> = if (phone.isNotBlank()) listOf(LabeledValue(phone)) else emptyList(),
    val emails: List<LabeledValue> = if (email.isNotBlank()) listOf(LabeledValue(email, "Home", 1)) else emptyList(),
) {
    val primaryPhone: String get() = phone.ifBlank { phones.firstOrNull()?.value.orEmpty() }
    val primaryEmail: String get() = email.ifBlank { emails.firstOrNull()?.value.orEmpty() }
}

data class ContactDraft(
    val name: String,
    val phone: String = "",
    val email: String = "",
    val company: String = "",
    val address: String = "",
    val note: String = "",
    val phones: List<LabeledValue> = if (phone.isNotBlank()) listOf(LabeledValue(phone)) else emptyList(),
    val emails: List<LabeledValue> = if (email.isNotBlank()) listOf(LabeledValue(email, "Home", 1)) else emptyList(),
) {
    val effectivePhones: List<LabeledValue>
        get() = if (phones.isNotEmpty()) phones else if (phone.isNotBlank()) listOf(LabeledValue(phone)) else emptyList()

    val effectiveEmails: List<LabeledValue>
        get() = if (emails.isNotEmpty()) emails else if (email.isNotBlank()) listOf(LabeledValue(email, "Home", 1)) else emptyList()
}
