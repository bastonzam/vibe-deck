package app.vibedeck.data

import app.vibedeck.data.model.CatalogStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

/** Independent catalog sources may fail independently; usable data must remain visible. */
class CatalogRefresher<T>(
    private val items: MutableStateFlow<List<T>>,
    private val status: MutableStateFlow<CatalogStatus>,
    private val key: (T) -> String,
    private val query: suspend (Boolean, String?) -> List<T>,
    private val onLoaded: (List<T>) -> Unit = {},
    private val describeError: (Exception) -> String,
    private val timeoutMillis: Long = 15_000,
) {
    private var generation = 0L

    private suspend fun fetch(popular: Boolean, tag: String?): Result<List<T>> = try {
        Result.success(withTimeout(timeoutMillis) { query(popular, tag) })
    } catch (timeout: TimeoutCancellationException) { Result.failure(timeout) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { Result.failure(error) }

    suspend fun refresh(tag: String?) {
        val revision = ++generation
        val previous = items.value
        status.value = CatalogStatus(loading = true)
        val successful = mutableListOf<T>()
        try {
            supervisorScope {
                val latest = async { fetch(false, tag) }
                val popular = async { fetch(true, tag) }
                val results = listOf(latest, popular).map { request ->
                    request.await().also { result ->
                        if (generation == revision) result.getOrNull()?.let { loaded ->
                            successful.addAll(loaded)
                            onLoaded(loaded)
                            if (successful.isNotEmpty()) items.value =
                                (if (tag == null) successful.toList() else items.value + successful).distinctBy(key)
                        }
                    }
                }
                if (generation != revision) return@supervisorScope
                val failures = results.mapNotNull { it.exceptionOrNull() as? Exception }
                when {
                    failures.isEmpty() -> {
                        // Only an entirely successful empty response can clear an old catalog.
                        items.value = (if (tag == null) successful.toList() else items.value + successful).distinctBy(key)
                        status.value = CatalogStatus()
                    }
                    successful.isNotEmpty() || previous.isNotEmpty() -> {
                        if (successful.isEmpty()) items.value = previous
                        status.value = CatalogStatus(warning = "อัปเดตเกมบางส่วนไม่สำเร็จ ยังแสดงเกมที่โหลดได้อยู่ · ${describeError(failures.first())}")
                    }
                    results.any { it.isSuccess } -> status.value = CatalogStatus(warning =
                        "โหลดรายการบางส่วนแล้ว อีกส่วนยังไม่พร้อม · ${describeError(failures.first())}")
                    else -> status.value = CatalogStatus(error = describeError(failures.first()))
                }
            }
        } catch (cancelled: CancellationException) {
            if (generation == revision) status.value = CatalogStatus()
            throw cancelled
        }
    }
}
