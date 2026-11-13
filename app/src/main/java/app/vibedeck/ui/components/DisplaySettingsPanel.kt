package app.vibedeck.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.vibedeck.ui.DisplayState

@Composable
fun DisplaySettingsPanel(state: DisplayState, onSafeArea: (Boolean) -> Unit, onOrientation: (String) -> Unit, onDark: (Boolean) -> Unit,
    userName: String? = null, onAccount: () -> Unit = {}, onSignOut: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ปรับให้เล่นสบาย", style = MaterialTheme.typography.headlineMedium)
        Text(userName?.let { "สวัสดี $it" } ?: "ยังไม่ได้เข้าสู่ระบบ", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = if (userName == null) onAccount else onSignOut) { Text(if (userName == null) "เข้าสู่ระบบด้วย Google" else "ออกจากระบบ") }
        Text("พื้นที่แสดงผล", style = MaterialTheme.typography.titleMedium)
        listOf(false to "Full Screen · เต็มขอบจอ", true to "Safe Area · เว้นกล้องและแถบระบบ").forEach { (value, label) ->
            FilterChip(selected = state.safeArea == value, onClick = { onSafeArea(value) }, label = { Text(label) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
        }
        Text("แนวหน้าจอ", style = MaterialTheme.typography.titleMedium)
        listOf("auto" to "อัตโนมัติ · ตามเว็บหรือเซ็นเซอร์", "landscape" to "แนวนอน", "portrait" to "แนวตั้ง").forEach { (value, label) ->
            FilterChip(selected = state.orientation == value, onClick = { onOrientation(value) }, label = { Text(label) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
        }
        Text("หน้าตาแอป", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = state.dark, onClick = { onDark(true) }, label = { Text("ธีมมืด") }, modifier = Modifier.heightIn(min = 48.dp))
            FilterChip(selected = !state.dark, onClick = { onDark(false) }, label = { Text("ธีมสว่าง") }, modifier = Modifier.heightIn(min = 48.dp))
        }
        Text("จำค่านี้ไว้สำหรับการเปิดเกมครั้งถัดไป สถานะบัญชีและ Guest ที่เว็บบันทึกไว้จะเก็บบนเครื่องตามอายุ session",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
