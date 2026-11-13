package app.vibedeck.data.metadata

import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException

/** Rejects non-public DNS answers; OkHttp connects using the exact checked address list. */
object PublicHostDns : Dns {
    fun safeClientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder().dns(this).proxy(Proxy.NO_PROXY)
        .followRedirects(false).followSslRedirects(false).addInterceptor { chain ->
        val host = chain.request().url.host
        // OkHttp bypasses Dns.lookup for numeric IPs; validate those explicitly too.
        if (host.contains(':') || host.matches(Regex("[0-9.]+"))) lookup(host)
        chain.proceed(chain.request())
    }
    override fun lookup(hostname: String): List<InetAddress> {
        if (hostname.equals("localhost", true) || hostname.lowercase().endsWith(".localhost")) throw UnknownHostException("ไม่อนุญาต localhost")
        val addresses = Dns.SYSTEM.lookup(hostname)
        if (addresses.isEmpty() || addresses.any(::isPrivate)) throw UnknownHostException("ไม่อนุญาตเครือข่ายภายใน")
        return addresses
    }
    fun isPrivate(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress || address.isMulticastAddress) return true
        val bytes = address.address
        if (bytes.size == 16) return (bytes[0].toInt() and 0xfe) == 0xfc
        val a = bytes[0].toInt() and 255
        val b = bytes[1].toInt() and 255
        return a == 0 || a == 10 || a == 127 || a >= 224 || (a == 169 && b == 254) || (a == 172 && b in 16..31) ||
            (a == 192 && b == 168) || (a == 100 && b in 64..127)
    }
}
