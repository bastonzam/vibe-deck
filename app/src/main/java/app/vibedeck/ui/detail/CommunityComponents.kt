package app.vibedeck.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vibedeck.data.model.GameComment
import coil.compose.AsyncImage

@Composable
fun RatingEditor(state: DetailState, onRate: (Int?) -> Unit, onCommentText: (String) -> Unit, onSend: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ให้คะแนนเกมนี้", style = MaterialTheme.typography.titleLarge)
        Text(if (state.uid == null) "ล็อกอินแล้วแบ่งปันความรู้สึกหลังเล่นกัน" else state.myRating?.let { "คะแนนของคุณ $it จาก 5 ดาว" } ?: "คุณยังไม่ได้ให้คะแนน",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Horizontally scrollable even at accessibility font sizes; every star is a 48dp target.
        androidx.compose.foundation.lazy.LazyRow {
            items(5) { index ->
                val value = index + 1
                IconButton(onClick = { onRate(value) }, enabled = !state.busy, modifier = Modifier.size(48.dp)) {
                    Icon(if (value <= (state.myRating ?: 0)) Icons.Default.Star else Icons.Default.StarBorder,
                        "ให้ $value ดาว", tint = if (value <= (state.myRating ?: 0)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (state.myRating != null) TextButton(onClick = { onRate(null) }, enabled = !state.busy) { Text("ลบคะแนนของฉัน") }
        Text("คอมเมนต์ (${state.game?.commentCount ?: 0})", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = state.commentText, onValueChange = onCommentText, modifier = Modifier.fillMaxWidth(), minLines = 2,
            label = { Text("เล่าให้ชุมชนฟัง · ไม่เกิน 500 ตัวอักษร") }, enabled = !state.busy)
        Button(onClick = onSend, enabled = state.commentText.isNotBlank() && !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (state.busy) "กำลังส่ง…" else "ส่งคอมเมนต์")
        }
    }
}

@Composable
fun CommentCard(comment: GameComment, isMine: Boolean, enabled: Boolean, onDelete: () -> Unit, onReport: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Surface(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Text(comment.authorName.take(1), style = MaterialTheme.typography.titleSmall)
                    if (comment.authorPhotoUrl.isNotBlank()) AsyncImage(comment.authorPhotoUrl, "รูปโปรไฟล์ ${comment.authorName}", Modifier.matchParentSize())
                }
                Column(Modifier.weight(1f)) {
                    Text(comment.authorName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(relativeTime(comment.createdAtMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menu = true }, enabled = enabled) { Icon(Icons.Default.MoreVert, "เมนูคอมเมนต์") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (isMine) DropdownMenuItem(text = { Text("ลบคอมเมนต์ของฉัน") }, onClick = { menu = false; onDelete() })
                        else DropdownMenuItem(text = { Text("รายงานคอมเมนต์") }, onClick = { menu = false; onReport() })
                    }
                }
            }
            Text(comment.text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

internal fun relativeTime(time: Long?, now: Long = System.currentTimeMillis()): String {
    if (time == null) return "กำลังบันทึก…"
    val minutes = ((now - time).coerceAtLeast(0) / 60_000)
    return when { minutes < 1 -> "เมื่อสักครู่"; minutes < 60 -> "$minutes นาทีที่แล้ว"; minutes < 1440 -> "${minutes / 60} ชม.ที่แล้ว"; else -> "${minutes / 1440} วันที่แล้ว" }
}
