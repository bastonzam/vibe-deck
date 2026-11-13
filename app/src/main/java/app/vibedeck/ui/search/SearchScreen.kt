package app.vibedeck.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vibedeck.data.*
import app.vibedeck.ui.components.*
import app.vibedeck.ui.store.GameCategories
import app.vibedeck.ui.theme.VibeDeckTheme

@Composable
fun SearchScreen(state: SearchState, onQuery: (String) -> Unit, onCategory: (String) -> Unit,
    onTool: (String) -> Unit, onOrientation: (String) -> Unit, onSearch: () -> Unit,
    onClearHistory: () -> Unit, onReset: () -> Unit, onDetail: (Game) -> Unit, onRetry: () -> Unit = {}) {
    val keyboard = LocalSoftwareKeyboardController.current
    val filters = state.filters
    LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ค้นพบเกมใหม่", style = MaterialTheme.typography.headlineLarge)
                OutlinedTextField(value = filters.query, onValueChange = onQuery, singleLine = true,
                    label = { Text("ชื่อเกม ผู้สร้าง หรือเครื่องมือ AI") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (filters.query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Close, "ล้างคำค้น") } },
                    shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(); keyboard?.hide() }))
            }
        }
        if (filters.query.isBlank() && state.history.isNotEmpty()) {
            item { SectionHeader("ค้นหาล่าสุด", onClearHistory) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.history) { query -> TagChip(query, onClick = { onQuery(query) }) }
                }
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GameCategories) { TagChip(it, filters.category == it) { onCategory(it) } }
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("ทุกเครื่องมือ", "Claude", "Cursor", "ChatGPT", "Gemini")) { TagChip(it, filters.tool == it) { onTool(it) } }
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("ทุกแนวจอ" to "ทุกแนวจอ", "landscape" to "แนวนอน", "portrait" to "แนวตั้ง")) { (value, label) ->
                    TagChip(label, filters.orientation == value) { onOrientation(value) }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(if (filters.collection.isBlank()) "พบ ${state.results.size} เกม" else "${filters.collection} · ${state.results.size} เกม", style = MaterialTheme.typography.titleLarge)
                Text("เกมจากชุมชน VibeDeck", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (filters.category != "ทั้งหมด" || filters.tool != "ทุกเครื่องมือ" || filters.orientation != "ทุกแนวจอ" || filters.collection.isNotBlank()) {
                    TextButton(onClick = onReset) { Text("ล้างตัวกรอง") }
                }
            }
        }
        val notice = state.warning ?: state.error?.takeIf { state.results.isNotEmpty() }
        if (notice != null) item { CatalogNotice(notice, onRetry) }
        if (state.error != null && state.results.isEmpty()) item { EmptyState("ยังโหลดรายการเกมไม่ได้", state.error, "ลองอีกครั้ง", onRetry) }
        else if (state.loading && state.results.isEmpty()) item { ShimmerPlaceholder(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 20.dp)) }
        else if (state.results.isEmpty()) item { EmptyState("ยังไม่เจอเกมที่ใช่", "ลองใช้คำค้นสั้นลง หรือเลือกหมวดหมู่อื่น", "ล้างคำค้นและตัวกรอง", { onQuery(""); onReset() }) }
        items(state.results, key = { it.id }) { game ->
            GameRow(game, { onSearch(); keyboard?.hide(); onDetail(game) }, Modifier.padding(horizontal = 20.dp))
        }
    }
}

@Preview(widthDp = 390, heightDp = 860, showBackground = true)
@Composable
private fun SearchPreview() { VibeDeckTheme { Surface { SearchScreen(SearchState(results = SampleGames.all), {}, {}, {}, {}, {}, {}, {}, {}) } } }
