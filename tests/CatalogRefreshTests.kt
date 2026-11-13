import app.vibedeck.data.CatalogRefresher
import app.vibedeck.data.model.CatalogStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private var passed = 0
private fun test(name: String, block: suspend CoroutineScope.() -> Unit) {
    runBlocking { block() }
    passed++
    println("PASS $name")
}
private fun loader(items: MutableStateFlow<List<String>>, status: MutableStateFlow<CatalogStatus>,
    query: suspend (Boolean, String?) -> List<String>) = CatalogRefresher(items, status, { it }, query, describeError = { "query failed" })

fun main() {
    test("latest games stay visible when the popular query fails") {
        val items = MutableStateFlow<List<String>>(emptyList())
        val status = MutableStateFlow(CatalogStatus())
        loader(items, status) { popular, _ -> if (popular) error("missing index") else listOf("loaded") }.refresh(null)
        check(items.value == listOf("loaded"))
        check(status.value.error == null && status.value.warning != null && !status.value.loading)
    }
    test("cached games survive a failed refresh with a non-blocking warning") {
        val items = MutableStateFlow(listOf("cached"))
        val status = MutableStateFlow(CatalogStatus())
        loader(items, status) { _, _ -> error("offline") }.refresh(null)
        check(items.value == listOf("cached"))
        check(status.value.error == null && status.value.warning != null)
    }
    test("a full error is reserved for no data and both queries failing") {
        val items = MutableStateFlow<List<String>>(emptyList())
        val status = MutableStateFlow(CatalogStatus())
        loader(items, status) { _, _ -> error("offline") }.refresh(null)
        check(items.value.isEmpty() && status.value.error != null && !status.value.loading)
    }
    test("a later successful refresh clears warnings and deduplicates query results") {
        val items = MutableStateFlow(listOf("old"))
        val status = MutableStateFlow(CatalogStatus())
        var failing = true
        val loader = loader(items, status) { popular, _ ->
            if (failing) error("offline") else if (popular) listOf("same", "popular") else listOf("same", "latest")
        }
        loader.refresh(null)
        failing = false
        loader.refresh(null)
        check(items.value == listOf("same", "latest", "popular"))
        check(status.value == CatalogStatus())
    }
    test("an old failed request cannot replace a newer successful request") {
        val releaseOld = CompletableDeferred<Unit>()
        val items = MutableStateFlow<List<String>>(emptyList())
        val status = MutableStateFlow(CatalogStatus())
        val loader = loader(items, status) { _, tag ->
            if (tag == "old") { releaseOld.await(); error("old failure") }
            listOf("new")
        }
        val old = async { loader.refresh("old") }
        yield()
        loader.refresh("new")
        releaseOld.complete(Unit)
        old.await()
        check(items.value == listOf("new") && status.value == CatalogStatus())
    }
    test("cancelling refresh does not turn cancellation into a loading error") {
        val items = MutableStateFlow(listOf("cached"))
        val status = MutableStateFlow(CatalogStatus())
        val loader = loader(items, status) { _, _ -> awaitCancellation() }
        val request = launch { loader.refresh(null) }
        yield()
        request.cancelAndJoin()
        check(items.value == listOf("cached") && status.value.error == null && !status.value.loading)
    }
    test("confirmed empty catalog clears old games; partial empty failure preserves them") {
        val items = MutableStateFlow(listOf("cached"))
        val status = MutableStateFlow(CatalogStatus())
        loader(items, status) { popular, _ -> if (popular) error("offline") else emptyList() }.refresh(null)
        check(items.value == listOf("cached") && status.value.error == null)
        loader(items, status) { _, _ -> emptyList() }.refresh(null)
        check(items.value.isEmpty() && status.value == CatalogStatus())
    }
    test("latest games publish before a slower popular query finishes") {
        val releasePopular = CompletableDeferred<Unit>()
        val items = MutableStateFlow<List<String>>(emptyList())
        val status = MutableStateFlow(CatalogStatus())
        val loader = loader(items, status) { popular, _ ->
            if (popular) { releasePopular.await(); error("popular failed") }
            listOf("latest")
        }
        val request = async { loader.refresh(null) }
        withTimeout(2000) { items.first { it == listOf("latest") } }
        check(status.value.loading)
        releasePopular.complete(Unit)
        request.await()
        check(items.value == listOf("latest") && status.value.error == null)
    }
    println("$passed catalog regression tests passed")
}
