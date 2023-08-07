package com.example.avatar_ai_app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.avatar_ai_app.R

@Composable
fun ActionButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color,
    contentDescription: String = "",
    values: Pair<Int, String> = Pair(R.drawable.robot_face, "")
) {
    val interactionSource = remember { MutableInteractionSource() }

    val buttonColor by rememberUpdatedState(
        if (interactionSource.collectIsPressedAsState().value) Color.DarkGray else color
    )
    val iconTint by rememberUpdatedState(
        if (interactionSource.collectIsPressedAsState().value) Color.White else Color.Black
    )

    if (enabled) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                values.second,
                Modifier.padding(start = 8.dp)
            )
            FloatingActionButton(
                onClick = onClick,
                modifier = Modifier
                    .padding(5.dp)
                    .size(56.dp),
                shape = CircleShape,
                containerColor = buttonColor,
                interactionSource = interactionSource
            ) {
                Icon(
                    painter = painterResource(id = values.first),
                    contentDescription = "",
                    Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    tint = iconTint
                )
            }
        }
    }
}