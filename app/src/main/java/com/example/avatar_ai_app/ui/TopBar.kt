package com.example.avatar_ai_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.ui.components.MenuButton
import com.example.avatar_ai_app.ui.theme.ARAppTheme

@Composable
fun TopBar(
    languageButtonOnClick: () -> Unit,
    avatarButtonOnClick: () -> Unit
) {
    val topBarColor = Color.DarkGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopCenter)
            .background(color = topBarColor)
    ) {
        MenuButton(
            onClick = avatarButtonOnClick,
            color = topBarColor,
            iconId = R.drawable.robot_face
        )
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        )
        MenuButton(
            onClick = languageButtonOnClick,
            color = topBarColor,
            iconId = R.drawable.language_icon
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    ARAppTheme {
        TopBar(
            avatarButtonOnClick = {},
            languageButtonOnClick = {}
        )
    }
}