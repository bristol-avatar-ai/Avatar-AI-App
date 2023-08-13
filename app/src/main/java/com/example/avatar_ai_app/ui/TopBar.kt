package com.example.avatar_ai_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.ui.components.BottomShadow
import com.example.avatar_ai_app.ui.components.MenuButton
import com.example.avatar_ai_app.ui.theme.ARAppTheme

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ARAppTheme {
        val topBarColor = MaterialTheme.colorScheme.surface
        Column (modifier = modifier){
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
        )
    }
}