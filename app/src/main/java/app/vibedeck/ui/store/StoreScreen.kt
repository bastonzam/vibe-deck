package app.vibedeck.ui.store

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vibedeck.data.*
import app.vibedeck.ui.components.*
import app.vibedeck.ui.theme.VibeDeckTheme

val GameCategories = listOf("ทั้งหมด", "Arcade", "Puzzle", "Idle", "Platformer", "Strategy")

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StoreScreen(state: StoreState, onCategory: (String) -> Unit, onDetail: (Game) -> Unit,
    onPlay: (Game) -> Unit, onSearch: () -> Unit, onAdd: () -> Unit, onAll: (String) -> Unit, onRefresh: () -> Unit = {}) {
    val filtered = state.games.filter { state.category == "ทั้งหมด" || state.category in it.tags }
    val notice = state.warning ?: state.error?.takeIf { filtered.isNotEmpty() }
    PullToRefreshBox(isRefreshing = state.loading, onRefresh = onRefresh) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("VibeDeck", style = MaterialTheme.typography.headlineLarge)
                        Text("ค้นพบเกมใหม่จากชุมชน", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "ค้นหาเกม") }
                    FilledTonalIconButton(onClick = onAdd) { Icon(Icons.Default.Add, "เพิ่มเกม") }
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GameCategories) { tag -> TagChip(tag, state.category == tag) { onCategory(tag) } }
                }
            }
            if (notice != null) item { CatalogNotice(notice, onRefresh) }
            if (filtered.isEmpty()) {
                when {
                    state.loading -> item { ShimmerPlaceholder(Modifier.fillMaxWidth().height(110.dp).padding(horizontal = 20.dp)) }
                    state.error != null -> item { EmptyState("ยังโหลดรายการเกมไม่ได้", state.error, "ลองอีกครั้ง", onRefresh) }
                    else -> item { EmptyState("ยังไม่มีเกมในหมวดนี้", "เพิ่มลิงก์เกมของคุณ หรือเลือกดูเกมหมวดอื่น", "เพิ่มเกม", onAdd) }
                }
            } else {
                item { SectionHeader("มาใหม่") { onAll("มาใหม่") } }
                items(filtered.sortedByDescending { it.createdAtMillis }.take(8), key = { "new-${it.id}" }) { game ->
                    StoreGameRow(game, { onDetail(game) }, { onPlay(game) })
                }
                item { SectionHeader("ยอดนิยม") { onAll("ยอดนิยม") } }
                items(filtered.sortedByDescending { it.rating }.take(5), key = { "popular-${it.id}" }) { game ->
                    StoreGameRow(game, { onDetail(game) }, { onPlay(game) })
                }
                val selected = filtered.filter { it.isVerified }.take(5)
                if (selected.isNotEmpty()) {
                    item { SectionHeader("เลือกโดยทีมงาน") { onAll("เลือกโดยทีมงาน") } }
                    items(selected, key = { "selected-${it.id}" }) { game ->
                        StoreGameRow(game, { onDetail(game) }, { onPlay(game) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreGameRow(game: Game, onDetail: () -> Unit, onPlay: () -> Unit) {
    GameRow(game, onDetail, Modifier.padding(horizontal = 20.dp)) {
        IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, "เล่น ${game.title}") }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 860)
@Composable
private fun StorePreview() { VibeDeckTheme { Surface { StoreScreen(StoreState(SampleGames.all), {}, {}, {}, {}, {}, {}) } } }
