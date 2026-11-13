package app.vibedeck.auth

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AppUser(val uid: String, val displayName: String, val photoUrl: String)
enum class ProfileSyncPhase { IDLE, SYNCING, READY, ERROR }
data class ProfileSyncState(val uid: String? = null, val phase: ProfileSyncPhase = ProfileSyncPhase.IDLE, val message: String? = null)
class ProfileSyncException(message: String) : IllegalStateException(message)

/** Authentication is committed immediately; profile availability is an independent, retryable state. */
class AuthSession(
    private val scope: CoroutineScope,
    private val writeProfile: suspend (AppUser) -> Unit,
    private val describeError: (Exception) -> String,
    private val timeoutMillis: Long = 12_000,
) {
    private val _user = MutableStateFlow<AppUser?>(null)
    val user = _user.asStateFlow()
    private val _profile = MutableStateFlow(ProfileSyncState())
    val profile = _profile.asStateFlow()
    private val mutex = Mutex()
    private var generation = 0L
    private var request: Job? = null

    fun updateUser(user: AppUser?) {
        if (_user.value == user) return
        generation++
        request?.cancel()
        _user.value = user
        _profile.value = ProfileSyncState(uid = user?.uid)
        if (user != null) request = scope.launch { syncProfile() }
    }

    suspend fun syncProfile(): Boolean = mutex.withLock {
        val user = _user.value ?: return@withLock false
        if (_profile.value.uid == user.uid && _profile.value.phase == ProfileSyncPhase.READY) return@withLock true
        val revision = generation
        _profile.value = ProfileSyncState(user.uid, ProfileSyncPhase.SYNCING)
        try {
            withTimeout(timeoutMillis) { writeProfile(user) }
            if (generation != revision) return@withLock false
            _profile.value = ProfileSyncState(user.uid, ProfileSyncPhase.READY)
            true
        } catch (_: TimeoutCancellationException) {
            if (generation == revision) _profile.value = ProfileSyncState(user.uid, ProfileSyncPhase.ERROR,
                "เข้าสู่ระบบแล้ว แต่การบันทึกโปรไฟล์ใช้เวลานานเกินไป ตรวจการเชื่อมต่อแล้วลองใหม่")
            false
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (generation == revision) _profile.value = ProfileSyncState(user.uid, ProfileSyncPhase.ERROR, describeError(error))
            false
        }
    }
}

object ProfileSyncMessages {
    fun failure(code: String?, detail: String = ""): String = when {
        detail.contains("has not been used", true) || detail.contains("API has", true) && detail.contains("disabled", true) ->
            "เข้าสู่ระบบแล้ว แต่ยังบันทึกโปรไฟล์ไม่ได้ กรุณาเปิด Cloud Firestore API และสร้างฐานข้อมูลใน Firebase Console"
        code == "PERMISSION_DENIED" ->
            "เข้าสู่ระบบแล้ว แต่ Firestore ไม่อนุญาตให้บันทึกโปรไฟล์ ตรวจ API และ Rules ของ users/{uid} แล้วลองใหม่"
        code == "UNAVAILABLE" || code == "DEADLINE_EXCEEDED" ->
            "เข้าสู่ระบบแล้ว แต่ยังเชื่อมต่อ Firestore ไม่ได้ ตรวจอินเทอร์เน็ตแล้วลองบันทึกโปรไฟล์ใหม่"
        code == "NOT_FOUND" || code == "FAILED_PRECONDITION" ->
            "เข้าสู่ระบบแล้ว แต่ฐานข้อมูล Firestore ยังไม่พร้อม ตรวจการสร้างฐานข้อมูลและการตั้งค่าใน Firebase Console"
        code == "UNAUTHENTICATED" ->
            "เข้าสู่ระบบแล้ว แต่ Firestore ยืนยันเซสชันไม่ได้ ลองบันทึกโปรไฟล์ใหม่หรือออกจากระบบแล้วเข้าอีกครั้ง"
        else -> "เข้าสู่ระบบแล้ว แต่ยังบันทึกโปรไฟล์ไม่ได้ ลองใหม่ได้โดยไม่ต้องล็อกอินซ้ำ"
    }
}
