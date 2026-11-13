package app.vibedeck.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vibedeck.data.*
import app.vibedeck.ui.components.*
import app.vibedeck.ui.theme.VibeDeckTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(state: DetailState, similar: List<Game>, onBack: () -> Unit, onPlay: (Game) -> Unit,
    onFavorite: () -> Unit, onDetail: (Game) -> Unit, onRate: (Int?) -> Unit = {},
    onCommentText: (String) -> Unit = {}, onSendComment: () -> Unit = {}, onDeleteComment: (String) -> Unit = {},
    onReport: (String, String?) -> Unit = { _, _ -> }, onLoadMore: () -> Unit = {}, onRefreshComments: () -> Unit = {},
    onRetry: () -> Unit = {}, onMessageShown: () -> Unit = {}, onAdd: (String) -> Unit = {}) {
    val game = state.game
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var expanded by rememberSaveable(game?.id) { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("เล่นไม่ได้") }
    var reportCommentId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); onMessageShown() }
    }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("รายละเอียดเกม", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "ย้อนกลับ") } }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (game != null) Surface(tonalElevation = 4.dp) {
                Button(onClick = { onPlay(game) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).heightIn(min = 56.dp)) {
                    Icon(Icons.Default.PlayArrow, null)
                    Text("เล่นเลย · ไม่ต้องติดตั้ง", Modifier.padding(start = 8.dp))
                }
            }
        }) { padding ->
        if (game == null && state.loading) {
            ShimmerPlaceholder(Modifier.padding(padding).fillMaxWidth().height(250.dp))
        } else if (game == null) {
            Box(Modifier.padding(padding)) { EmptyState("หาเกมนี้ไม่พบ", "กลับไปค้นหาเกมใหม่ใน Store ได้เลย", "ย้อนกลับ", onBack) }
        } else LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (state.error != null) item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("ลองอีกครั้ง") }
                }
            }
            item { GameArtwork(game, Modifier.fillMaxWidth().aspectRatio(1.6f)) }
            item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(game.title, style = MaterialTheme.typography.headlineLarge)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(game.developerName, Modifier.weight(1f, fill = false), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (game.isVerified) VerifiedBadge()
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RatingPill(game.rating)
                        Text("เกมเว็บ · เล่นฟรี", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("จาก ${game.ratingCount} โหวต", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(game.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { TagChip(when (game.orientation) { "landscape" -> "แนวนอน"; "portrait" -> "แนวตั้ง"; else -> "หมุนอัตโนมัติ" }) }
                    item { TagChip(if (game.usesAudio) "แนะนำให้เปิดเสียง" else "เล่นแบบปิดเสียงได้") }
                    item { TagChip(game.aiTools.joinToString().ifBlank { "ไม่ระบุเครื่องมือ AI" }) }
                }
            }
            item {
                FlowRow(Modifier.padding(horizontal = 12.dp)) {
                    TextButton(onClick = onFavorite, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(if (state.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                        Text(if (state.favorite) "ชอบแล้ว" else "ที่ชอบ", Modifier.padding(start = 6.dp))
                    }
                    TextButton(onClick = {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${game.title}\n${game.url}")
                        }, "แชร์เกม"))
                    }, modifier = Modifier.heightIn(min = 48.dp)) { Icon(Icons.Default.Share, null); Text("แชร์", Modifier.padding(start = 6.dp)) }
                    TextButton(onClick = { reportCommentId = null; reportOpen = true }, enabled = state.saved && !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Default.Flag, null); Text("รายงาน", Modifier.padding(start = 6.dp))
                    }
                }
            }
            if (state.saved) {
                item { RatingEditor(state, onRate, onCommentText, onSendComment) }
                item { TextButton(onClick = onRefreshComments, modifier = Modifier.padding(horizontal = 12.dp)) { Text("รีเฟรชคอมเมนต์") } }
                if (state.comments.isEmpty() && !state.commentsLoading && state.commentsError == null) item {
                    Text("ยังไม่มีคอมเมนต์ มาเริ่มบทสนทนากัน", Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(state.comments, key = { "comment-${it.id}" }) { comment ->
                    CommentCard(comment, comment.uid == state.uid, !state.busy,
                        { onDeleteComment(comment.id) }, { reportCommentId = comment.id; reportOpen = true })
                }
                if (state.commentsError != null) item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text(state.commentsError, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = if (state.comments.isEmpty()) onRefreshComments else onLoadMore) { Text("ลองอีกครั้ง") }
                    }
                }
                if (state.commentsLoading) item { ShimmerPlaceholder(Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 20.dp)) }
                if (state.hasMore) item {
                    LaunchedEffect(state.comments.lastOrNull()?.id) { if (!state.commentsLoading && state.commentsError == null) onLoadMore() }
                    TextButton(onClick = onLoadMore, enabled = !state.commentsLoading, modifier = Modifier.padding(horizontal = 12.dp)) { Text("โหลดคอมเมนต์เพิ่มเติม") }
                }
            } else if (!state.loading) item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("เกมนี้ยังไม่ได้บันทึกเข้า Store บันทึกก่อนเพื่อให้ดาวและคอมเมนต์ได้", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onAdd(game.url) }) { Text("เพิ่มเกมนี้เข้า Store") }
                }
            }
            if (game.screenshots.isNotEmpty()) {
                item { SectionHeader("บรรยากาศในเกม") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(game.screenshots) { index, image ->
                            Card { GameArtwork(game.copy(id = "${game.id}-$index"), Modifier.width(260.dp).aspectRatio(1.6f), image,
                                "ภาพตัวอย่าง ${game.title} ลำดับที่ ${index + 1}") }
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("เกี่ยวกับเกมนี้", style = MaterialTheme.typography.titleLarge)
                    Text(game.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "ย่อคำอธิบาย" else "อ่านเพิ่มเติม") }
                    if (game.id.startsWith("game-")) Text("ข้อมูลตัวอย่าง · URL นี้ใช้สาธิตและยังไม่ใช่เกมจริง",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (similar.isNotEmpty()) {
                item { SectionHeader("เกมที่คล้ายกัน") }
                item { GameShelf(similar, onDetail) }
            }
        }
    }
    if (reportOpen) AlertDialog(onDismissRequest = { reportOpen = false }, title = { Text("รายงานเกม") },
        text = {
            Column {
                Text("เลือกเหตุผลเพื่อให้ทีมงานตรวจสอบ", style = MaterialTheme.typography.bodyMedium)
                listOf("เล่นไม่ได้", "เนื้อหาไม่เหมาะสม", "ละเมิดสิทธิ์", "อื่น ๆ").forEach { item ->
                    TextButton(onClick = { reason = item }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        RadioButton(selected = reason == item, onClick = null)
                        Text(item, Modifier.weight(1f).padding(start = 8.dp))
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = {
            reportOpen = false
            onReport(reason, reportCommentId)
        }, enabled = !state.busy) { Text("ส่งรายงาน") } }, dismissButton = { TextButton(onClick = { reportOpen = false }) { Text("ยกเลิก") } })
}

@Preview(widthDp = 390, heightDp = 860, showBackground = true)
@Composable
private fun DetailPreview() { VibeDeckTheme { DetailScreen(DetailState(SampleGames.all.first()), SampleGames.all.drop(1).take(4), {}, {}, {}, {}) } }
