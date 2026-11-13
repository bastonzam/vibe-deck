package app.vibedeck.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Neon, onPrimary = OnNeon, primaryContainer = DeepLime, onPrimaryContainer = SoftWhite,
    secondary = Muted, onSecondary = Ink, background = Ink, onBackground = SoftWhite,
    surface = NightSurface, onSurface = SoftWhite, surfaceVariant = NightContainer,
    onSurfaceVariant = Muted, outline = Border, error = Error,
)
private val LightColors = lightColorScheme(
    primary = DeepLime, onPrimary = PaperSurface, primaryContainer = Neon, onPrimaryContainer = OnNeon,
    secondary = LightMuted, background = Paper, onBackground = Ink,
    surface = PaperSurface, onSurface = Ink, surfaceVariant = PaperContainer,
    onSurfaceVariant = LightMuted, outline = LightBorder,
)

@Composable
fun VibeDeckTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = VibeTypography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)),
        content = content,
    )
}
