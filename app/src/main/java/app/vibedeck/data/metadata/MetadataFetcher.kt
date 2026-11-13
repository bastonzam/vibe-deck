package app.vibedeck.data.metadata

import app.vibedeck.UrlPolicy
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class MetadataFetcher {
    private val client = PublicHostDns.safeClientBuilder().followRedirects(false).followSslRedirects(false)
        .callTimeout(10, TimeUnit.SECONDS).connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build()
    suspend fun fetch(input: String): MetadataResult {
        val normalized = UrlPolicy.normalize(input) ?: return MetadataResult.Failure("ลิงก์ไม่ถูกต้อง กรุณาใช้ http หรือ https")
        return try {
            withTimeout(10_000) { load(normalized) }
        } catch (_: TimeoutCancellationException) { MetadataResult.Failure("อ่านข้อมูลเกิน 10 วินาที กรอกข้อมูลเองหรือทดลองใหม่ได้") }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { MetadataResult.Failure("อ่านข้อมูลเว็บไม่ได้ กรอกชื่อและรายละเอียดเองได้") }
    }
    private suspend fun load(initial: String): MetadataResult {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        var url = initial
        repeat(6) { hop ->
            val remaining = deadline - System.nanoTime()
            check(remaining > 0) { "หมดเวลา" }
            val call = client.newCall(Request.Builder().url(url).header("Accept", "text/html").header("User-Agent", "VibeDeck/0.1 GamePreview").build())
            call.timeout().timeout(remaining, TimeUnit.NANOSECONDS)
            call.awaitResponse().use { response ->
                if (response.code in listOf(301, 302, 303, 307, 308)) {
                    check(hop < 5)
                    url = response.header("Location")?.let { response.request.url.resolve(it)?.toString() } ?: error("redirect ไม่ถูกต้อง")
                    check(UrlPolicy.normalize(url) != null)
                } else {
                    check(response.isSuccessful)
                    val body = response.body ?: error("ไม่พบ HTML")
                    check(body.contentType()?.let { it.type == "text" && it.subtype == "html" } == true)
                    return withContext(Dispatchers.IO) {
                        val source = body.source()
                        source.request(1_048_577L)
                        check(source.buffer.size <= 1_048_576L) { "หน้าเว็บใหญ่เกินไป" }
                        val bytes = source.readByteArray(minOf(source.buffer.size, 1_048_576L))
                        val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                        MetadataParser.parse(String(bytes, charset), response.request.url.toString())
                    }
                }
            }
        }
        return MetadataResult.Failure("เว็บเปลี่ยนเส้นทางมากเกินไป")
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
            }
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { response.close() }
            }
        })
    }
}
