package com.gtw.filamentmanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@ExperimentalMaterial3Api
@Composable
fun ReadyToWrite(
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Column {
            Text("Ready to write", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    }
}