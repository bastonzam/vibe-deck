package app.vibedeck.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vibedeck.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class LibraryScreenState(val library: LibraryState = LibraryState(), val favoritesTab: Boolean = false, val error: String? = null)
class LibraryViewModel(private val repository: GameRepository) : ViewModel() {
    private val favoritesTab = MutableStateFlow(false)
    private val attempted = mutableSetOf<String>()
    private val error = MutableStateFlow<String?>(null)
    val state = combine(repository.library, favoritesTab, error) { library, tab, message -> LibraryScreenState(library, tab, message) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryScreenState(repository.library.value))
    fun selectTab(favorites: Boolean) { favoritesTab.value = favorites }
    fun toggleFavorite(url: String) { repository.toggleFavorite(url) }
    fun refresh() {
        repository.refreshLibrary()
        // Render domain cards immediately; enrich old URL-only history in the background.
        viewModelScope.launch {
            error.value = null
            try { repository.enrichLibrary() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error.value = "อัปเดตข้อมูลจาก Store ไม่สำเร็จ ยังเปิดเกมในคลังได้" }
            val unknown = (repository.library.value.recent + repository.library.value.favorites).distinctBy { it.url }
                .filter { it.submittedBy.isBlank() && it.thumbnailUrl.isBlank() && attempted.add(it.url) }
            unknown.forEach { game ->
                try { repository.preview(game.url) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* A domain card remains usable when a site is offline. */ }
            }
        }
    }
}
