package com.example.avatar_ai_app.ui.components.chatBox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.avatar_ai_app.shared.MessageType
import com.example.avatar_ai_app.ui.theme.ARAppTheme
import com.example.avatar_ai_app.ui.theme.spacing

@Composable
fun Message(
    type: MessageType,
    string: String,
) {
    ARAppTheme {
        val contentColor: Color = when (type) {
            MessageType.USER -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            MessageType.RESPONSE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.small)
        ) {
            if (type == MessageType.USER) Spacer(Modifier.weight(1f))

            Text(
                text = string,
                modifier = Modifier
                    .background(color = contentColor, shape = RoundedCornerShape(10.dp))
                    .padding(MaterialTheme.spacing.small),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (type == MessageType.RESPONSE) Spacer(Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
fun ChatPreview() {
    ARAppTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            Message(type = MessageType.USER, string = "Hello")
            Message(type = MessageType.RESPONSE, string = "Hello! How can I help?")
        }
    }
}