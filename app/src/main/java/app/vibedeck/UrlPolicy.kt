package app.vibedeck

import android.net.Uri

/** รวมกฎเรื่อง URL ไว้ที่เดียว เฟส 2 ค่อยเพิ่ม allowlist/blocklist/Safe Browsing ตรงนี้ */
object UrlPolicy {

    /** แปลง input ของผู้ใช้เป็น URL ที่เล่นได้ หรือ null ถ้าใช้ไม่ได้ */
    fun normalize(input: String): String? {
        var s = input.trim()
        if (s.isEmpty()) return null
        if (!s.contains("://")) s = "https://$s"
        val uri = Uri.parse(s)
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return s
    }

    /** ใช้ตัดสินใจใน WebViewClient ว่าให้ navigate ต่อไหม */
    fun isAllowedNavigation(url: String): Boolean {
        val scheme = Uri.parse(url).scheme?.lowercase()
        return scheme == "http" || scheme == "https"
    }
}
