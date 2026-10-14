package com.example.contactexplorer.data

data class SearchRequest(
    val query: String,
    val contacts: List<Contact>,
    val groupNames: Map<String, List<String>> = emptyMap(),
    val tagNames: Map<String, List<String>> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val recency: Map<String, Int> = emptyMap(),
)

interface SearchEngine {
    fun search(request: SearchRequest): List<Contact>
}
