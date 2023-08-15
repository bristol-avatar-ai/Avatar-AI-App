package com.example.avatar_ai_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.ui.components.BottomShadow
import com.example.avatar_ai_app.ui.components.MenuButton
import com.example.avatar_ai_app.ui.components.SettingsMenu
import com.example.avatar_ai_app.ui.theme.ARAppTheme

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    menuState: Boolean
) {

    val showMenu = rememberUpdatedState(newValue = menuState)

    ARAppTheme {
        val topBarColor = MaterialTheme.colorScheme.surface
        Column(modifier = modifier, horizontalAlignment = Alignment.End) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.TopCenter)
                    .background(color = topBarColor)
                    .statusBarsPadding()

            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                MenuButton(
                    onClick = { onClick() },
                    iconId = R.drawable.menu_icon
                )
            }
            Box{
                SettingsMenu(
                    showMenu = showMenu.value,
                    dismissMenu = onDismiss,
                    languageButtonOnClick = {}
                )
            }
            BottomShadow(alpha = 0.15f)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    ARAppTheme {
        TopBar(
            onClick = {},
            menuState = true,
            onDismiss = {}
        )
    }
}