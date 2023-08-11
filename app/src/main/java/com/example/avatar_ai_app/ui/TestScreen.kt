package com.example.avatar_ai_app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.avatar_ai_app.ar.ArViewModel
import com.example.avatar_ai_app.ui.components.EnableCameraButton
import com.example.avatar_ai_app.ui.components.LoadingScreen
import com.example.avatar_ai_app.ui.components.SendAndMicButton
import com.example.avatar_ai_app.ui.components.UserInput
import io.github.sceneview.ar.ARScene

@Composable
fun TestScreen(
    mainViewModel: MainViewModel = viewModel(),
    arViewModel: ArViewModel = viewModel(),
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val arState by arViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val touchPosition by remember { mainViewModel.touchPosition }
    val focusRequester = remember { mainViewModel.focusRequester }

    SideEffect {
        val cameraPermissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val recordingPermissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        mainViewModel.updatePermissionStatus(Manifest.permission.CAMERA, cameraPermissionStatus)
        mainViewModel.updatePermissionStatus(
            Manifest.permission.RECORD_AUDIO,
            recordingPermissionStatus
        )
    }

    val isCameraEnabled by mainViewModel.isCameraEnabled.collectAsState()
    val isRecordingEnabled by mainViewModel.isRecordingEnabled.collectAsState()

    val cameraPermissionResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            mainViewModel.onPermissionResult(
                permission = Manifest.permission.CAMERA,
                isGranted = isGranted
            )
        }
    )

    Box(
        Modifier.fillMaxHeight()
    ) {
        if (!uiState.isLoaded) {
            LoadingScreen()
        } else {
            LaunchedEffect(isCameraEnabled) {
                if (!isCameraEnabled) {
                    cameraPermissionResultLauncher.launch(Manifest.permission.CAMERA)
                }
            }
            //If camera is enabled load the ArScene, otherwise load the camera enable button
            when (isCameraEnabled) {
                true -> {
                    ARScene(
                        modifier = Modifier
                            .fillMaxSize(),
                        nodes = remember { arViewModel.nodes },
                        planeRenderer = false,
                        onCreate = { arSceneView ->
                            arViewModel.initialiseArScene(arSceneView)
                            arViewModel.addModelToScene(arSceneView, ArViewModel.ModelType.AVATAR)
                        },
                    )
                }
                false -> {
                    Box(Modifier.align(Alignment.Center)) {
                        EnableCameraButton(
                            onClick = {
                                cameraPermissionResultLauncher.launch(Manifest.permission.CAMERA)
                            }
                        )
                    }
                }
            }
            //Adds an invisible box to detect touches on the ArScreen and request focus
            BoxWithConstraints(
                Modifier
                    .fillMaxHeight(0.95f)
                    .fillMaxWidth(0.95f)
                    .align(Alignment.Center)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                arViewModel.dismissActionMenu()
                                focusRequester.requestFocus()
                            }
                        )
                    }
                    .focusRequester(focusRequester)
                    .focusable()
            ) {}
            //Adjust the position of the BottomBar depending on the keyboard state
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                TopBar( onClick = {} )
                Spacer(Modifier.weight(1f))
                BottomBar(
                    mainViewModel = mainViewModel,
                    uiState = uiState,
                    isRecordingEnabled = isRecordingEnabled
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBar(
    mainViewModel: MainViewModel,
    uiState: UiState,
    isRecordingEnabled: Boolean,
) {
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mainViewModel.textState
    }

    var textFieldFocusState by remember { mainViewModel.textFieldFocusState }

    val audioPermissionResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            mainViewModel.onPermissionResult(
                permission = Manifest.permission.RECORD_AUDIO,
                isGranted = isGranted
            )
        }
    )

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
                )
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                Modifier.size(5.dp)
            )
            UserInput(
                textFieldValue = textState,
                onTextChanged = { textState = it },
                placeHolderText = { Text(stringResource(uiState.textFieldStringResId)) },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.LightGray,
                ),
                modifier = Modifier.weight(1f),
                onTextFieldFocused = { focused: Boolean ->
                    textFieldFocusState = focused
                },
                onFocusChanged = {
                    mainViewModel.updateInputMode()
                }
            )
            SendAndMicButton(
                onPress = { mainViewModel.sendButtonOnPress() },
                onRelease = { mainViewModel.sendButtonOnRelease() },
                permissionLauncher = audioPermissionResultLauncher,
                icon = painterResource(mainViewModel.getMicOrSendIcon()),
                textFieldFocusState = textFieldFocusState,
                recordingPermissionsEnabled = isRecordingEnabled
            )
        }
    }
}

