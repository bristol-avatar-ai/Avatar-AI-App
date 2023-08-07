package com.example.arapp.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInput(
    onTextChanged: (TextFieldValue) -> Unit,
    textFieldValue: TextFieldValue,
    onSend: () -> Unit,
    placeHolderText: @Composable (() -> Unit),
    colors: TextFieldColors,
    modifier: Modifier,
    onTextFieldFocused: (Boolean) -> Unit,
    focusState: Boolean,
    onFocusChanged: () -> Unit
) {
    var previousFocusState by remember { mutableStateOf(false) }
    TextField(
        value = textFieldValue,
        onValueChange = { onTextChanged(it) },
        placeholder = placeHolderText,
        colors = colors,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrect = true,
            imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
            onSend = { onSend() }
        ),
        modifier = modifier.onFocusChanged { state ->
            if (previousFocusState != state.isFocused) {
                onTextFieldFocused(state.isFocused)
            }
            previousFocusState = state.isFocused
            onFocusChanged()
        }
    )
}