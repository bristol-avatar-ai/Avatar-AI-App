package com.example.avatar_ai_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.ui.theme.ARAppTheme
import com.example.avatar_ai_app.ui.theme.spacing

@Composable
fun EnableCameraButton(
    onClick: () -> Unit,
) {
    val pressed = remember { mutableStateOf(false) }

    val buttonColor by rememberUpdatedState(
        if (pressed.value) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface
    )

    val textColor by rememberUpdatedState(
        if (pressed.value) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.error_icon),
            contentDescription = "Error Icon",
            modifier = Modifier.size(50.dp)
        )
        Text(
            modifier = Modifier
                .padding(16.dp),
            text = stringResource(id = R.string.camera_permissions_declined_screen),
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier
                .background(color = buttonColor, shape = RoundedCornerShape(20.dp))
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
                }

        ) {
            Text(
                text = stringResource(id = R.string.grant_camera_access_button),
                color = textColor
            )
        }

    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    ARAppTheme {
        EnableCameraButton(onClick = {})
    }
}

