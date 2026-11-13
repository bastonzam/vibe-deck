package app.vibedeck.data.metadata

import org.jsoup.Jsoup
import java.net.URI

data class GameMetadata(val title: String, val description: String, val imageUrl: String, val finalUrl: String)
sealed interface MetadataResult {
    data class Success(val metadata: GameMetadata) : MetadataResult
    data class PartialSuccess(val metadata: GameMetadata) : MetadataResult
    data class Failure(val message: String) : MetadataResult
}

object MetadataParser {
    fun parse(html: String, finalUrl: String): MetadataResult {
        val doc = Jsoup.parse(html, finalUrl)
        fun meta(key: String): String = doc.select("meta[property=$key], meta[name=$key]").firstOrNull { it.attr("content").isNotBlank() }?.attr("content").orEmpty()
        fun clean(value: String, limit: Int) = value.replace(Regex("\\s+"), " ").trim().take(limit)
        fun absolute(value: String): String? = runCatching {
            require(value.isNotBlank())
            val uri = URI(finalUrl).resolve(value.trim())
            require(uri.scheme?.lowercase() in listOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null)
            uri.toString()
        }.getOrNull()
        val host = URI(finalUrl).host.orEmpty()
        val title = clean(sequenceOf(meta("og:title"), meta("twitter:title"), doc.title(), host).first { it.isNotBlank() }, 80)
        val description = clean(sequenceOf(meta("og:description"), meta("description")).firstOrNull { it.isNotBlank() }.orEmpty(), 500)
        val icons = doc.select("link[rel]").filter { "icon" in it.attr("rel").lowercase().split(Regex("\\s+")) }
            .sortedByDescending { element -> Regex("(\\d+)x(\\d+)").findAll(element.attr("sizes")).map { it.groupValues[1].toIntOrNull() ?: 0 }.maxOrNull() ?: 0 }
        val candidates = listOf(meta("og:image"), meta("twitter:image")) +
            doc.select("link[rel=apple-touch-icon]").map { it.attr("href") } + icons.map { it.attr("href") }
        val image = candidates.firstNotNullOfOrNull(::absolute) ?: absolute("/favicon.ico").orEmpty()
        val result = GameMetadata(title, description, image, finalUrl)
        return if (description.isBlank() || image.isBlank() || title == host) MetadataResult.PartialSuccess(result) else MetadataResult.Success(result)
    }
}
