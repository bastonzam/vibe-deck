package app.vibedeck.data

import android.content.Context
import app.vibedeck.RecentStore
import app.vibedeck.UrlPolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest
import app.vibedeck.data.model.*
import app.vibedeck.data.metadata.*

interface GameRepository {
    val games: StateFlow<List<Game>>
    val library: StateFlow<LibraryState>
    val searches: StateFlow<List<String>>
    fun find(id: String): Game?
    fun gameForUrl(url: String): Game
    fun refreshLibrary()
    fun toggleFavorite(url: String)
    fun saveSearch(query: String)
    fun clearSearches()
    suspend fun preview(url: String): Game
    suspend fun existingGame(url: String): Game? = null
    fun queueSubmission(game: Game)
    val catalogStatus: StateFlow<CatalogStatus> get() = MutableStateFlow(CatalogStatus())
    suspend fun refreshCatalog(tag: String? = null) {}
    suspend fun enrichLibrary() {}
    suspend fun saveGame(draft: GameDraft): SaveGameResult = error("ใช้ได้เมื่อเชื่อมต่อ Firestore")
    fun observeGame(id: String): Flow<Game?> = flowOf(find(id))
    fun observeRating(id: String, uid: String): Flow<Int?> = flowOf(null)
    suspend fun rateGame(id: String, value: Int?) { error("ใช้ได้เมื่อเชื่อมต่อ Firestore") }
    suspend fun comments(id: String, cursor: String? = null): CommentPage = CommentPage(emptyList(), null, false)
    suspend fun addComment(id: String, text: String) { error("ใช้ได้เมื่อเชื่อมต่อ Firestore") }
    suspend fun deleteComment(id: String, commentId: String) { error("ใช้ได้เมื่อเชื่อมต่อ Firestore") }
    suspend fun reportGame(id: String, reason: String, commentId: String? = null) { error("ใช้ได้เมื่อเชื่อมต่อ Firestore") }
}

/** Catalog is mock; library, search history, metadata and pending submissions are local. */
class FakeGameRepository(context: Context, private val store: RecentStore, initialGames: List<Game> = SampleGames.all) : GameRepository {
    private val preferences = context.getSharedPreferences("game_metadata", Context.MODE_PRIVATE)
    private val _games = MutableStateFlow(initialGames)
    override val games = _games.asStateFlow()
    private val _library = MutableStateFlow(LibraryState())
    override val library = _library.asStateFlow()
    private val _searches = MutableStateFlow(preferences.getString("searches", "").orEmpty().split('\n').filter(String::isNotBlank))
    override val searches = _searches.asStateFlow()
    private val external = mutableMapOf<String, Game>()

    override fun find(id: String): Game? = games.value.find { it.id == id } ?: external.values.find { it.id == id }

    override fun refreshLibrary() {
        _library.value = LibraryState(store.recent().map(::gameForUrl), store.favorites().map(::gameForUrl))
    }

    override fun gameForUrl(url: String): Game {
        games.value.find { it.url == url }?.let { return it }
        external[url]?.let { return it }
        val cached = runCatching { JSONObject(preferences.getString("metadata:$url", "")!!) }.getOrNull()
        val host = runCatching { URI(url).host }.getOrNull() ?: url
        val id = "web-" + MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).take(12).joinToString("") { "%02x".format(it) }
        return Game(id = id, title = cached?.optString("title")?.takeIf(String::isNotBlank) ?: host,
            url = url, thumbnailUrl = cached?.optString("image").orEmpty(),
            description = cached?.optString("description")?.takeIf(String::isNotBlank) ?: "เกมจาก $host · เพิ่มจากลิงก์ของคุณ",
            developerName = host).also { external[url] = it }
    }

    override suspend fun preview(url: String): Game {
        val normalized = UrlPolicy.normalize(url) ?: error("กรุณาใส่ลิงก์ http หรือ https ที่ถูกต้อง")
        games.value.find { it.url == normalized }?.let { return it }
        val existing = gameForUrl(normalized)
        val metadata = when (val result = MetadataFetcher().fetch(normalized)) {
            is MetadataResult.Success -> result.metadata
            is MetadataResult.PartialSuccess -> result.metadata
            is MetadataResult.Failure -> error(result.message)
        }
        val game = existing.copy(title = metadata.title.ifBlank { existing.title }, thumbnailUrl = metadata.imageUrl,
            description = metadata.description.ifBlank { existing.description })
        external[normalized] = game
        preferences.edit().putString("metadata:$normalized", JSONObject().put("title", game.title)
            .put("image", game.thumbnailUrl).put("description", game.description).toString()).apply()
        refreshLibrary()
        return game
    }

    override fun toggleFavorite(url: String) { store.toggleFavorite(url); refreshLibrary() }
    override fun saveSearch(query: String) {
        val clean = query.trim().replace('\n', ' ').take(100)
        if (clean.isEmpty()) return
        _searches.value = (listOf(clean) + searches.value.filterNot { it == clean }).take(8)
        preferences.edit().putString("searches", searches.value.joinToString("\n")).apply()
    }
    override fun clearSearches() {
        _searches.value = emptyList()
        preferences.edit().remove("searches").apply()
    }
    override fun queueSubmission(game: Game) {
        preferences.edit().putString("submission:${game.id}", game.url).apply()
    }

    fun cacheExternal(game: Game) {
        external[game.url] = game
        preferences.edit().putString("metadata:${game.url}", JSONObject().put("title", game.title)
            .put("image", game.thumbnailUrl).put("description", game.description).toString()).apply()
        refreshLibrary()
    }
}
