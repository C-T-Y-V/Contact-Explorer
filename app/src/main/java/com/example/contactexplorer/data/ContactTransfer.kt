package com.example.contactexplorer.data

import org.json.JSONArray
import org.json.JSONObject

private class ParsedCard {
    var name: String = ""
    val phones = mutableListOf<LabeledValue>()
    val emails = mutableListOf<LabeledValue>()
    var company: String = ""
    var address: String = ""
    var note: String = ""
    val categories = mutableListOf<String>()
    val tags = mutableListOf<String>()
    var isFavorite: Boolean = false
}

data class VaultData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val contacts: List<ContactDraft>,
    val groups: List<String>,
    val tags: List<String>,
    val smartRules: List<Triple<String, String, String>>, // name, field, value
)

object ContactTransfer {
    private val standardColumns = listOf("name", "phone", "email", "company", "address", "notes")
    private val organizedColumns = listOf("name", "phone", "email", "company", "address", "notes", "groups", "tags", "favorite")

    fun toCsv(
        contacts: List<Contact>,
        groupMap: Map<String, List<String>> = emptyMap(),
        tagMap: Map<String, List<String>> = emptyMap(),
        favorites: Set<String> = emptySet(),
        includeOrganization: Boolean = false,
    ): String = buildString {
        val cols = if (includeOrganization) organizedColumns else standardColumns
        appendLine(cols.joinToString(","))
        contacts.forEach { contact ->
            val phones = (if (contact.phones.isNotEmpty()) contact.phones.map { it.value } else listOfNotNull(contact.phone.takeIf(String::isNotBlank))).joinToString(";")
            val emails = (if (contact.emails.isNotEmpty()) contact.emails.map { it.value } else listOfNotNull(contact.email.takeIf(String::isNotBlank))).joinToString(";")
            val baseList = mutableListOf(contact.name, phones, emails, contact.company, contact.address, contact.note)
            if (includeOrganization) {
                baseList.add(groupMap[contact.id].orEmpty().joinToString(";"))
                baseList.add(tagMap[contact.id].orEmpty().joinToString(";"))
                baseList.add(if (contact.id in favorites) "true" else "false")
            }
            baseList.joinTo(this, separator = ",") { csvEscape(it) }
            appendLine()
        }
    }

    fun fromCsv(source: String): List<ContactDraft> {
        val rows = parseCsv(source).filter { it.any(String::isNotBlank) }
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim().lowercase() }
        val hasHeader = header.firstOrNull() == "name"
        val data = if (hasHeader) rows.drop(1) else rows

        val nameIdx = if (hasHeader) header.indexOf("name").takeIf { it >= 0 } ?: 0 else 0
        val phoneIdx = if (hasHeader) header.indexOf("phone").takeIf { it >= 0 } ?: 1 else 1
        val emailIdx = if (hasHeader) header.indexOf("email").takeIf { it >= 0 } ?: 2 else 2
        val compIdx = if (hasHeader) header.indexOf("company").takeIf { it >= 0 } ?: 3 else 3
        val addrIdx = if (hasHeader) header.indexOf("address").takeIf { it >= 0 } ?: 4 else 4
        val noteIdx = if (hasHeader) header.indexOf("notes").takeIf { it >= 0 } ?: header.indexOf("note").takeIf { it >= 0 } ?: 5 else 5
        val groupsIdx = if (hasHeader) header.indexOf("groups").takeIf { it >= 0 } else null
        val tagsIdx = if (hasHeader) header.indexOf("tags").takeIf { it >= 0 } else null
        val favIdx = if (hasHeader) header.indexOf("favorite").takeIf { it >= 0 } else null

        return data.mapNotNull { row ->
            val name = row.getOrNull(nameIdx)?.trim()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val phoneStr = row.getOrNull(phoneIdx).orEmpty()
            val emailStr = row.getOrNull(emailIdx).orEmpty()
            val phoneList = phoneStr.split(";").map(String::trim).filter(String::isNotBlank).map { LabeledValue(it) }
            val emailList = emailStr.split(";").map(String::trim).filter(String::isNotBlank).map { LabeledValue(it, "Home", 1) }

            val categories = groupsIdx?.let { idx -> row.getOrNull(idx)?.split(";")?.map(String::trim)?.filter(String::isNotBlank) }.orEmpty()
            val tags = tagsIdx?.let { idx -> row.getOrNull(idx)?.split(";")?.map(String::trim)?.filter(String::isNotBlank) }.orEmpty()
            val isFav = favIdx?.let { idx -> row.getOrNull(idx)?.trim()?.equals("true", ignoreCase = true) } ?: false

            ContactDraft(
                name = name,
                phone = phoneList.firstOrNull()?.value ?: phoneStr,
                email = emailList.firstOrNull()?.value ?: emailStr,
                company = row.getOrNull(compIdx).orEmpty(),
                address = row.getOrNull(addrIdx).orEmpty(),
                note = row.getOrNull(noteIdx).orEmpty(),
                phones = phoneList,
                emails = emailList,
                categories = categories,
                tags = tags,
                isFavorite = isFav,
            )
        }
    }

    fun toSingleVcf(contact: Contact): String = toVcf(listOf(contact))

    fun toVcf(
        contacts: List<Contact>,
        groupMap: Map<String, List<String>> = emptyMap(),
        tagMap: Map<String, List<String>> = emptyMap(),
        favorites: Set<String> = emptySet(),
        includeOrganization: Boolean = false,
    ): String = buildString {
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

            if (includeOrganization) {
                val groups = groupMap[contact.id].orEmpty().filter(String::isNotBlank)
                if (groups.isNotEmpty()) {
                    appendLine("CATEGORIES:${groups.joinToString(",") { vcfEscape(it) }}")
                }
                val tags = tagMap[contact.id].orEmpty().filter(String::isNotBlank)
                if (tags.isNotEmpty()) {
                    appendLine("X-TAGS:${tags.joinToString(",") { vcfEscape(it) }}")
                }
                if (contact.id in favorites) {
                    appendLine("X-FAVORITE:TRUE")
                }
            }
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
                        "CATEGORIES", "CATEGORY" -> {
                            val items = value.split(',').map(String::trim).filter(String::isNotBlank)
                            currentCard!!.categories.addAll(items)
                        }
                        "X-TAGS", "X-TAG" -> {
                            val items = value.split(',').map(String::trim).filter(String::isNotBlank)
                            currentCard!!.tags.addAll(items)
                        }
                        "X-FAVORITE" -> {
                            currentCard!!.isFavorite = value.trim().equals("true", ignoreCase = true)
                        }
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
                categories = card.categories.distinct(),
                tags = card.tags.distinct(),
                isFavorite = card.isFavorite,
            )
        }
    }

    fun toVaultJson(
        contacts: List<Contact>,
        groupMap: Map<String, List<String>>,
        tagMap: Map<String, List<String>>,
        favorites: Set<String>,
        groupsList: List<String>,
        tagsList: List<String>,
        smartRules: List<Triple<String, String, String>> = emptyList(),
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("app", "Contact Explorer")

        val groupsArr = JSONArray()
        groupsList.forEach { groupsArr.put(it) }
        root.put("all_groups", groupsArr)

        val tagsArr = JSONArray()
        tagsList.forEach { tagsArr.put(it) }
        root.put("all_tags", tagsArr)

        val rulesArr = JSONArray()
        smartRules.forEach { (name, field, value) ->
            val ruleObj = JSONObject()
            ruleObj.put("name", name)
            ruleObj.put("field", field)
            ruleObj.put("value", value)
            rulesArr.put(ruleObj)
        }
        root.put("smart_rules", rulesArr)

        val contactsArr = JSONArray()
        contacts.forEach { c ->
            val cObj = JSONObject()
            cObj.put("name", c.name)
            cObj.put("phone", c.phone)
            cObj.put("email", c.email)
            cObj.put("company", c.company)
            cObj.put("address", c.address)
            cObj.put("note", c.note)
            cObj.put("isFavorite", c.id in favorites)

            val pArr = JSONArray()
            c.phones.forEach { p ->
                val pObj = JSONObject()
                pObj.put("val", p.value)
                pObj.put("lbl", p.label)
                pObj.put("type", p.type)
                pArr.put(pObj)
            }
            cObj.put("phones", pArr)

            val eArr = JSONArray()
            c.emails.forEach { e ->
                val eObj = JSONObject()
                eObj.put("val", e.value)
                eObj.put("lbl", e.label)
                eObj.put("type", e.type)
                eArr.put(eObj)
            }
            cObj.put("emails", eArr)

            val cgArr = JSONArray()
            groupMap[c.id].orEmpty().forEach { cgArr.put(it) }
            cObj.put("groups", cgArr)

            val ctArr = JSONArray()
            tagMap[c.id].orEmpty().forEach { ctArr.put(it) }
            cObj.put("tags", ctArr)

            contactsArr.put(cObj)
        }
        root.put("contacts", contactsArr)

        return root.toString(2)
    }

    fun fromVaultJson(jsonString: String): VaultData? = runCatching {
        val root = JSONObject(jsonString)
        val allGroups = mutableListOf<String>()
        val gArr = root.optJSONArray("all_groups")
        if (gArr != null) {
            for (i in 0 until gArr.length()) allGroups.add(gArr.getString(i))
        }

        val allTags = mutableListOf<String>()
        val tArr = root.optJSONArray("all_tags")
        if (tArr != null) {
            for (i in 0 until tArr.length()) allTags.add(tArr.getString(i))
        }

        val rules = mutableListOf<Triple<String, String, String>>()
        val rArr = root.optJSONArray("smart_rules")
        if (rArr != null) {
            for (i in 0 until rArr.length()) {
                val rObj = rArr.getJSONObject(i)
                rules.add(Triple(rObj.optString("name"), rObj.optString("field"), rObj.optString("value")))
            }
        }

        val drafts = mutableListOf<ContactDraft>()
        val cArr = root.optJSONArray("contacts")
        if (cArr != null) {
            for (i in 0 until cArr.length()) {
                val cObj = cArr.getJSONObject(i)
                val pList = mutableListOf<LabeledValue>()
                val pArr = cObj.optJSONArray("phones")
                if (pArr != null) {
                    for (j in 0 until pArr.length()) {
                        val p = pArr.getJSONObject(j)
                        pList.add(LabeledValue(p.optString("val"), p.optString("lbl", "Mobile"), p.optInt("type", 2)))
                    }
                }
                val eList = mutableListOf<LabeledValue>()
                val eArr = cObj.optJSONArray("emails")
                if (eArr != null) {
                    for (j in 0 until eArr.length()) {
                        val e = eArr.getJSONObject(j)
                        eList.add(LabeledValue(e.optString("val"), e.optString("lbl", "Home"), e.optInt("type", 1)))
                    }
                }

                val cGroups = mutableListOf<String>()
                val cgArr = cObj.optJSONArray("groups")
                if (cgArr != null) {
                    for (j in 0 until cgArr.length()) cGroups.add(cgArr.getString(j))
                }

                val cTags = mutableListOf<String>()
                val ctArr = cObj.optJSONArray("tags")
                if (ctArr != null) {
                    for (j in 0 until ctArr.length()) cTags.add(ctArr.getString(j))
                }

                drafts.add(
                    ContactDraft(
                        name = cObj.optString("name", "Unnamed contact"),
                        phone = cObj.optString("phone", pList.firstOrNull()?.value.orEmpty()),
                        email = cObj.optString("email", eList.firstOrNull()?.value.orEmpty()),
                        company = cObj.optString("company"),
                        address = cObj.optString("address"),
                        note = cObj.optString("note"),
                        phones = pList,
                        emails = eList,
                        categories = cGroups,
                        tags = cTags,
                        isFavorite = cObj.optBoolean("isFavorite", false),
                    )
                )
            }
        }
        VaultData(
            version = root.optInt("version", 1),
            exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
            contacts = drafts,
            groups = allGroups,
            tags = allTags,
            smartRules = rules,
        )
    }.getOrNull()

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
