package app.vibedeck.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vibedeck.data.*
import app.vibedeck.ui.components.*
import app.vibedeck.ui.theme.VibeDeckTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameScreen(state: AddGameState, onInput: (String) -> Unit, onPreview: () -> Unit,
    onPlay: (Game) -> Unit, onSubmit: () -> Unit, onBack: () -> Unit,
    onTitle: (String) -> Unit = {}, onDescription: (String) -> Unit = {}, onImage: (String) -> Unit = {},
    onTags: (String) -> Unit = {}, onOrientation: (String) -> Unit = {}, onSaved: (String) -> Unit = {}) {
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        TopAppBar(title = { Text("เพิ่มเกมของคุณ") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "ย้อนกลับ") }
        })
    }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ทุกไอเดีย\nมีที่ให้เล่น", style = MaterialTheme.typography.headlineLarge)
                    Text("วางลิงก์เกมเว็บ แอปจะอ่านชื่อและภาพตัวอย่างให้ก่อนเปิดเล่น", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.input, onValueChange = onInput, enabled = !state.saving, label = { Text("ลิงก์เกม") },
                        placeholder = { Text("https://example.com") }, modifier = Modifier.fillMaxWidth(), maxLines = 3,
                        shape = MaterialTheme.shapes.medium, isError = state.error != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { keyboard?.hide(); onPreview() }))
                    TextButton(onClick = { clipboard.getText()?.text?.let(onInput) }, enabled = !state.saving) {
                        Icon(Icons.Default.ContentPaste, null, Modifier.padding(end = 8.dp))
                        Text("วางจากคลิปบอร์ด")
                    }
                    Button(onClick = { keyboard?.hide(); onPreview() }, enabled = state.input.isNotBlank() && !state.loading && !state.saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(if (state.loading) "กำลังอ่านข้อมูลเกม…" else "ดูตัวอย่างเกม")
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            if (state.loading) item { ShimmerPlaceholder(Modifier.fillMaxWidth().height(140.dp)) }
            if (state.game != null && !state.loading) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("พร้อมเปิดโลกใหม่หรือยัง", style = MaterialTheme.typography.titleLarge)
                        GameRow(state.game, { onPlay(state.game) })
                        if (state.game.description.isBlank()) Text("เว็บไม่ได้ระบุคำอธิบาย คุณกรอกเองได้ก่อนบันทึก",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = state.game.title, onValueChange = onTitle, enabled = !state.saving && state.savedId == null,
                            label = { Text("ชื่อเกม · ไม่เกิน 80 ตัวอักษร") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = state.game.description, onValueChange = onDescription, enabled = !state.saving && state.savedId == null,
                            label = { Text("คำอธิบาย · ไม่เกิน 500 ตัวอักษร") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = state.game.thumbnailUrl, onValueChange = onImage, enabled = !state.saving && state.savedId == null,
                            label = { Text("URL รูปหน้าปก · เว้นว่างได้") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = state.tagsText, onValueChange = onTags, enabled = !state.saving && state.savedId == null,
                            label = { Text("แท็ก · คั่นด้วยจุลภาค เช่น Puzzle, Arcade") }, modifier = Modifier.fillMaxWidth())
                        Text("ทิศทางหน้าจอ", style = MaterialTheme.typography.titleSmall)
                        listOf("any" to "อัตโนมัติ", "landscape" to "แนวนอน", "portrait" to "แนวตั้ง").forEach { (value, label) ->
                            FilterChip(selected = state.game.orientation == value || (value == "any" && state.game.orientation == "auto"),
                                onClick = { if (!state.saving && state.savedId == null) onOrientation(value) }, label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
                        }
                        Button(onClick = { onPlay(state.game) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Icon(Icons.Default.PlayArrow, null); Text("เล่นเลย", Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(onClick = onSubmit, enabled = !state.saving && state.savedId == null, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text(when { state.saving -> "กำลังบันทึก…"; state.duplicate -> "เกมนี้มีอยู่แล้ว"; state.savedId != null -> "บันทึกแล้ว"; else -> "บันทึกเข้า Store" })
                        }
                        state.savedId?.let { id ->
                            Text(if (state.duplicate) "เกมนี้มีอยู่แล้ว" else "บันทึกเกมเรียบร้อยแล้ว", color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { onSaved(id) }) { Text("ไปหน้าเกมนี้") }
                        }
                    }
                }
            }
            item {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("เกมของคุณยังอยู่กับคุณ", style = MaterialTheme.typography.titleSmall)
                        Text("สถานะบัญชีและข้อมูล Guest ที่เว็บบันทึกไว้จะเก็บบนเครื่องตามอายุ session ของเกม ตั้งค่า Full Screen และ Safe Area ได้ที่คลังของฉัน",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 860, showBackground = true)
@Composable
private fun AddPreview() { VibeDeckTheme { AddGameScreen(AddGameState(input = SampleGames.all.first().url, game = SampleGames.all.first()), {}, {}, {}, {}, {}) } }
