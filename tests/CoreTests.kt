import app.vibedeck.data.model.GameIdentity
import app.vibedeck.data.model.RatingTotals
import app.vibedeck.data.metadata.MetadataParser
import app.vibedeck.data.metadata.MetadataResult
import app.vibedeck.data.metadata.GameMetadata
import app.vibedeck.data.metadata.PublicHostDns
import java.net.InetAddress

private var passed = 0
private fun test(name: String, block: () -> Unit) { block(); passed++; println("PASS $name") }
private fun metadata(html: String, base: String = "https://games.example/play/index.html"): GameMetadata = when (val result = MetadataParser.parse(html, base)) {
    is MetadataResult.Success -> result.metadata
    is MetadataResult.PartialSuccess -> result.metadata
    is MetadataResult.Failure -> error(result.message)
}

fun main() {
    test("canonical URL casing, default port, root slash and fragment produce one game ID") {
        val urls = listOf("https://EXAMPLE.com", "https://example.com/", "https://example.com:443/#play")
        check(urls.map(GameIdentity::id).distinct().size == 1)
        check(GameIdentity.id(urls.first()).matches(Regex("[0-9a-f]{64}")))
    }
    test("dot paths normalize while query, case-sensitive paths and non-default ports remain distinct") {
        check(GameIdentity.id("https://example.com/a/../game") == GameIdentity.id("https://example.com/game#x"))
        check(GameIdentity.id("https://example.com/game?a=1") != GameIdentity.id("https://example.com/game?a=2"))
        check(GameIdentity.id("https://example.com/Game") != GameIdentity.id("https://example.com/game"))
        check(GameIdentity.id("https://example.com:8443/game") != GameIdentity.id("https://example.com/game"))
    }
    test("unsupported schemes and URL credentials are rejected") {
        check(runCatching { GameIdentity.id("file:///game") }.isFailure)
        check(runCatching { GameIdentity.id("https://user:pass@example.com/") }.isFailure)
    }
    test("complete OG metadata takes precedence and resolves relative images after redirects") {
        val result = metadata("""<title>fallback</title><meta property="og:title" content="  Cat &amp; Moon ">
            <meta name="twitter:title" content="twitter"><meta property="og:description" content=" A   cozy game ">
            <meta property="og:image" content="../cover.png"><meta name="twitter:image" content="/twitter.jpg">""")
        check(result.title == "Cat & Moon" && result.description == "A cozy game")
        check(result.imageUrl == "https://games.example/cover.png")
    }
    test("twitter and title fallbacks work with missing OG fields") {
        check(metadata("<title>Page title</title><meta name='twitter:title' content='Twitter'>").title == "Twitter")
        check(metadata("<title>Page &amp; title</title>").title == "Page & title")
        check(metadata("<meta name='description' content='hello'>").description == "hello")
    }
    test("empty documents return PartialSuccess with domain and favicon fallbacks") {
        val result = MetadataParser.parse("", "https://game.example/start")
        check(result is MetadataResult.PartialSuccess)
        check(result.metadata.title == "game.example" && result.metadata.imageUrl == "https://game.example/favicon.ico")
    }
    test("apple icon precedes largest favicon; largest favicon is selected otherwise") {
        val icons = """<link rel="icon" sizes="16x16" href="small.png"><link rel="shortcut icon" sizes="256x256" href="large.png">"""
        check(metadata(icons).imageUrl.endsWith("/play/large.png"))
        check(metadata(icons + "<link rel='apple-touch-icon' href='/apple.png'>").imageUrl.endsWith("/apple.png"))
    }
    test("unsafe image schemes are skipped and whitespace / text lengths are bounded") {
        val result = metadata("""<meta property="og:image" content="javascript:alert(1)"><meta name="twitter:image" content="//cdn.example/image.png">
            <meta property="og:title" content="${"x".repeat(120)}"><meta name="description" content="${"y".repeat(600)}">""")
        check(result.imageUrl == "https://cdn.example/image.png" && result.title.length == 80 && result.description.length == 500)
    }
    test("rating add, edit and delete use the correct differences") {
        val first = RatingTotals(0, 0).change(null, 5)
        check(first == RatingTotals(5, 1) && first.average == 5.0)
        val edit = first.change(5, 2)
        check(edit == RatingTotals(2, 1))
        check(edit.change(2, null) == RatingTotals(0, 0))
        check(RatingTotals(10, 3).change(4, 1) == RatingTotals(7, 3))
    }
    test("all 25 rating edits preserve count; same-value updates are idempotent") {
        for (old in 1..5) for (next in 1..5) {
            val total = RatingTotals(5L + old, 2)
            check(total.change(old, next) == RatingTotals(5L + next, 2))
            check(total.change(old, old) == total)
        }
    }
    test("invalid rating and negative totals are rejected") {
        check(runCatching { RatingTotals(0, 0).change(null, 0) }.isFailure)
        check(runCatching { RatingTotals(0, 0).change(null, 6) }.isFailure)
        check(runCatching { RatingTotals(0, 0).change(5, null) }.isFailure)
    }
    test("SSRF rejects loopback, private, link-local, CGNAT and IPv6 local addresses") {
        listOf("127.0.0.1", "10.1.2.3", "172.16.0.1", "172.31.255.254", "192.168.1.1", "169.254.169.254",
            "0.0.0.0", "100.64.0.1", "::1", "fe80::1", "fc00::1", "::ffff:127.0.0.1").forEach {
            check(PublicHostDns.isPrivate(InetAddress.getByName(it))) { "not blocked: $it" }
        }
        check(!PublicHostDns.isPrivate(InetAddress.getByName("8.8.8.8")))
        check(!PublicHostDns.isPrivate(InetAddress.getByName("172.32.0.1")))
        check(runCatching { PublicHostDns.lookup("localhost") }.isFailure)
        check(runCatching { PublicHostDns.lookup("127.0.0.1") }.isFailure)
    }
    println("$passed core tests passed")
}
