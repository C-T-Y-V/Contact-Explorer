package com.example.contactexplorer.data

import kotlin.math.max

data class DuplicateGroup(
    val contacts: List<Contact>,
    val reason: String,
    val confidence: Int,
)

data class KeywordCluster(
    val keyword: String,
    val suggestedCategory: String,
    val contacts: List<Contact>,
)

/** Local-only duplicate matching and deterministic tag suggestions. */
object OfflineIntelligence {
    fun normalizePhone(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    fun findDuplicateGroups(contacts: List<Contact>): List<DuplicateGroup> {
        val pairScores = mutableMapOf<Pair<String, String>, Pair<Int, String>>()

        fun recordMatch(c1: Contact, c2: Contact, score: Int, reason: String) {
            val key = if (c1.id < c2.id) c1.id to c2.id else c2.id to c1.id
            val current = pairScores[key]
            if (current == null || score > current.first) {
                pairScores[key] = score to reason
            }
        }

        val contactMap = contacts.associateBy { it.id }

        // 1. Group by normalized phone numbers across all phones
        val phoneBuckets = mutableMapOf<String, MutableList<Contact>>()
        contacts.forEach { c ->
            val allPhoneNumbers = (c.phones.map { it.value } + listOf(c.phone))
                .map { normalizePhone(it) }
                .filter { it.length >= 7 }
                .distinct()
            allPhoneNumbers.forEach { num ->
                phoneBuckets.getOrPut(num) { mutableListOf() }.add(c)
            }
        }
        phoneBuckets.values.forEach { bucket ->
            val distinct = bucket.distinctBy { it.id }
            for (i in 0 until distinct.size) {
                for (j in i + 1 until distinct.size) {
                    recordMatch(distinct[i], distinct[j], 95, "Identical phone number")
                }
            }
        }

        // 2. Group by normalized email across all emails
        val emailBuckets = mutableMapOf<String, MutableList<Contact>>()
        contacts.forEach { c ->
            val allEmails = (c.emails.map { it.value } + listOf(c.email))
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .distinct()
            allEmails.forEach { email ->
                emailBuckets.getOrPut(email) { mutableListOf() }.add(c)
            }
        }
        emailBuckets.values.forEach { bucket ->
            val distinct = bucket.distinctBy { it.id }
            for (i in 0 until distinct.size) {
                for (j in i + 1 until distinct.size) {
                    recordMatch(distinct[i], distinct[j], 90, "Identical email address")
                }
            }
        }

        // 3. Group by Name
        val nameBuckets = mutableMapOf<String, MutableList<Contact>>()
        contacts.forEach { c ->
            val normName = c.name.trim().lowercase().replace(Regex("\\s+"), " ")
            if (normName.length >= 2 && normName != "unnamed contact") {
                nameBuckets.getOrPut(normName) { mutableListOf() }.add(c)
            }
        }
        nameBuckets.values.forEach { bucket ->
            val distinct = bucket.distinctBy { it.id }
            for (i in 0 until distinct.size) {
                for (j in i + 1 until distinct.size) {
                    val c1 = distinct[i]
                    val c2 = distinct[j]
                    val companyMatch = c1.company.isNotBlank() && c1.company.equals(c2.company, ignoreCase = true)
                    val score = if (companyMatch) 85 else 75
                    val reason = if (companyMatch) "Same name and company" else "Identical name"
                    recordMatch(c1, c2, score, reason)
                }
            }
        }

        // Build connected components for groups
        val adjacency = mutableMapOf<String, MutableSet<String>>()
        val pairReasons = mutableMapOf<String, String>()
        val pairMaxConfidence = mutableMapOf<String, Int>()

        pairScores.forEach { (pair, scoreAndReason) ->
            val (score, reason) = scoreAndReason
            if (score >= 60) {
                adjacency.getOrPut(pair.first) { mutableSetOf() }.add(pair.second)
                adjacency.getOrPut(pair.second) { mutableSetOf() }.add(pair.first)
                pairReasons[pair.first] = reason
                pairReasons[pair.second] = reason
                pairMaxConfidence[pair.first] = max(pairMaxConfidence[pair.first] ?: 0, score)
                pairMaxConfidence[pair.second] = max(pairMaxConfidence[pair.second] ?: 0, score)
            }
        }

        val visited = mutableSetOf<String>()
        val result = mutableListOf<DuplicateGroup>()

        adjacency.keys.forEach { startId ->
            if (startId !in visited) {
                val groupIds = mutableListOf<String>()
                val queue = ArrayDeque<String>()
                queue.add(startId)
                visited.add(startId)
                while (queue.isNotEmpty()) {
                    val curr = queue.removeFirst()
                    groupIds.add(curr)
                    adjacency[curr].orEmpty().forEach { neighbor ->
                        if (neighbor !in visited) {
                            visited.add(neighbor)
                            queue.add(neighbor)
                        }
                    }
                }
                if (groupIds.size > 1) {
                    val memberContacts = groupIds.mapNotNull { contactMap[it] }
                    if (memberContacts.size > 1) {
                        val maxScore = groupIds.maxOfOrNull { pairMaxConfidence[it] ?: 70 } ?: 70
                        val reason = groupIds.firstNotNullOfOrNull { pairReasons[it] } ?: "Potential duplicate"
                        result.add(DuplicateGroup(memberContacts, reason, maxScore))
                    }
                }
            }
        }

        return result.sortedByDescending { it.confidence }
    }

    fun duplicateGroups(contacts: List<Contact>): List<List<Contact>> {
        return findDuplicateGroups(contacts).map { it.contacts }
    }

    fun suggestedTags(contact: Contact): Set<String> = buildSet {
        val allPhones = (contact.phones.map { it.value } + listOf(contact.phone)).joinToString(" ")
        val allEmails = (contact.emails.map { it.value } + listOf(contact.email)).joinToString(" ")
        val text = listOf(contact.name, contact.company, allEmails, contact.address, contact.note, allPhones)
            .joinToString(" ").lowercase()
        val keywords = mapOf(
            "developer" to listOf("developer", "engineer", "software", "programmer"),
            "college" to listOf("college", "university", "student", "campus"),
            "work" to listOf("work", "office", "manager", "project"),
            "family" to listOf("mom", "dad", "sister", "brother", "family", "mother", "father"),
            "emergency" to listOf("emergency", "doctor", "hospital", "police"),
        )
        keywords.forEach { (tag, words) -> if (words.any(text::contains)) add(tag) }
        val emailDomain = allEmails.split(" ", ",").mapNotNull { it.substringAfter('@', "").takeIf(String::isNotBlank) }.firstOrNull()
        emailDomain?.let { add("domain:$it") }
        contact.company.trim().takeIf(String::isNotBlank)?.let { add("company:${it.lowercase()}") }
    }

    fun detectKeywordClusters(contacts: List<Contact>): List<KeywordCluster> {
        val clusters = mutableListOf<KeywordCluster>()
        val seenCategories = mutableSetOf<String>()

        val semanticRules = listOf(
            Pair("Legal & Advocates", listOf("adv", "advocate", "lawyer", "legal", "vakil")),
            Pair("Class & College", listOf("class", "classmate", "college", "school", "batch", "batchmate", "tuition", "coaching", "academy")),
            Pair("Medical & Doctors", listOf("dr", "doctor", "doc", "clinic", "dentist", "pharma", "hospital")),
            Pair("Services & Home", listOf("plumber", "electrician", "carpenter", "driver", "mechanic", "maid", "cook", "painter")),
            Pair("Real Estate", listOf("flat", "broker", "dealer", "agent", "realtor", "property", "builder", "rent")),
            Pair("Gym & Fitness", listOf("gym", "fitness", "trainer", "coach", "workout", "yoga")),
            Pair("Corporate & Work", listOf("office", "work", "corp", "manager", "boss", "client", "vendor", "tcs", "wipro", "infosys", "accenture")),
            Pair("Family", listOf("mom", "dad", "mother", "father", "bhai", "sister", "brother", "chacha", "mama", "uncle", "aunty"))
        )

        for ((category, keywords) in semanticRules) {
            val matching = contacts.filter { c ->
                val tokens = (c.name + " " + c.company + " " + c.note)
                    .lowercase()
                    .split(Regex("[^a-z0-9]+"))
                    .filter(String::isNotBlank)
                keywords.any { kw -> kw in tokens }
            }
            if (matching.size >= 2) {
                clusters.add(KeywordCluster(keywords.first(), category, matching))
                seenCategories.add(category)
            }
        }

        val stopWords = setOf(
            "the", "and", "mr", "mrs", "shri", "kumar", "singh", "sharma", "patel",
            "gupta", "verma", "reddy", "rao", "contact", "new", "home", "cell",
            "work", "call", "test", "sim", "airtel", "jio", "phone"
        )
        val tokenMap = mutableMapOf<String, MutableList<Contact>>()
        contacts.forEach { c ->
            val tokens = c.name.lowercase()
                .split(Regex("[^a-z0-9]+"))
                .filter { it.length >= 3 && it !in stopWords }
                .distinct()
            tokens.forEach { t ->
                tokenMap.getOrPut(t) { mutableListOf() }.add(c)
            }
        }

        tokenMap.filter { (_, list) ->
            list.size >= 2 && clusters.none { it.contacts.containsAll(list) }
        }.forEach { (token, list) ->
            val prettyName = token.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            if (clusters.none { it.suggestedCategory.equals(prettyName, ignoreCase = true) }) {
                clusters.add(KeywordCluster(token, prettyName, list))
            }
        }

        return clusters.sortedByDescending { it.contacts.size }
    }

    fun findContactsByKeyword(contacts: List<Contact>, keyword: String): List<Contact> {
        val clean = keyword.trim().lowercase()
        if (clean.isBlank()) return emptyList()
        return contacts.filter { c ->
            val nameTokens = c.name.lowercase().split(Regex("[^a-z0-9]+"))
            val companyTokens = c.company.lowercase().split(Regex("[^a-z0-9]+"))
            val noteTokens = c.note.lowercase().split(Regex("[^a-z0-9]+"))
            clean in nameTokens || clean in companyTokens || clean in noteTokens ||
                c.name.contains(clean, ignoreCase = true) ||
                c.company.contains(clean, ignoreCase = true) ||
                c.note.contains(clean, ignoreCase = true)
        }
    }
}

fun contactRankScore(contact: Contact, favorite: Boolean, viewedAt: Long?, now: Long = System.currentTimeMillis()): Int {
    val recency = viewedAt?.let { (1 - ((now - it).coerceAtLeast(0) / (30.0 * 24 * 60 * 60 * 1000))).coerceAtLeast(0.0) } ?: 0.0
    return contact.timesContacted.coerceAtMost(100) * 2 + (if (favorite) 50 else 0) + (recency * 100).toInt()
}
