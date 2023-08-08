package com.example.avatar_ai_app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.avatar_ai_app.R

@Composable
fun ActionMenuItem(
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color,
    values: Pair<Int, String> = Pair(R.drawable.robot_face, "")
) {
    if (enabled) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                values.second,
                Modifier.padding(start = 8.dp)
            )
            ActionButton(
                onClick = {onClick()},
                color = color,
                iconId = values.first
            )
        }
    }
}