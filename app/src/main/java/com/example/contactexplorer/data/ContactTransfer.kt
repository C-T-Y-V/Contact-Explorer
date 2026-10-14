package com.example.contactexplorer.data

private class ParsedCard {
    var name: String = ""
    val phones = mutableListOf<LabeledValue>()
    val emails = mutableListOf<LabeledValue>()
    var company: String = ""
    var address: String = ""
    var note: String = ""
}

object ContactTransfer {
    private val columns = listOf("name", "phone", "email", "company", "address", "notes")

    fun toCsv(contacts: List<Contact>): String = buildString {
        appendLine(columns.joinToString(","))
        contacts.forEach { contact ->
            val phones = (if (contact.phones.isNotEmpty()) contact.phones.map { it.value } else listOfNotNull(contact.phone.takeIf(String::isNotBlank))).joinToString(";")
            val emails = (if (contact.emails.isNotEmpty()) contact.emails.map { it.value } else listOfNotNull(contact.email.takeIf(String::isNotBlank))).joinToString(";")
            listOf(contact.name, phones, emails, contact.company, contact.address, contact.note)
                .joinTo(this, separator = ",") { csvEscape(it) }
            appendLine()
        }
    }

    fun fromCsv(source: String): List<ContactDraft> {
        val rows = parseCsv(source).filter { it.any(String::isNotBlank) }
        if (rows.isEmpty()) return emptyList()
        val hasHeader = rows.first().firstOrNull()?.trim()?.equals("name", ignoreCase = true) == true
        val data = if (hasHeader) rows.drop(1) else rows
        return data.mapNotNull { row ->
            val values = row + List((6 - row.size).coerceAtLeast(0)) { "" }
            values.firstOrNull()?.trim()?.takeIf(String::isNotBlank)?.let { name ->
                val phoneStr = values.getOrElse(1) { "" }
                val emailStr = values.getOrElse(2) { "" }
                val phoneList = phoneStr.split(";").map(String::trim).filter(String::isNotBlank).map { LabeledValue(it) }
                val emailList = emailStr.split(";").map(String::trim).filter(String::isNotBlank).map { LabeledValue(it, "Home", 1) }
                ContactDraft(
                    name = name,
                    phone = phoneList.firstOrNull()?.value ?: phoneStr,
                    email = emailList.firstOrNull()?.value ?: emailStr,
                    company = values.getOrElse(3) { "" },
                    address = values.getOrElse(4) { "" },
                    note = values.getOrElse(5) { "" },
                    phones = phoneList,
                    emails = emailList,
                )
            }
        }
    }

    fun toSingleVcf(contact: Contact): String = toVcf(listOf(contact))

    fun toVcf(contacts: List<Contact>): String = buildString {
        contacts.forEach { contact ->
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("FN:${vcfEscape(contact.name)}")
            appendLine("N:${vcfEscape(contact.name)};;;;")
            val phones = if (contact.phones.isNotEmpty()) contact.phones else if (contact.phone.isNotBlank()) listOf(LabeledValue(contact.phone)) else emptyList()
            phones.forEach { p ->
                val type = when (p.type) {
                    android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "HOME"
                    android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "WORK"
                    else -> "CELL"
                }
                appendLine("TEL;TYPE=$type:${vcfEscape(p.value)}")
            }
            val emails = if (contact.emails.isNotEmpty()) contact.emails else if (contact.email.isNotBlank()) listOf(LabeledValue(contact.email)) else emptyList()
            emails.forEach { e ->
                val type = when (e.type) {
                    android.provider.ContactsContract.CommonDataKinds.Email.TYPE_WORK -> "WORK"
                    else -> "INTERNET"
                }
                appendLine("EMAIL;TYPE=$type:${vcfEscape(e.value)}")
            }
            contact.company.takeIf(String::isNotBlank)?.let { appendLine("ORG:${vcfEscape(it)}") }
            contact.address.takeIf(String::isNotBlank)?.let { appendLine("ADR;TYPE=HOME:;;${vcfEscape(it)};;;;") }
            contact.note.takeIf(String::isNotBlank)?.let { appendLine("NOTE:${vcfEscape(it)}") }
            appendLine("END:VCARD")
        }
    }

    fun fromVcf(source: String): List<ContactDraft> {
        val lines = mutableListOf<String>()
        source.replace("\r\n", "\n").lineSequence().forEach { line ->
            if ((line.startsWith(' ') || line.startsWith('\t')) && lines.isNotEmpty()) lines[lines.lastIndex] += line.drop(1)
            else lines += line
        }
        val cards = mutableListOf<ParsedCard>()
        var currentCard: ParsedCard? = null
        lines.forEach { line ->
            when {
                line.equals("BEGIN:VCARD", true) -> currentCard = ParsedCard()
                line.equals("END:VCARD", true) -> { currentCard?.let { cards += it }; currentCard = null }
                currentCard != null && ':' in line -> {
                    val keyPart = line.substringBefore(':')
                    val key = keyPart.substringBefore(';').uppercase()
                    val value = vcfUnescape(line.substringAfter(':'))
                    val params = keyPart.substringAfter(';', "").uppercase()
                    when (key) {
                        "FN" -> if (currentCard!!.name.isBlank()) currentCard!!.name = value
                        "N" -> if (currentCard!!.name.isBlank()) currentCard!!.name = value.substringBefore(';').replace(";", " ").trim()
                        "TEL" -> {
                            val type = when {
                                "HOME" in params -> android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_HOME
                                "WORK" in params -> android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                                else -> android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                            }
                            val label = when {
                                "HOME" in params -> "Home"
                                "WORK" in params -> "Work"
                                else -> "Mobile"
                            }
                            currentCard!!.phones.add(LabeledValue(value, label, type))
                        }
                        "EMAIL" -> {
                            val type = when {
                                "WORK" in params -> android.provider.ContactsContract.CommonDataKinds.Email.TYPE_WORK
                                else -> android.provider.ContactsContract.CommonDataKinds.Email.TYPE_HOME
                            }
                            val label = when {
                                "WORK" in params -> "Work"
                                else -> "Home"
                            }
                            currentCard!!.emails.add(LabeledValue(value, label, type))
                        }
                        "ORG" -> if (currentCard!!.company.isBlank()) currentCard!!.company = value
                        "ADR" -> if (currentCard!!.address.isBlank()) currentCard!!.address = value.trim(';').replace(";;", "").replace(";", ", ")
                        "NOTE" -> if (currentCard!!.note.isBlank()) currentCard!!.note = value
                    }
                }
            }
        }
        return cards.mapNotNull { card ->
            val name = card.name.ifBlank { "Unnamed contact" }
            if (card.name.isBlank() && card.phones.isEmpty() && card.emails.isEmpty()) null
            else ContactDraft(
                name = name,
                phone = card.phones.firstOrNull()?.value.orEmpty(),
                email = card.emails.firstOrNull()?.value.orEmpty(),
                company = card.company,
                address = card.address,
                note = card.note,
                phones = card.phones,
                emails = card.emails,
            )
        }
    }

    private fun csvEscape(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${value.replace("\"", "\"\"")}\"" else value
    private fun vcfEscape(value: String): String = value.replace("\\", "\\\\").replace("\n", "\\n").replace(",", "\\,").replace(";", "\\;")
    private fun vcfUnescape(value: String): String = value.replace("\\n", "\n", true).replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

    private fun parseCsv(source: String): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < source.length) {
            val ch = source[i]
            when {
                ch == '"' && quoted && i + 1 < source.length && source[i + 1] == '"' -> { cell.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> { row += cell.toString(); cell.clear() }
                (ch == '\n' || ch == '\r') && !quoted -> {
                    row += cell.toString(); cell.clear(); rows += row; row = mutableListOf()
                    if (ch == '\r' && i + 1 < source.length && source[i + 1] == '\n') i++
                }
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row += cell.toString(); rows += row }
        return rows
    }
}
