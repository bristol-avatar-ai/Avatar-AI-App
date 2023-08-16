package com.example.avatar_ai_app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun ActionButton(
    onClick: () -> Unit,
    color: Color,
    contentDescription: String = "",
    iconId: Int
) {
    val interactionSource = remember { MutableInteractionSource() }

    val buttonColor by rememberUpdatedState(
        if (interactionSource.collectIsPressedAsState().value) Color.DarkGray else color
    )
    val iconTint by rememberUpdatedState(
        if (interactionSource.collectIsPressedAsState().value) Color.White else Color.Black
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
            painter = painterResource(iconId),
            contentDescription = contentDescription,
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            tint = iconTint
        )
    }
}
