package app.vibedeck.data.model

import java.net.URI
import java.net.IDN
import java.security.MessageDigest

/** Canonicalizes an already validated absolute URL. Query and path semantics stay intact. */
object GameIdentity {
    fun canonicalUrl(normalized: String): String {
        val uri = URI(normalized.trim()).normalize()
        val scheme = uri.scheme?.lowercase() ?: error("URL ต้องมี scheme")
        require(scheme in listOf("http", "https") && uri.userInfo == null)
        val host = uri.host?.lowercase()?.let(IDN::toASCII) ?: error("URL ต้องมีชื่อ host")
        val port = uri.port.takeUnless { it == -1 || (scheme == "https" && it == 443) || (scheme == "http" && it == 80) }
        val authority = host + (port?.let { ":$it" } ?: "")
        val path = uri.rawPath.orEmpty().ifBlank { "/" }
        return "$scheme://$authority$path" + (uri.rawQuery?.let { "?$it" } ?: "")
    }
    fun id(normalized: String): String = MessageDigest.getInstance("SHA-256")
        .digest(canonicalUrl(normalized).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
