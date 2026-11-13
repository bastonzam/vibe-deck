package app.vibedeck.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vibedeck.auth.AuthRepository
import app.vibedeck.auth.signInErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthGateState(val visible: Boolean = false, val loading: Boolean = false, val error: String? = null, val notice: String? = null)
class AuthGateViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthGateState())
    val state = _state.asStateFlow()
    private var pending: (() -> Unit)? = null
    fun requireLogin(action: () -> Unit) {
        if (auth.user.value != null) action()
        else { pending = action; _state.value = AuthGateState(visible = true) }
    }
    fun open() { _state.value = AuthGateState(visible = true) }
    fun dismiss() { if (!state.value.loading) { pending = null; _state.value = AuthGateState() } }
    fun signIn(activity: Activity) {
        if (state.value.loading) return
        _state.value = AuthGateState(visible = true, loading = true)
        viewModelScope.launch {
            try {
                auth.signIn(activity)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                _state.value = AuthGateState(visible = true, error = signInErrorMessage(error))
                return@launch
            }
            val action = pending
            pending = null
            _state.value = AuthGateState(notice = "เข้าสู่ระบบสำเร็จ")
            try { action?.invoke() }
            catch (_: Exception) { _state.value = AuthGateState(notice = "เข้าสู่ระบบแล้ว แต่ทำรายการต่อไม่สำเร็จ กรุณาลองรายการนั้นอีกครั้ง") }
        }
    }
    fun clearNotice() { _state.update { it.copy(notice = null) } }
    fun retryProfile() { viewModelScope.launch { auth.syncProfile() } }
    fun signOut() { viewModelScope.launch { auth.signOut() } }
}
