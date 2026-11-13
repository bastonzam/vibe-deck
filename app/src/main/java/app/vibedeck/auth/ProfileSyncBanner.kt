package app.vibedeck.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileSyncBanner(state: ProfileSyncState, onRetry: () -> Unit) {
    if (state.phase != ProfileSyncPhase.ERROR && state.phase != ProfileSyncPhase.SYNCING) return
    Surface(modifier = Modifier.statusBarsPadding(), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.phase == ProfileSyncPhase.SYNCING) {
                Text("เข้าสู่ระบบแล้ว · กำลังบันทึกโปรไฟล์", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                Text(state.message.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("ลองบันทึกโปรไฟล์อีกครั้ง") }
            }
        }
    }
}
