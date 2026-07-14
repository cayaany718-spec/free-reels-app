package com.example.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun UpdateDialog(onDismiss: () -> Unit, onUpdate: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cập nhật mới") },
        text = { Text("Một phiên bản mới đã sẵn sàng. Hãy cập nhật để có trải nghiệm tốt nhất.") },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text("Cập nhật")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bỏ qua")
            }
        }
    )
}
