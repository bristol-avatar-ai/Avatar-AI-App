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
    dismiss: () -> Unit = {},
    title: String = "",
    bodyText: String = "",
    buttonText: String = "Dismiss"
) {
    AlertDialog(
        onDismissRequest = { dismiss() },
        title = {
            Text(title)
        },
        text = {
            Text(bodyText)
        },
        confirmButton = {
            TextButton(
                onClick = { dismiss() }
            ) {
                Text( buttonText)
            }
        }
    )
}

@Preview
@Composable
fun AlertScreenPreview() {
    ARAppTheme {
        AlertScreen(
            bodyText = stringResource(id = R.string.speech_error_message)
        )
    }
}