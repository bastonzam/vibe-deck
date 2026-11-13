package app.vibedeck.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vibedeck.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import app.vibedeck.auth.AuthRepository
import app.vibedeck.data.model.GameComment

data class DetailState(val game: Game? = null, val favorite: Boolean = false, val saved: Boolean = false,
    val loading: Boolean = true, val error: String? = null, val myRating: Int? = null, val uid: String? = null,
    val comments: List<GameComment> = emptyList(), val commentText: String = "", val commentsLoading: Boolean = false,
    val commentsError: String? = null, val hasMore: Boolean = false, val busy: Boolean = false, val message: String? = null)
@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModel(private val repository: GameRepository, private val id: String, private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(DetailState(game = repository.find(id)))
    val state = _state.asStateFlow()
    private var cursor: String? = null
    private var gameListener: Job? = null
    private var commentsRequest: Job? = null
    private var commentsStarted = false
    init {
        listen()
        viewModelScope.launch { repository.library.collect { library -> _state.update { it.copy(favorite = library.favorites.any { g -> g.url == it.game?.url }) } } }
        viewModelScope.launch {
            combine(auth.user, _state.map { it.saved }.distinctUntilChanged()) { user, saved -> user.takeIf { saved } }
                .distinctUntilChanged().flatMapLatest { user ->
                _state.update { it.copy(uid = user?.uid, myRating = null) }
                if (user == null) flowOf(null) else repository.observeRating(id, user.uid).catch { error ->
                    if (error is CancellationException) throw error
                    _state.update { it.copy(message = "โหลดคะแนนของคุณไม่สำเร็จ ลองเปิดหน้าเกมอีกครั้ง") }
                    emit(null)
                }
            }.collect { rating -> _state.update { it.copy(myRating = rating) } }
        }
    }
    fun listen() {
        gameListener?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        gameListener = viewModelScope.launch {
            repository.observeGame(id).catch { error ->
                if (error is CancellationException) throw error
                _state.update { it.copy(loading = false, error = "โหลดรายละเอียดไม่สำเร็จ ทดลองใหม่ได้") }
            }.collect { game ->
                _state.update { it.copy(game = game ?: repository.find(id), saved = game != null && game.submittedBy.isNotBlank(), loading = false) }
                if (game != null && !commentsStarted) { commentsStarted = true; loadComments(true) }
            }
        }
    }
    fun toggleFavorite() { repository.find(id)?.let { repository.toggleFavorite(it.url) } }
    fun commentText(value: String) { _state.update { it.copy(commentText = value.take(500)) } }
    fun clearMessage() { _state.update { it.copy(message = null) } }
    fun rate(value: Int?) = action("อัปเดตคะแนนแล้ว") { repository.rateGame(id, value) }
    fun sendComment() {
        val text = state.value.commentText.trim()
        if (text.isEmpty()) return
        action("ส่งคอมเมนต์แล้ว") { repository.addComment(id, text); _state.update { it.copy(commentText = "") }; loadComments(true) }
    }
    fun deleteComment(commentId: String) = action("ลบคอมเมนต์แล้ว") {
        repository.deleteComment(id, commentId)
        _state.update { it.copy(comments = it.comments.filterNot { row -> row.id == commentId }) }
    }
    fun report(reason: String, commentId: String?) = action("ส่งรายงานแล้ว ขอบคุณที่ช่วยดูแลชุมชน") { repository.reportGame(id, reason, commentId) }
    private fun action(success: String, block: suspend () -> Unit) {
        if (state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try { block(); _state.update { it.copy(busy = false, message = success) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _state.update { it.copy(busy = false,
                message = if (error is app.vibedeck.auth.ProfileSyncException) error.message else "ทำรายการไม่สำเร็จ ตรวจการเชื่อมต่อแล้วลองอีกครั้ง") } }
        }
    }
    fun loadComments(reset: Boolean = false) {
        if (!reset && (state.value.commentsLoading || !state.value.hasMore)) return
        if (reset) commentsRequest?.cancel()
        if (reset) cursor = null
        _state.update { it.copy(commentsLoading = true, commentsError = null) }
        commentsRequest = viewModelScope.launch {
            try {
                val page = repository.comments(id, cursor)
                cursor = page.nextCursor
                _state.update { it.copy(comments = (if (reset) page.comments else it.comments + page.comments).distinctBy { row -> row.id },
                    hasMore = page.hasMore && page.nextCursor != null, commentsLoading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(commentsLoading = false, commentsError = "โหลดคอมเมนต์ไม่สำเร็จ") } }
        }
    }
}
