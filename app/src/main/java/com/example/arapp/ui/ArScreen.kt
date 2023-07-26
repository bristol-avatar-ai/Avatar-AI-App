package com.example.arapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.arapp.R
import com.example.arapp.ui.theme.ARAppTheme
import io.github.sceneview.ar.ARScene

@Composable
fun ArScreen(
    arViewModel: ArViewModel = viewModel()
) {
    val arUiState by arViewModel.uiState.collectAsState()
    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current
    val touchPosition by remember { arViewModel.touchPosition }

    Box(
        Modifier.fillMaxHeight()
    ) {
        ARScene(
            modifier = Modifier
                .fillMaxSize(),
            nodes = remember { arViewModel.nodes },
            planeRenderer = false,
            onCreate = { arSceneView ->
                arViewModel.addAvatarToScene(arSceneView, coroutine, context)
            },
        )
//        //Allows detection of touches on the ARScene
//        BoxWithConstraints(
//            Modifier
//                .fillMaxHeight(0.95f)
//                .fillMaxWidth(0.95f)
//                .background(Color.Red)
//                .align(Alignment.Center)
//                .pointerInput(Unit) {
//                    detectTapGestures(onLongPress = { press ->
//                        arViewModel.arSceneOnLongPress(press)
//                    })
//                }
//        ) {
//            val constraints = this
//            val offSet = arViewModel.generateCoordinates(constraints, touchPosition)
//
//        }
        Column(
            Modifier.align(Alignment.BottomCenter)
        ) {
            Box {
                FloatingActionMenu(arViewModel)
            }
            BottomBar(arViewModel, arUiState)
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBar(
    arViewModel: ArViewModel,
    arUiState: ArUiState
) {
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.Gray,
                    shape = RoundedCornerShape(20.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserInput(
                textFieldValue = textState,
                onTextChanged = { textState = it },
                placeHolderText = { Text(text = "Ask me a question!") },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            )
            BarButton(
                onClick = { },
                icon = painterResource(
                    if (textState.text.isEmpty()) {
                        R.drawable.baseline_mic_24
                    } else {
                        R.drawable.baseline_send_24
                    }
                ),
                description = "Microphone button",
                selected = false
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInput(
    onTextChanged: (TextFieldValue) -> Unit,
    textFieldValue: TextFieldValue,
    placeHolderText: @Composable (() -> Unit),
    colors: TextFieldColors,
    modifier: Modifier
) {
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
        modifier = modifier
    )
}

@Composable
fun BarButton(
    onClick: () -> Unit,
    icon: Painter,
    description: String,
    selected: Boolean
) {
    val backgroundModifier = if (selected) {
        Modifier.background(
            color = MaterialTheme.colorScheme.secondary,
            shape = RoundedCornerShape(14.dp)
        )
    } else {
        Modifier
    }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(50.dp)
            .padding(all = 5.dp)
            .then(backgroundModifier)
    ) {
        Icon(
            icon,
            description,
            Modifier.fillMaxSize()
        )

    }
}

@Composable
fun FloatingActionMenu(arViewModel: ArViewModel) {
    Column {
        ActionButton(
            onClick = { arViewModel.avatarButtonOnClick() },
            color = Color.LightGray,
            values = arViewModel.getActionButtonValues(AvatarButtonType.VISIBILITY)
        )
        ActionButton(
            onClick = { arViewModel.anchorOrFollowButtonOnClick() },
            color = Color.LightGray,
            values = arViewModel.getActionButtonValues(AvatarButtonType.MODE)
        )
        ActionButton(
            onClick = {},
            color = Color.Gray
        )
    }
}

@Composable
fun ActionButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color,
    selected: Boolean = false,
    contentDescription: String = "",
    values: Pair<Int, String> = Pair(R.drawable.robot_face, "")
) {
    val buttonColor = if (selected) {
        Color.DarkGray
    } else {
        color
    }
    if (enabled) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = onClick,
                modifier = Modifier
                    .padding(5.dp)
                    .size(56.dp),
                shape = CircleShape,
                containerColor = buttonColor
            ) {
                Icon(
                    painter = painterResource(id = values.first),
                    contentDescription = "",
                    Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
            Text(
                values.second,
                Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun MenuButton(
    onClick: () -> Unit,
    text: String,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = false)
@Composable
fun ArScreenPreview() {
    ARAppTheme {
        ArScreen()
    }
}

