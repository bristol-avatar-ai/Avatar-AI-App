package com.example.avatar_ai_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.language.Language
import com.example.avatar_ai_app.ui.MainViewModel
import com.example.avatar_ai_app.ui.theme.ARAppTheme
import com.example.avatar_ai_app.ui.theme.spacing

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun LanguageSelectionMenu(
    currentLanguage: Language,
    mainViewModel: MainViewModel
) {

    val scrollState = rememberLazyListState()
    val languages = Language.entries

    val languageSelectionState = rememberUpdatedState(newValue = currentLanguage)
    ARAppTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
                .background(color = MaterialTheme.colorScheme.surface),
        ) {
            LazyColumn(
                modifier = Modifier
                    .heightIn(min = 0.dp, max = 200.dp)
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.CenterStart)
                    .padding(MaterialTheme.spacing.medium),
                state = scrollState,
            ) {
                languages.forEach { language: Language ->
                    item {
                        Row(
                            horizontalArrangement = Arrangement.Start
                        ) {
                            LanguageMenuItem(
                                iconResId = if (languageSelectionState.value == language) {
                                    R.drawable.radio_button_checked
                                } else R.drawable.radio_button_unchecked,
                                languageString = language.string,
                                onClick = { mainViewModel.onLanguageSelectionResult(language) }
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageMenuItem(
    iconResId: Int,
    languageString: String,
    onClick: () -> Unit
) {

    val pressed = remember { mutableStateOf(false) }

    val iconAndTextColor by rememberUpdatedState(
        if (pressed.value) MaterialTheme.colorScheme.inverseOnSurface
        else MaterialTheme.colorScheme.onSurface
    )

    Row(modifier = Modifier.pointerInput(Unit){
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
    }) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            tint = iconAndTextColor
        )
        Spacer(Modifier.size(MaterialTheme.spacing.small))
        Text(
            text = languageString,
            color = iconAndTextColor
        )
    }
}

