package app.vibedeck.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vibedeck.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class SearchFilters(val query: String = "", val category: String = "ทั้งหมด", val tool: String = "ทุกเครื่องมือ", val orientation: String = "ทุกแนวจอ", val collection: String = "")
data class SearchState(val filters: SearchFilters = SearchFilters(), val results: List<Game> = emptyList(), val history: List<String> = emptyList(), val loading: Boolean = false, val error: String? = null, val warning: String? = null)

class SearchViewModel(private val repository: GameRepository, collection: String = "", category: String = "ทั้งหมด") : ViewModel() {
    private val filters = MutableStateFlow(SearchFilters(collection = collection, category = category))
    val state = combine(repository.games, repository.searches, filters, repository.catalogStatus) { games, history, filter, status ->
        var matches = games.filter { game ->
            (filter.query.isBlank() || listOf(game.title, game.developerName, game.tags.joinToString(), game.aiTools.joinToString()).any { it.contains(filter.query.trim(), true) }) &&
                (filter.category == "ทั้งหมด" || filter.category in game.tags) &&
                (filter.tool == "ทุกเครื่องมือ" || filter.tool in game.aiTools) &&
                (filter.orientation == "ทุกแนวจอ" || game.orientation == filter.orientation) &&
                (filter.collection != "เลือกโดยทีมงาน" || game.isVerified)
        }
        matches = when (filter.collection) { "มาใหม่" -> matches.sortedByDescending { it.createdAtMillis }; "ยอดนิยม" -> matches.sortedByDescending { it.rating }; else -> matches }
        SearchState(filter, matches, history, status.loading, status.error, status.warning)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchState(filters.value))
    fun query(value: String) { filters.update { it.copy(query = value.take(100)) } }
    init { refresh() }
    fun refresh() { viewModelScope.launch {
        try { repository.refreshCatalog(filters.value.category.takeUnless { it == "ทั้งหมด" }) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { }
    } }
    fun category(value: String) { filters.update { it.copy(category = value) }; refresh() }
    fun tool(value: String) { filters.update { it.copy(tool = value) } }
    fun orientation(value: String) { filters.update { it.copy(orientation = value) } }
    fun clearFilters() { filters.value = SearchFilters(query = filters.value.query) }
    fun saveSearch() { repository.saveSearch(filters.value.query) }
    fun clearHistory() { repository.clearSearches() }
}
