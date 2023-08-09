package com.example.avatar_ai_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.ui.theme.ARAppTheme

@Composable
fun MenuButton(
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

    Box(
        modifier = Modifier
            .size(48.dp)
            .background(color = buttonColor, shape = CircleShape)
    ) {
        IconButton(
            onClick = { onClick() },
            modifier = Modifier
                .fillMaxSize(),
            interactionSource = interactionSource
        ) {
            Icon(
                painter = painterResource(iconId),
                contentDescription = contentDescription,
                Modifier.fillMaxSize(),
                tint = iconTint
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
fun MenuButtonPreview() {
    ARAppTheme {
        MenuButton(
            onClick = {},
            color = Color.Gray,
            iconId = R.drawable.language_icon
        )
    }
}