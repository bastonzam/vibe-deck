package app.vibedeck.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.vibedeck.ui.AuthGateState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInSheet(state: AuthGateState, onDismiss: () -> Unit, onSignIn: () -> Unit) {
    if (state.visible) ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("เข้าร่วมโลกของผู้สร้าง", style = MaterialTheme.typography.headlineMedium)
            Text("ล็อกอินเพื่อบันทึกเกม ให้ดาว และพูดคุยกับชุมชน ดูและเล่นเกมได้โดยไม่ต้องล็อกอิน")
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onSignIn, enabled = !state.loading, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("เข้าสู่ระบบด้วย Google")
            }
        }
    }
}
