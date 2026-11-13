package app.vibedeck.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    val user: StateFlow<AppUser?>
    val profileSync: StateFlow<ProfileSyncState>
    suspend fun signIn(activity: Activity)
    suspend fun syncProfile(): Boolean
    suspend fun signOut()
}

class FirebaseAuthRepository(context: Context, private val auth: FirebaseAuth, private val db: FirebaseFirestore) : AuthRepository {
    private val appContext = context.applicationContext
    private val manager = CredentialManager.create(appContext)
    private fun current() = auth.currentUser?.let { AppUser(it.uid, (it.displayName ?: "ผู้เล่น").take(120), it.photoUrl?.toString().orEmpty()) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val session = AuthSession(scope, ::writeProfile, { error ->
        val code = (error as? FirebaseFirestoreException)?.code?.name
        // Only log the phase and a non-sensitive code; never log credentials or profile values.
        android.util.Log.w("VibeDeckAuth", "Profile sync failed: ${code ?: error.javaClass.simpleName}")
        ProfileSyncMessages.failure(code, error.message.orEmpty())
    })
    override val user = session.user
    override val profileSync = session.profile
    private val listener = FirebaseAuth.AuthStateListener { session.updateUser(current()) }
    init { auth.addAuthStateListener(listener) }

    override suspend fun signIn(activity: Activity) {
        val generatedId = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        val id = if (generatedId != 0) generatedId else appContext.resources.getIdentifier("vibedeck_web_client_id", "string", appContext.packageName)
        check(id != 0) { "กรุณาเปิด Google Sign-In และดาวน์โหลด google-services.json ใหม่ก่อน" }
        val option = GetGoogleIdOption.Builder().setServerClientId(appContext.getString(id))
            .setFilterByAuthorizedAccounts(false).setAutoSelectEnabled(false).build()
        val result = manager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build()).credential
        check(result is CustomCredential && result.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { "ไม่พบข้อมูลบัญชี Google" }
        val token = GoogleIdTokenCredential.createFrom(result.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await().user ?: error("เข้าสู่ระบบไม่สำเร็จ")
        session.updateUser(current())
        // Google/Firebase authentication is complete. Profile sync runs separately in session.
    }

    override suspend fun syncProfile(): Boolean = session.syncProfile()

    private suspend fun writeProfile(user: AppUser) {
        // Preserve the original creation timestamp across repeated sign-ins.
        db.runTransaction { transaction ->
            val ref = db.collection("users").document(user.uid)
            val existing = transaction.get(ref)
            val fields = mutableMapOf<String, Any>("displayName" to user.displayName, "photoUrl" to user.photoUrl)
            if (!existing.exists()) fields["createdAt"] = FieldValue.serverTimestamp()
            transaction.set(ref, fields, com.google.firebase.firestore.SetOptions.merge())
        }.await()
    }
    override suspend fun signOut() {
        auth.signOut()
        // Firebase session is already cleared even if the credential provider is unavailable.
        runCatching { manager.clearCredentialState(ClearCredentialStateRequest()) }
    }
    fun close() { auth.removeAuthStateListener(listener); scope.cancel() }
}

/** Keep configuration / cancellation / network failures distinct from profile-write failures. */
fun signInErrorMessage(error: Exception): String = when {
    error is GetCredentialCancellationException -> "ยกเลิกการเข้าสู่ระบบแล้ว กดปุ่มเพื่อเลือกบัญชีอีกครั้งได้"
    error is GetCredentialException && error.message.orEmpty().contains("28444") ->
        "Google Sign-In ยังตั้งค่าไม่ครบ (28444) ตรวจ package, SHA-1 ของ APK และ Web client ID ใน Firebase Console"
    error is NoCredentialException -> "ไม่พบบัญชี Google ที่ใช้งานได้ ตรวจบัญชีในมือถือและ Google Play services แล้วลองใหม่"
    error is FirebaseNetworkException -> "เชื่อมต่อบริการล็อกอินไม่ได้ ตรวจอินเทอร์เน็ตแล้วลองใหม่"
    error is FirebaseAuthException && error.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ->
        "ยังไม่ได้เปิด Google provider ใน Firebase Authentication"
    error is FirebaseAuthException -> "ยืนยันบัญชีกับ Firebase ไม่สำเร็จ (${error.errorCode}) กรุณาลองใหม่"
    else -> "เข้าสู่ระบบ Google ไม่สำเร็จ กรุณาลองใหม่หรือตรวจการตั้งค่า Google Sign-In"
}
