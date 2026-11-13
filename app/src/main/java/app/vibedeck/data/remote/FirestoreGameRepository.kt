package app.vibedeck.data.remote

import android.content.Context
import app.vibedeck.RecentStore
import app.vibedeck.UrlPolicy
import app.vibedeck.auth.AuthRepository
import app.vibedeck.auth.ProfileSyncException
import app.vibedeck.data.Game
import app.vibedeck.data.GameRepository
import app.vibedeck.data.FakeGameRepository
import app.vibedeck.data.LibraryState
import app.vibedeck.data.CatalogRefresher
import app.vibedeck.data.metadata.*
import app.vibedeck.data.model.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.net.URI

class FirestoreGameRepository(context: Context, store: RecentStore, private val db: FirebaseFirestore,
    private val auth: AuthRepository, private val metadata: MetadataFetcher) : GameRepository {
    private val collection = db.collection("games")
    private val local = FakeGameRepository(context, store, emptyList())
    private val known = linkedMapOf<String, Game>()
    private val _games = MutableStateFlow<List<Game>>(emptyList())
    override val games = _games.asStateFlow()
    private val _library = MutableStateFlow(LibraryState())
    override val library = _library.asStateFlow()
    override val searches = local.searches
    private val _status = MutableStateFlow(CatalogStatus())
    override val catalogStatus = _status.asStateFlow()
    private val catalog = CatalogRefresher(_games, _status, { it.id }, ::queryCatalog,
        onLoaded = { loaded -> loaded.forEach(::remember) }, describeError = { error ->
            val code = (error as? FirebaseFirestoreException)?.code
            android.util.Log.w("VibeDeckCatalog", "Catalog refresh failed: ${code?.name ?: error.javaClass.simpleName}")
            when (code) {
                FirebaseFirestoreException.Code.FAILED_PRECONDITION -> "Firestore ยังไม่พร้อมสำหรับรายการนี้ ตรวจ composite indexes ใน Firebase Console"
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Firestore ไม่อนุญาตให้อ่านรายการนี้ ตรวจ API และ Rules ของ games"
                else -> "ตรวจการเชื่อมต่อแล้วลองอีกครั้ง"
            }
        })

    override fun find(id: String): Game? = known[id] ?: local.find(id)
    override fun gameForUrl(url: String): Game {
        val normalized = GameIdentity.canonicalUrl(UrlPolicy.normalize(url) ?: error("ลิงก์ไม่ถูกต้อง"))
        return known[GameIdentity.id(normalized)] ?: local.gameForUrl(normalized).copy(id = GameIdentity.id(normalized))
            .also { known[it.id] = it }
    }
    private fun remember(game: Game) {
        known[game.id] = game
        local.cacheExternal(game)
        refreshLibrary()
    }
    override fun refreshLibrary() {
        local.refreshLibrary()
        val state = local.library.value
        fun resolved(games: List<Game>) = games.mapNotNull { runCatching { gameForUrl(it.url) }.getOrNull() }.distinctBy { it.url }
        _library.value = LibraryState(resolved(state.recent), resolved(state.favorites))
    }
    override fun toggleFavorite(url: String) { local.toggleFavorite(url); refreshLibrary() }
    override suspend fun enrichLibrary() {
        val ids = (library.value.recent + library.value.favorites).map { it.id }.distinct()
        ids.chunked(30).forEach { chunk ->
            collection.whereEqualTo("status", "approved").whereIn(FieldPath.documentId(), chunk).get().await()
                .documents.mapNotNull(::decodeGame).forEach(::remember)
        }
    }
    override fun saveSearch(query: String) = local.saveSearch(query)
    override fun clearSearches() = local.clearSearches()
    override fun queueSubmission(game: Game) = local.queueSubmission(game)

    override suspend fun preview(url: String): Game {
        val base = gameForUrl(url)
        val result = metadata.fetch(base.url)
        val data = when (result) {
            is MetadataResult.Success -> result.metadata
            is MetadataResult.PartialSuccess -> result.metadata
            is MetadataResult.Failure -> error(result.message)
        }
        return base.copy(title = data.title, description = data.description, thumbnailUrl = data.imageUrl).also(::remember)
    }

    /** Both queries have matching indexes. Tag queries are available for catalog expansion. */
    suspend fun queryCatalog(popular: Boolean, tag: String? = null): List<Game> {
        var query: Query = collection.whereEqualTo("status", "approved")
        if (!tag.isNullOrBlank()) query = query.whereArrayContains("tags", tag)
        query = query.orderBy(if (popular) "ratingAvg" else "createdAt", Query.Direction.DESCENDING).limit(100)
        return query.get().await().documents.mapNotNull(::decodeGame)
    }
    override suspend fun refreshCatalog(tag: String?) {
        catalog.refresh(tag)
    }

    private fun user() = auth.user.value ?: error("กรุณาล็อกอินก่อน")
    override suspend fun existingGame(url: String): Game? {
        val normalized = GameIdentity.canonicalUrl(UrlPolicy.normalize(url) ?: error("ลิงก์ไม่ถูกต้อง"))
        return collection.document(GameIdentity.id(normalized)).get().await().let(::decodeGame)?.also(::remember)
    }
    override suspend fun saveGame(draft: GameDraft): SaveGameResult {
        val user = user()
        val url = GameIdentity.canonicalUrl(UrlPolicy.normalize(draft.url) ?: error("ลิงก์ไม่ถูกต้อง"))
        require(draft.title.trim().length in 1..80 && draft.description.length <= 500)
        require(draft.tags.size <= 8 && draft.tags.all { it.length in 1..24 })
        require(draft.orientation in listOf("any", "portrait", "landscape"))
        val id = GameIdentity.id(url)
        val result = db.runTransaction { tx ->
            val ref = collection.document(id)
            if (tx.get(ref).exists()) SaveGameResult.AlreadyExists(id)
            else {
                tx.set(ref, mapOf(
                    "url" to url, "normalizedUrl" to url, "host" to URI(url).host,
                    "title" to draft.title.trim(), "description" to draft.description.trim(), "imageUrl" to draft.imageUrl.trim(),
                    "tags" to draft.tags, "orientation" to draft.orientation, "submittedBy" to user.uid,
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp(), "status" to "approved",
                    "ratingSum" to 0L, "ratingCount" to 0L, "ratingAvg" to 0.0, "commentCount" to 0L,
                ))
                SaveGameResult.Created(id)
            }
        }.await()
        collection.document(id).get().await().let(::decodeGame)?.let { game ->
            remember(game); _games.value = (listOf(game) + games.value).distinctBy { it.id }
        }
        return result
    }

    override fun observeGame(id: String): Flow<Game?> = callbackFlow {
        val registration = collection.document(id).addSnapshotListener { document, error ->
            if (error != null) close(error)
            else {
                val game = document?.let(::decodeGame)
                if (game != null) {
                    remember(game)
                    _games.value = games.value.map { if (it.id == id) game else it }
                }
                trySend(game)
            }
        }
        awaitClose { registration.remove() }
    }
    override fun observeRating(id: String, uid: String): Flow<Int?> = callbackFlow {
        val registration = collection.document(id).collection("ratings").document(uid).addSnapshotListener { doc, error ->
            if (error != null) close(error) else trySend(if (doc?.exists() == true) doc.getLong("value")?.toInt() else null)
        }
        awaitClose { registration.remove() }
    }
    override suspend fun rateGame(id: String, value: Int?) {
        require(value == null || value in 1..5)
        val uid = user().uid
        db.runTransaction { tx ->
            val ref = collection.document(id)
            val ratingRef = ref.collection("ratings").document(uid)
            val game = tx.get(ref)
            val old = tx.get(ratingRef)
            check(game.exists() && game.getString("status") == "approved")
            val totals = RatingTotals(game.getLong("ratingSum") ?: 0, game.getLong("ratingCount") ?: 0)
                .change(if (old.exists()) old.getLong("value")?.toInt() else null, value)
            if (value == null) {
                if (!old.exists()) return@runTransaction
                tx.delete(ratingRef)
            } else {
                val fields = mutableMapOf<String, Any>("value" to value, "updatedAt" to FieldValue.serverTimestamp())
                fields["createdAt"] = (if (old.exists()) old.getTimestamp("createdAt") else null) ?: FieldValue.serverTimestamp()
                tx.set(ratingRef, fields)
            }
            tx.update(ref, mapOf("ratingSum" to totals.sum, "ratingCount" to totals.count, "ratingAvg" to totals.average, "updatedAt" to FieldValue.serverTimestamp()))
        }.await()
    }

    override suspend fun comments(id: String, cursor: String?): CommentPage {
        val comments = collection.document(id).collection("comments")
        var query: Query = comments.orderBy("createdAt", Query.Direction.DESCENDING).orderBy(FieldPath.documentId(), Query.Direction.DESCENDING).limit(20)
        if (cursor != null) {
            // Cursor encodes immutable timestamp + id and survives deletion of the boundary comment.
            val parts = cursor.split(':', limit = 3)
            query = query.startAfter(com.google.firebase.Timestamp(parts[0].toLong(), parts[1].toInt()), parts[2])
        }
        val docs = query.get().await().documents
        val rows = docs.map { doc -> GameComment(doc.id, doc.getString("uid").orEmpty(), doc.getString("authorName") ?: "ผู้เล่น",
            doc.getString("authorPhotoUrl").orEmpty(), doc.getString("text").orEmpty(), doc.getTimestamp("createdAt")?.toDate()?.time) }
        val last = docs.lastOrNull()
        val timestamp = last?.getTimestamp("createdAt")
        return CommentPage(rows, if (last != null && timestamp != null) "${timestamp.seconds}:${timestamp.nanoseconds}:${last.id}" else null, docs.size == 20)
    }
    override suspend fun addComment(id: String, text: String) {
        val user = user()
        val clean = text.trim()
        require(clean.length in 1..500)
        if (!auth.syncProfile()) throw ProfileSyncException(auth.profileSync.value.message ?: "โปรไฟล์ยังไม่พร้อม กรุณาลองบันทึกโปรไฟล์อีกครั้ง")
        check(auth.user.value?.uid == user.uid) { "บัญชีเปลี่ยนแล้ว กรุณาส่งคอมเมนต์อีกครั้ง" }
        val ref = collection.document(id)
        val comment = ref.collection("comments").document()
        db.runBatch { batch ->
            batch.set(comment, mapOf("uid" to user.uid, "authorName" to user.displayName, "authorPhotoUrl" to user.photoUrl,
                "text" to clean, "createdAt" to FieldValue.serverTimestamp()))
            batch.update(ref, mapOf("commentCount" to FieldValue.increment(1), "commentMutationId" to comment.id, "updatedAt" to FieldValue.serverTimestamp()))
        }.await()
    }
    override suspend fun deleteComment(id: String, commentId: String) {
        val uid = user().uid
        val ref = collection.document(id)
        val comment = ref.collection("comments").document(commentId)
        // A transaction prevents retries/concurrent deletes from decrementing twice.
        db.runTransaction { tx ->
            val existing = tx.get(comment)
            if (!existing.exists()) return@runTransaction
            check(existing.getString("uid") == uid)
            tx.delete(comment)
            tx.update(ref, mapOf("commentCount" to FieldValue.increment(-1), "commentMutationId" to commentId, "updatedAt" to FieldValue.serverTimestamp()))
        }.await()
    }
    override suspend fun reportGame(id: String, reason: String, commentId: String?) {
        require(reason.trim().length in 1..200)
        val fields = mutableMapOf<String, Any>("targetType" to if (commentId == null) "game" else "comment", "gameId" to id,
            "reporterUid" to user().uid, "reason" to reason.trim(), "createdAt" to FieldValue.serverTimestamp())
        if (commentId != null) fields["commentId"] = commentId
        db.collection("reports").add(fields).await()
    }

    private fun decodeGame(doc: DocumentSnapshot): Game? {
        if (!doc.exists()) return null
        return Game(id = doc.id, title = doc.getString("title").orEmpty(), url = doc.getString("normalizedUrl") ?: doc.getString("url").orEmpty(),
            description = doc.getString("description").orEmpty(), thumbnailUrl = doc.getString("imageUrl").orEmpty(),
            tags = (doc.get("tags") as? List<*>)?.filterIsInstance<String>().orEmpty(), orientation = doc.getString("orientation") ?: "any",
            developerName = doc.getString("developerName") ?: doc.getString("host") ?: "ผู้สร้างในชุมชน",
            isVerified = doc.getBoolean("isVerified") == true,
            aiTools = (doc.get("aiTools") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            screenshots = (doc.get("screenshots") as? List<*>)?.filterIsInstance<String>().orEmpty(), usesAudio = doc.getBoolean("usesAudio") == true,
            rating = doc.getDouble("ratingAvg")?.takeIf { (doc.getLong("ratingCount") ?: 0) > 0 },
            ratingCount = doc.getLong("ratingCount") ?: 0, commentCount = doc.getLong("commentCount") ?: 0,
            submittedBy = doc.getString("submittedBy").orEmpty(), status = doc.getString("status").orEmpty(), createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0)
    }
}
