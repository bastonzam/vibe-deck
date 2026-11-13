package app.vibedeck.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vibedeck.UrlPolicy
import app.vibedeck.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import app.vibedeck.data.model.GameDraft
import app.vibedeck.data.model.SaveGameResult

data class AddGameState(val input: String = "", val game: Game? = null, val loading: Boolean = false,
    val error: String? = null, val submitted: Boolean = false, val saving: Boolean = false,
    val savedId: String? = null, val duplicate: Boolean = false, val tagsText: String = "")
class AddGameViewModel(private val repository: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow(AddGameState())
    val state = _state.asStateFlow()
    private var request: Job? = null
    fun input(value: String) {
        if (state.value.saving) return
        request?.cancel()
        _state.value = AddGameState(input = value.take(4096))
    }
    fun preview() {
        if (state.value.saving) return
        request?.cancel()
        val normalized = UrlPolicy.normalize(state.value.input)
        if (normalized == null) {
            _state.update { it.copy(error = "กรุณาใส่ลิงก์ http หรือ https ที่ถูกต้อง", game = null) }
            return
        }
        val fallback = runCatching { repository.gameForUrl(normalized) }.getOrElse {
            _state.update { it.copy(error = "รูปแบบ URL นี้ไม่รองรับ กรุณาตรวจลิงก์อีกครั้ง", game = null) }
            return
        }
        _state.update { it.copy(loading = true, error = null, submitted = false, savedId = null, duplicate = false,
            game = fallback.copy(title = fallback.title.take(80), description = fallback.description.take(500), orientation = "any")) }
        request = viewModelScope.launch {
            try {
                val existing = try { withTimeoutOrNull(8000) { repository.existingGame(normalized) } }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { null } // The save transaction still checks duplicates when this lookup is offline.
                if (existing != null) {
                    _state.update { it.copy(loading = false, game = existing, savedId = existing.id,
                        duplicate = true, tagsText = existing.tags.joinToString(", ")) }
                    return@launch
                }
                val game = repository.preview(normalized)
                _state.update { it.copy(loading = false, game = game) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                _state.update { it.copy(loading = false, error = "อ่านภาพและชื่อเว็บไม่ได้ แต่ยังเปิดเล่นจากลิงก์นี้ได้") }
            }
        }
    }
    private fun edit(change: (AddGameState) -> AddGameState) {
        _state.update { if (it.saving || it.savedId != null) it else change(it) }
    }
    fun title(value: String) = edit { it.copy(game = it.game?.copy(title = value.take(80))) }
    fun description(value: String) = edit { it.copy(game = it.game?.copy(description = value.take(500))) }
    fun image(value: String) = edit { it.copy(game = it.game?.copy(thumbnailUrl = value.take(4096))) }
    fun tags(value: String) = edit { it.copy(tagsText = value.take(199)) }
    fun orientation(value: String) = edit { it.copy(game = it.game?.copy(orientation = value)) }
    fun submit() {
        if (state.value.saving || state.value.loading || state.value.savedId != null) return
        val game = state.value.game ?: return
        if (game.thumbnailUrl.isNotBlank() && !game.thumbnailUrl.matches(Regex("https?://[^\\s]+"))) {
            _state.update { it.copy(error = "URL รูปหน้าปกต้องขึ้นต้นด้วย http:// หรือ https://") }; return
        }
        val tags = state.value.tagsText.split(',').map(String::trim).filter(String::isNotBlank).distinct()
        if (game.title.isBlank() || tags.size > 8 || tags.any { it.length > 24 }) {
            _state.update { it.copy(error = "กรอกชื่อเกม และใช้แท็กไม่เกิน 8 แท็ก แท็กละ 24 ตัวอักษร") }; return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val result = repository.saveGame(GameDraft(game.url, game.title, game.description, game.thumbnailUrl, tags,
                    game.orientation.takeIf { it in listOf("portrait", "landscape") } ?: "any"))
                val id = when (result) { is SaveGameResult.Created -> result.id; is SaveGameResult.AlreadyExists -> result.id }
                _state.update { it.copy(saving = false, savedId = id, duplicate = result is SaveGameResult.AlreadyExists) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(saving = false, error = "บันทึกไม่สำเร็จ ตรวจการเชื่อมต่อและทดลองใหม่") } }
        }
    }
}
