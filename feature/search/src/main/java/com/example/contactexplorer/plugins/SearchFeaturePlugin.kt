package com.example.contactexplorer.plugins

import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.SearchEngine
import com.example.contactexplorer.data.SearchRequest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

private data class CachedContactIndex(
    val contactId: String,
    val lastModified: Long,
    val normalizedName: String,
    val allPhones: List<String>,
    val digitsOnlyPhones: List<String>,
    val allEmails: List<String>,
    val company: String,
    val city: String,
    val address: String,
    val note: String,
    val nameTokens: List<String>,
    val allTokens: Set<String>,
)

class KotlinSearchEngine : SearchEngine {
    private val aliases = mapOf(
        "work" to "company", "office" to "company", "college" to "group",
        "school" to "group", "family" to "group", "friend" to "group",
        "mobile" to "phone", "cell" to "phone", "mail" to "email",
    )

    private val indexCache = ConcurrentHashMap<String, CachedContactIndex>()

    private fun getOrCreateIndex(contact: Contact): CachedContactIndex {
        val cached = indexCache[contact.id]
        if (cached != null && cached.lastModified == contact.lastModified) {
            return cached
        }

        val phones = (contact.phones.map { it.value } + listOf(contact.phone))
            .filter(String::isNotBlank).distinct()
        val digitsOnly = phones.map { it.filter(Char::isDigit) }.filter(String::isNotBlank)
        val emails = (contact.emails.map { it.value.lowercase() } + listOf(contact.email.lowercase()))
            .filter(String::isNotBlank).distinct()

        val normName = contact.name.lowercase()
        val normComp = contact.company.lowercase()
        val normAddr = contact.address.lowercase()
        val normNote = contact.note.lowercase()
        val city = normAddr.substringBefore(',').trim()

        val nameTokens = normName.split(Regex("[^a-z0-9]+")).filter(String::isNotBlank)
        val allTokens = buildSet {
            addAll(nameTokens)
            addAll(normComp.split(Regex("[^a-z0-9]+")).filter(String::isNotBlank))
            addAll(normAddr.split(Regex("[^a-z0-9]+")).filter(String::isNotBlank))
            addAll(normNote.split(Regex("[^a-z0-9]+")).filter(String::isNotBlank))
            addAll(digitsOnly)
            emails.forEach { e ->
                add(e)
                addAll(e.split(Regex("[^a-z0-9]+")).filter(String::isNotBlank))
            }
        }

        val index = CachedContactIndex(
            contactId = contact.id,
            lastModified = contact.lastModified,
            normalizedName = normName,
            allPhones = phones,
            digitsOnlyPhones = digitsOnly,
            allEmails = emails,
            company = normComp,
            city = city,
            address = normAddr,
            note = normNote,
            nameTokens = nameTokens,
            allTokens = allTokens,
        )
        indexCache[contact.id] = index
        return index
    }

    override fun search(request: SearchRequest): List<Contact> {
        val tokens = request.query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (tokens.isEmpty()) return request.contacts

        val filters = tokens.mapNotNull { token ->
            val split = token.split(':', limit = 2)
            if (split.size == 2 && split[1].isNotBlank()) (aliases[split[0].lowercase()] ?: split[0].lowercase()) to split[1].lowercase() else null
        }
        val free = tokens.filterNot { it.contains(':') }.map { it.lowercase() }

        return request.contacts.mapNotNull { contact ->
            val idx = getOrCreateIndex(contact)
            val groups = request.groupNames[contact.id].orEmpty().map { it.lowercase() }
            val tags = request.tagNames[contact.id].orEmpty().map { it.lowercase() }

            // Field-specific filtering
            if (filters.isNotEmpty()) {
                val matchesAllFilters = filters.all { (key, value) ->
                    when (key) {
                        "name" -> idx.normalizedName.contains(value)
                        "phone" -> {
                            val qDigits = value.filter(Char::isDigit)
                            idx.allPhones.any { it.contains(value) } ||
                                (qDigits.isNotBlank() && idx.digitsOnlyPhones.any { it.contains(qDigits) })
                        }
                        "email" -> idx.allEmails.any { it.contains(value) }
                        "company" -> idx.company.contains(value)
                        "city" -> idx.city.contains(value)
                        "address" -> idx.address.contains(value)
                        "note" -> idx.note.contains(value)
                        "group" -> groups.any { it.contains(value) }
                        "tag" -> tags.any { it.contains(value) }
                        else -> {
                            idx.normalizedName.contains(value) ||
                                idx.allPhones.any { it.contains(value) } ||
                                idx.allEmails.any { it.contains(value) } ||
                                idx.company.contains(value)
                        }
                    }
                }
                if (!matchesAllFilters) return@mapNotNull null
            }

            // Free-text scoring across all fields
            var textScore = 0
            if (free.isNotEmpty()) {
                val searchableTextList = mutableListOf<String>().apply {
                    add(idx.normalizedName)
                    addAll(idx.allPhones)
                    addAll(idx.allEmails)
                    if (idx.company.isNotBlank()) add(idx.company)
                    if (idx.address.isNotBlank()) add(idx.address)
                    if (idx.note.isNotBlank()) add(idx.note)
                    addAll(groups)
                    addAll(tags)
                }

                for (word in free) {
                    val wordDigits = word.filter(Char::isDigit)
                    var bestScore = 0

                    // Fast check in tokens
                    if (word in idx.allTokens) {
                        bestScore = 100
                    } else if (wordDigits.length >= 3 && idx.digitsOnlyPhones.any { it.contains(wordDigits) }) {
                        bestScore = 110
                    } else {
                        for (field in searchableTextList) {
                            val score = relevance(word, field)
                            if (score > bestScore) {
                                bestScore = score
                                if (score >= 100) break
                            }
                        }
                    }

                    if (bestScore == 0) return@mapNotNull null
                    textScore += bestScore
                }
            }

            val smartScore = (if (contact.id in request.favorites) 20 else 0) +
                (request.recency[contact.id] ?: 0) * 4 + contact.timesContacted.coerceAtMost(50)

            contact to (textScore + smartScore)
        }.sortedWith(compareByDescending<Pair<Contact, Int>> { it.second }.thenBy { it.first.name.lowercase() }).map { it.first }
    }

    private fun relevance(query: String, field: String): Int {
        if (field == query) return 120
        if (field.startsWith(query)) return 100
        if (field.contains(query)) return 80
        if (query.length < 4) return 0

        val tokens = field.split(Regex("[^a-z0-9@.+]+"))
        return tokens.maxOfOrNull { candidate ->
            val distance = editDistance(query, candidate)
            if (distance <= max(1, query.length / 4)) 55 - distance * 10 else 0
        } ?: 0
    }

    private fun editDistance(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        if ((a.length - b.length).let { if (it < 0) -it else it } > 2) return 99
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) current[j + 1] = minOf(
                current[j] + 1,
                previous[j + 1] + 1,
                previous[j] + if (a[i] == b[j]) 0 else 1,
            )
            previous = current
        }
        return previous[b.length]
    }
}

class SearchFeaturePlugin(private val engine: SearchEngine) : ExplorerPlugin {
    override val id = "search"
    private val feature = FeatureDescriptor(id, "Search", "Find contacts by name, number, company, group, or tag.", FeatureCategory.SEARCH)
    override val features = listOf(feature)

    override fun register(registry: ExplorerPluginRegistry.Builder) {
        registry.feature(feature) { service("search.engine", SearchEngine::class, engine) }
    }
}
