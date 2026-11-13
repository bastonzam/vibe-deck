package app.vibedeck.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vibedeck.data.Game
import app.vibedeck.data.SampleGames
import app.vibedeck.ui.theme.ArtworkTokens
import app.vibedeck.ui.theme.VibeDeckTheme
import coil.compose.AsyncImage
import kotlin.math.abs

@Composable
fun GameArtwork(game: Game, modifier: Modifier = Modifier, imageUrl: String = game.thumbnailUrl, label: String = "ภาพเกม ${game.title}") {
    val colors = ArtworkTokens.palettes[abs(game.id.hashCode() % ArtworkTokens.palettes.size)]
    var loading by remember(imageUrl) { mutableStateOf(imageUrl.isNotBlank()) }
    Box(modifier.background(Brush.linearGradient(colors)).semantics { contentDescription = label }) {
        Canvas(Modifier.matchParentSize()) {
            val r = size.minDimension
            drawCircle(ArtworkTokens.foreground.copy(alpha = .08f), r * .65f, Offset(size.width * .8f, size.height * .3f))
            drawCircle(ArtworkTokens.foreground.copy(alpha = .12f), r * .27f, Offset(size.width * .65f, size.height * .42f))
            drawOval(ArtworkTokens.foreground.copy(alpha = .25f), Offset(size.width * .24f, size.height * .32f), Size(size.width * .65f, r * .34f), style = Stroke(2.dp.toPx()))
            repeat(16) { index ->
                val x = ((index * 73 + game.id.hashCode().mod(97)) % 100) / 100f
                val y = ((index * 37 + 11) % 100) / 100f
                drawCircle(ArtworkTokens.foreground.copy(alpha = .35f), if (index % 3 == 0) 2.dp.toPx() else 1.dp.toPx(), Offset(size.width * x, size.height * y))
            }
        }
        Text(game.title.take(1), Modifier.align(Alignment.Center), style = MaterialTheme.typography.displaySmall,
            color = ArtworkTokens.foreground.copy(alpha = .65f))
        if (imageUrl.isNotBlank()) {
            AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(), onLoading = { loading = true },
                onSuccess = { loading = false }, onError = { loading = false })
            if (loading) ShimmerPlaceholder(Modifier.matchParentSize())
        }
    }
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "โหลดภาพ")
    val alpha by transition.animateFloat(.15f, .45f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "แสงขณะโหลด")
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        .semantics { contentDescription = "กำลังโหลดภาพ" })
}

@Composable
fun VerifiedBadge() {
    Icon(Icons.Default.Verified, "ผู้สร้างยืนยันตัวตนแล้ว", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
}

@Composable
fun RatingPill(rating: Double?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(Icons.Default.Star, "คะแนน", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(rating?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "ยังไม่มี", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun TagChip(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    if (onClick != null) FilterChip(selected = selected, onClick = onClick, label = { Text(text) }, modifier = Modifier.heightIn(min = 48.dp))
    else Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionHeader(title: String, onAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (onAll != null) TextButton(onClick = onAll, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("ดูทั้งหมด")
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.padding(start = 4.dp).size(16.dp))
        }
    }
}

@Composable
fun GameCard(game: Game, onClick: () -> Unit, modifier: Modifier = Modifier.width(186.dp)) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, label = "กดการ์ด")
    Card(onClick = onClick, interactionSource = interaction,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        GameArtwork(game, Modifier.fillMaxWidth().aspectRatio(1.45f))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(game.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (game.isVerified) VerifiedBadge()
            }
            Text(game.tags.firstOrNull() ?: "เกมเว็บ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            RatingPill(game.rating)
        }
    }
}

@Composable
fun FeaturedBanner(game: Game, onDetail: () -> Unit, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onDetail, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Box {
            GameArtwork(game, Modifier.fillMaxWidth().height(176.dp))
            Surface(Modifier.padding(14.dp).align(Alignment.TopStart), color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary, shape = MaterialTheme.shapes.small) {
                Text("คัดสรรจากชุมชน", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(game.title, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(game.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(game.tags.joinToString(" · "), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Button(onClick = onPlay, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Default.PlayArrow, null)
                    Text("เล่นเลย")
                }
            }
        }
    }
}

@Composable
fun GameShelf(games: List<Game>, onDetail: (Game) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(games, key = { it.id }) { game -> GameCard(game, { onDetail(game) }) }
    }
}

@Composable
fun GameRow(game: Game, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameArtwork(game, Modifier.size(76.dp).clip(MaterialTheme.shapes.small))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(game.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(game.tags.firstOrNull() ?: "เกมเว็บ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RatingPill(game.rating)
                    if (game.isVerified) VerifiedBadge()
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun EmptyState(title: String, description: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(Icons.Default.SportsEsports, null, Modifier.padding(24.dp).size(48.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameCardPreview() { VibeDeckTheme { GameCard(SampleGames.all.first(), {}) } }

@Preview(widthDp = 380, showBackground = true)
@Composable
private fun FeaturedBannerPreview() { VibeDeckTheme { FeaturedBanner(SampleGames.all.first(), {}, {}) } }
