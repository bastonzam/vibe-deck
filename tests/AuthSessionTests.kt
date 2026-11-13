import app.vibedeck.auth.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

private var passed = 0
private fun test(name: String, block: suspend CoroutineScope.() -> Unit) {
    runBlocking { block() }
    passed++
    println("PASS $name")
}
private val alice = AppUser("alice", "Alice", "")
private val bob = AppUser("bob", "Bob", "")
private fun background() = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

fun main() {
    test("authenticated user remains signed in when Firestore profile write fails") {
        val scope = background()
        try {
            val session = AuthSession(scope, { error("permission denied") }, { "profile unavailable" })
            session.updateUser(alice)
            check(session.user.value == alice)
            check(session.profile.value.phase == ProfileSyncPhase.ERROR)
            check(session.profile.value.message == "profile unavailable")
        } finally { scope.cancel() }
    }
    test("profile retry succeeds without another Google authentication") {
        val scope = background()
        var attempts = 0
        try {
            val session = AuthSession(scope, { if (++attempts == 1) error("offline") }, { "profile unavailable" })
            session.updateUser(alice)
            check(session.profile.value.phase == ProfileSyncPhase.ERROR)
            check(session.syncProfile())
            check(session.profile.value.phase == ProfileSyncPhase.READY)
            check(session.user.value == alice && attempts == 2)
        } finally { scope.cancel() }
    }
    test("profile timeout surfaces a profile warning and preserves login") {
        val scope = background()
        try {
            val session = AuthSession(scope, { awaitCancellation() }, { "unexpected" }, timeoutMillis = 20)
            session.updateUser(alice)
            val warning = withTimeout(2000) { session.profile.first { it.phase == ProfileSyncPhase.ERROR } }
            check(session.user.value == alice)
            check(warning.message.orEmpty().contains("เข้าสู่ระบบแล้ว"))
        } finally { scope.cancel() }
    }
    test("logout cancels outstanding profile work and clears profile state") {
        val scope = background()
        val release = CompletableDeferred<Unit>()
        try {
            val session = AuthSession(scope, { release.await() }, { "profile unavailable" })
            session.updateUser(alice)
            check(session.profile.value.phase == ProfileSyncPhase.SYNCING)
            session.updateUser(null)
            release.complete(Unit)
            yield()
            check(session.user.value == null)
            check(session.profile.value == ProfileSyncState())
        } finally { scope.cancel() }
    }
    test("late failure from previous account cannot replace the new account state") {
        val scope = background()
        try {
            val session = AuthSession(scope, { user ->
                if (user.uid == alice.uid) withContext(NonCancellable) { delay(25); error("old account failure") }
            }, { "profile unavailable" })
            session.updateUser(alice)
            session.updateUser(bob)
            withTimeout(2000) { session.profile.first { it.uid == bob.uid && it.phase == ProfileSyncPhase.READY } }
            check(session.user.value == bob)
            check(session.profile.value.message == null)
        } finally { scope.cancel() }
    }
    test("simultaneous retry waits for the active write instead of creating duplicate profiles") {
        val scope = background()
        val release = CompletableDeferred<Unit>()
        var writes = 0
        try {
            val session = AuthSession(scope, { writes++; release.await() }, { "profile unavailable" })
            session.updateUser(alice)
            val retry = async { session.syncProfile() }
            yield()
            release.complete(Unit)
            check(retry.await())
            check(writes == 1)
        } finally { scope.cancel() }
    }
    test("profile errors distinguish API setup, rules permissions and network") {
        check(ProfileSyncMessages.failure("PERMISSION_DENIED", "Cloud Firestore API has not been used in project before or it is disabled").contains("Cloud Firestore API"))
        check(ProfileSyncMessages.failure("PERMISSION_DENIED").contains("users/{uid}"))
        check(ProfileSyncMessages.failure("UNAVAILABLE").contains("อินเทอร์เน็ต"))
    }
    println("$passed auth regression tests passed")
}
