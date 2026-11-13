package app.vibedeck.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import app.vibedeck.DisplaySettings
import app.vibedeck.RecentStore
import app.vibedeck.auth.FirebaseAuthRepository
import app.vibedeck.data.remote.FirestoreGameRepository
import app.vibedeck.data.metadata.MetadataFetcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import app.vibedeck.data.GameRepository
import kotlinx.coroutines.flow.*

data class DisplayState(val safeArea: Boolean = true, val orientation: String = "auto", val dark: Boolean = true)
class AppViewModel(context: Context, store: RecentStore) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuthRepository(context, FirebaseAuth.getInstance(), db)
    val repository: GameRepository = FirestoreGameRepository(context, store, db, auth, MetadataFetcher())
    private val settings = DisplaySettings(context)
    private val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val _display = MutableStateFlow(DisplayState(settings.safeArea, settings.orientation, preferences.getBoolean("dark", true)))
    val display = _display.asStateFlow()
    init { repository.refreshLibrary() }
    fun safeArea(value: Boolean) { settings.safeArea = value; _display.update { it.copy(safeArea = value) } }
    fun orientation(value: String) { settings.orientation = value; _display.update { it.copy(orientation = value) } }
    fun dark(value: Boolean) { preferences.edit().putBoolean("dark", value).apply(); _display.update { it.copy(dark = value) } }
    override fun onCleared() { auth.close() }
}
