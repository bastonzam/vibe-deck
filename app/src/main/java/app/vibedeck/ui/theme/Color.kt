package app.vibedeck.ui.theme

import androidx.compose.ui.graphics.Color

internal val Ink = Color(0xFF121019)
internal val NightSurface = Color(0xFF1C1925)
internal val NightContainer = Color(0xFF282331)
internal val Paper = Color(0xFFF8F5FC)
internal val PaperSurface = Color(0xFFFFFFFF)
internal val PaperContainer = Color(0xFFEFE9F6)
internal val Neon = Color(0xFFC5F277)
internal val DeepLime = Color(0xFF456900)
internal val OnNeon = Color(0xFF1A2608)
internal val SoftWhite = Color(0xFFF5F0FC)
internal val Muted = Color(0xFFBDB5CA)
internal val LightMuted = Color(0xFF655D72)
internal val Border = Color(0xFF494151)
internal val LightBorder = Color(0xFFD2C9DC)
internal val Error = Color(0xFFFFB4AB)

/** Artwork-only palettes; interface colors come from MaterialTheme. */
object ArtworkTokens {
    val palettes = listOf(
        listOf(Color(0xFF554588), Color(0xFF222C50)),
        listOf(Color(0xFF276A68), Color(0xFF182E43)),
        listOf(Color(0xFF89573E), Color(0xFF3A254C)),
        listOf(Color(0xFF6A3968), Color(0xFF28284E)),
        listOf(Color(0xFF54714E), Color(0xFF1D3542)),
    )
    val scrim = Color(0xFF100E19)
    val foreground = Color(0xFFF9F5FF)
}
