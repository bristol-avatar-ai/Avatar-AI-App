package com.example.avatar_ai_app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.avatar_ai_app.ui.components.AlertScreen
import com.example.avatar_ai_app.ui.components.EnableCameraButton
import com.example.avatar_ai_app.ui.components.LoadingScreen
import com.example.avatar_ai_app.ui.components.chatBox.ChatBox
import com.example.avatar_ai_app.ui.components.chatBox.SendAndMicButton
import com.example.avatar_ai_app.ui.components.chatBox.UserInput
import com.example.avatar_ai_app.ui.components.languageMenu.LanguageSelectionMenu
import com.example.avatar_ai_app.ui.components.topbar.TopBar
import com.example.avatar_ai_app.ui.theme.ARAppTheme
import io.github.sceneview.ar.ARScene

private const val TAG = "MainScreen"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel = viewModel(),
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { mainViewModel.focusRequester }

    val isCameraEnabled by mainViewModel.isCameraEnabled.collectAsState()
    val isRecordingEnabled by mainViewModel.isRecordingEnabled.collectAsState()
    val isChatLoaded by mainViewModel.isChatViewModelLoaded.collectAsState()
    val isDatabaseLoaded by mainViewModel.isDatabaseViewModelLoaded.collectAsState()
    val isImageRecognitionLoaded by mainViewModel.isImageViewModelLoaded.collectAsState()
    val isLoading = !isChatLoaded || !isDatabaseLoaded || !isImageRecognitionLoaded
    val keyboardController = LocalSoftwareKeyboardController.current

    val cameraPermissionResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            mainViewModel.onPermissionResult(
                permission = Manifest.permission.CAMERA,
                isGranted = isGranted
            )
        }
    )

    //This value will only update after the initial loading is complete
    //Subsequent reloads of the ChatService will not affect this value
    val startupComplete = remember { mutableStateOf(false) }

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

    Box(
        Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
    ) {

        LaunchedEffect(isCameraEnabled) {
            if (!isCameraEnabled) {
                cameraPermissionResultLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        //This code block won't run until startup is complete
        if (startupComplete.value) {
            //If camera is enabled load the ArScene, otherwise load the camera enable button
            if (isCameraEnabled) {
                ARScene(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    planeRenderer = false,
                    onCreate = { arSceneView ->
                        mainViewModel.initialiseArScene(arSceneView)
                    }
                )
            }
        }

        //Adds an invisible box to detect touches on the ArScreen and request focus
        Box(
            Modifier
                .fillMaxHeight(0.95f)
                .fillMaxWidth(0.95f)
                .align(Alignment.Center)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusRequester.requestFocus()
                            mainViewModel.dismissLanguageMenu()
                        }
                    )
                }
                .pointerInput(Unit) {
                    var dragY = 0F
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            dragY = dragAmount
                        },
                        onDragEnd = {
                            mainViewModel.handleSwipe(
                                pan = dragY,
                                keyboardController = keyboardController
                            )
                        }
                    )
                }
                .focusRequester(focusRequester)
                .focusable()
        ) {
            if (!isCameraEnabled) {
                Box(Modifier.align(Alignment.Center)) {
                    EnableCameraButton(
                        onClick = {
                            cameraPermissionResultLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
        ) {
            TopBar(
                onClick = { mainViewModel.settingsMenuButtonOnClick() },
                menuState = uiState.isSettingsMenuShown,
                onDismiss = { mainViewModel.dismissSettingsMenu() },
                languageButtonOnClick = { mainViewModel.languageSettingsButtonOnClick() },
                clearChatButtonOnClick = { mainViewModel.clearChatButtonOnClick() },
                helpButtonOnClick = { mainViewModel.helpButtonOnClick() }
            )
            Spacer(Modifier.weight(1f))
            BottomBar(
                mainViewModel = mainViewModel,
                uiState = uiState,
                isRecordingEnabled = isRecordingEnabled,
                //locale = currentLocale
            )
        }
        AnimatedVisibility(
            visible = uiState.isLanguageMenuShown,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            LanguageSelectionMenu(
                currentLanguage = uiState.language,
                mainViewModel = mainViewModel,
                context = context
            )
        }
        if (isLoading) {
            LoadingScreen()
        } else {
            LaunchedEffect(startupComplete.value) {
                if (!startupComplete.value) {
                    startupComplete.value = true
                }
            }
        }
        if (uiState.alertIsShown) {
            AlertScreen(
                bodyText = stringResource(uiState.alertResId),
                onDismiss = { mainViewModel.dismissAlertDialogue() },
                onConfirm = { mainViewModel.alertOnClick() }
            )
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
        mutableStateOf(TextFieldValue())
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

    val isRecordingReady by mainViewModel.isRecordingReady.collectAsState()
    val isRecognitionReady by mainViewModel.isRecognitionReady.collectAsState()

    val isSendEnabled = isRecordingReady && isRecognitionReady

    val backGroundColor: Color by animateColorAsState(
        if (textFieldFocusState) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.surface, label = ""
    )

    ARAppTheme {
        Column {
            ChatBox(
                messages = uiState.messages,
                showMessages = uiState.messagesAreShown,
                focusState = textFieldFocusState
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = backGroundColor),
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
                    onPress = {
                        mainViewModel.sendButtonOnPress(textState.text)
                        textState = TextFieldValue()
                    },
                    onRelease = { mainViewModel.sendButtonOnRelease() },
                    permissionLauncher = audioPermissionResultLauncher,
                    icon = painterResource(mainViewModel.getMicOrSendIcon()),
                    textFieldFocusState = textFieldFocusState,
                    recordingPermissionsEnabled = isRecordingEnabled,
                    isSendEnabled = isSendEnabled
                )
            }
        }
    }
}

