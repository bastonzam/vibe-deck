package app.vibedeck.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CatalogNotice(message: String, onRetry: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp)) {
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("ลองอัปเดตอีกครั้ง") }
        }
    }
}
