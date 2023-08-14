package com.example.avatar_ai_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
        modifier = Modifier.background(color = MaterialTheme.colorScheme.surface )
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuButton(size = 20.dp, onClick = onClick, iconId = iconId)
        Text(text = text, textAlign = TextAlign.Center)
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