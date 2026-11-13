package app.vibedeck.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vibedeck.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class StoreState(val games: List<Game> = emptyList(), val category: String = "ทั้งหมด", val loading: Boolean = false, val error: String? = null, val warning: String? = null)
class StoreViewModel(private val repository: GameRepository) : ViewModel() {
    private val category = MutableStateFlow("ทั้งหมด")
    val state = combine(repository.games, category, repository.catalogStatus) { games, selected, status -> StoreState(games, selected, status.loading, status.error, status.warning) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreState(repository.games.value))
    init { refresh() }
    fun refresh() { viewModelScope.launch {
        try { repository.refreshCatalog(category.value.takeUnless { it == "ทั้งหมด" }) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Repository exposes a retryable error state. */ }
    } }
    fun selectCategory(value: String) { category.value = value; refresh() }
}
