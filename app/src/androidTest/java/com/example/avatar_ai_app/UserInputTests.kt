package com.example.avatar_ai_app

import android.Manifest
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.rule.GrantPermissionRule
import com.example.avatar_ai_app.ui.components.chatBox.SendAndMicButton
import com.example.avatar_ai_app.ui.components.chatBox.UserInput
import org.junit.Rule
import org.junit.Test

class UserInputTests {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun userInputTest() {
        val textValue = mutableStateOf(TextFieldValue())
        val focused = mutableStateOf(false)
        val pressed = mutableStateOf(false)
        val released = mutableStateOf(false)
        val description = "testButton"



        composeTestRule.activity.setContent {
            Row {
                UserInput(
                    onTextChanged = { textValue.value = it },
                    textFieldValue = textValue.value,
                    placeHolderText = {},
                    colors = TextFieldDefaults.textFieldColors(),
                    modifier = Modifier,
                    onTextFieldFocused = { focused.value = it },
                    onFocusChanged = { focused.value }
                )
                SendAndMicButton(
                    onPress = { pressed.value = true
                              focused.value = false},
                    onRelease = { released.value = true },
                    permissionLauncher = null,
                    icon = painterResource(R.drawable.send_icon),
                    description = description,
                    textFieldFocusState = focused.value,
                    recordingPermissionsEnabled = true,
                    isSendEnabled = true
                )
            }
        }
        composeTestRule.onNodeWithText("").performTextInput("Hello, World!")
        assert(textValue.value.text == "Hello, World!")
        assert(focused.value)

        composeTestRule.onNodeWithContentDescription(description).performClick()
        assert(pressed.value)
        assert(released.value)
        assert(!focused.value)
    }
}