package com.example.avatar_ai_app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.ui.theme.ARAppTheme

@Composable
fun AlertScreen(
    onDismiss: () -> Unit = {},
    bodyText: String = "",
    onConfirm: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        text = {
            Text(bodyText)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(text = stringResource(id = R.string.confirm_button))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = stringResource(id = R.string.go_back_button))
            }
        }
    )
}

@Preview
@Composable
fun AlertScreenPreview() {
    ARAppTheme {
        AlertScreen(
            bodyText = stringResource(id = R.string.clear_chat_message)
        )
    }
}