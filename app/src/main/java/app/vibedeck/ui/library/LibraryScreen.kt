package app.vibedeck.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vibedeck.data.*
import app.vibedeck.ui.components.*
import app.vibedeck.ui.theme.VibeDeckTheme

@Composable
fun LibraryScreen(state: LibraryScreenState, onTab: (Boolean) -> Unit, onPlay: (Game) -> Unit,
    onDetail: (Game) -> Unit, onFavorite: (String) -> Unit, onStore: () -> Unit, onSettings: () -> Unit, onRetry: () -> Unit = {}) {
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("คลังของฉัน", style = MaterialTheme.typography.headlineLarge)
                Text("โลกโปรดของคุณ อยู่ตรงนี้", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Tune, "ตั้งค่าการแสดงผล") }
        }
        TabRow(selectedTabIndex = if (state.favoritesTab) 1 else 0) {
            Tab(selected = !state.favoritesTab, onClick = { onTab(false) }, text = { Text("ล่าสุด (${state.library.recent.size})") })
            Tab(selected = state.favoritesTab, onClick = { onTab(true) }, text = { Text("ที่ชอบ (${state.library.favorites.size})") })
        }
        AnimatedContent(targetState = state.favoritesTab, label = "เปลี่ยนคลัง") { favorites ->
            val games = if (favorites) state.library.favorites else state.library.recent
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.error?.let { message -> item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("ลองอีกครั้ง") }
                } }
                if (games.isEmpty()) item {
                    EmptyState(if (favorites) "เก็บเกมที่ทำให้ยิ้มไว้ที่นี่" else "การผจญภัยยังรอคุณอยู่",
                        if (favorites) "แตะหัวใจในหน้าเกม แล้วกลับมาเล่นได้ทุกเมื่อ" else "เลือกเกมแรกจาก Store หรือเพิ่มลิงก์เกมของคุณ", "ไปสำรวจ Store", onStore)
                }
                items(games, key = { it.url }) { game ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GameRow(game, { onDetail(game) }) {
                            IconButton(onClick = { onPlay(game) }) { Icon(Icons.Default.PlayArrow, "เล่น ${game.title}") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onFavorite(game.url) }) {
                                val favorite = state.library.favorites.any { it.url == game.url }
                                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, Modifier.size(18.dp))
                                Text(if (favorite) "นำออกจากที่ชอบ" else "เก็บไว้ในที่ชอบ", Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 800, showBackground = true)
@Composable
private fun LibraryPreview() { VibeDeckTheme { Surface { LibraryScreen(LibraryScreenState(LibraryState(SampleGames.all.take(4))), {}, {}, {}, {}, {}, {}) } } }
