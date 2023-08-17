package com.example.avatar_ai_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.ui.theme.ARAppTheme
import com.example.avatar_ai_app.ui.theme.spacing

@Composable
fun SettingsMenu(
    showMenu: Boolean,
    dismissMenu: () -> Unit,
    languageButtonOnClick: () -> Unit
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = dismissMenu,
        modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)
    ) {
        SettingsMenuItem(
            onClick = languageButtonOnClick,
            iconId = R.drawable.language_icon,
            text = "Language"
        )
    }
}

@Composable
fun SettingsMenuItem(
    onClick: () -> Unit,
    iconId: Int,
    text: String
) {
    val pressed = remember { mutableStateOf(false) }

    val iconAndTextColor by rememberUpdatedState(
        if (pressed.value) MaterialTheme.colorScheme.inverseOnSurface
        else MaterialTheme.colorScheme.onSurface
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.small)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            onClick()
                            pressed.value = true
                            awaitRelease()
                        } finally {
                            pressed.value = false
                        }
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(size = 20.dp),
            painter = painterResource(id = iconId),
            tint = iconAndTextColor,
            contentDescription = null
        )
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = iconAndTextColor
        )
    }
}

@Preview
@Composable
fun SettingsMenuPreview() {
    ARAppTheme {
        SettingsMenu(showMenu = true, dismissMenu = {}, languageButtonOnClick = {})
    }
}

@Preview
@Composable
fun SettingsMenuItemPreview() {
    ARAppTheme {
        SettingsMenuItem(
            onClick = {},
            iconId = R.drawable.language_icon,
            text = "Language"
        )
    }
}