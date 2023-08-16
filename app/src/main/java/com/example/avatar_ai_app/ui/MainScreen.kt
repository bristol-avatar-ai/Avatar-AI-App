package com.example.avatar_ai_app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import com.example.avatar_ai_app.imagerecognition.ImageRecognitionViewModel
import com.example.avatar_ai_app.ui.components.ChatBox
import com.example.avatar_ai_app.ui.components.EnableCameraButton
import com.example.avatar_ai_app.ui.components.LoadingScreen
import com.example.avatar_ai_app.ui.components.SendAndMicButton
import com.example.avatar_ai_app.ui.components.UserInput
import com.example.avatar_ai_app.ui.theme.ARAppTheme
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARScene
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    mainViewModel: MainViewModel = viewModel(),
    arViewModel: ArViewModel = viewModel(),
    imageViewModel: ImageRecognitionViewModel = viewModel(),
) {
    val uiState by mainViewModel.uiState.collectAsState()
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
        Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
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
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        planeRenderer = false,
                        onCreate = { arSceneView ->
                            mainViewModel.setGraph()
                            mainViewModel.initialiseArScene(arSceneView)
                            mainViewModel.addModelToScene(arSceneView, ArViewModel.ModelType.AVATAR)
                        },
                    )

                    LaunchedEffect(Unit) {
                        while (true) {
                            val frame = arViewModel.arSceneView?.currentFrame
                            if (frame?.camera?.trackingState == TrackingState.TRACKING) {
                                try {
                                    val image: Image = frame.frame.acquireCameraImage()
                                    val bitmap: Bitmap? = imageViewModel.yuv420ToBitmap(image)
                                    image.close()

                                    if (bitmap != null) {
                                        //saveBitmapToFile(imageViewModel.getApplication<Application>().applicationContext, bitmap)
                                        imageViewModel.classifyImage(bitmap)
                                    }
                                } catch (e: Exception) {
                                    Log.e("Camera", "Camera image not available", e)
                                }
                            }
                            delay(500L) // Increased delay to 500 milliseconds for lower frame rate
                        }
                    }

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
                                focusRequester.requestFocus()
                                Log.d("Swipe", "Tap detected")
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
                                mainViewModel.handleSwipe(dragY)
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
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                TopBar(onClick = {})
                Spacer(Modifier.weight(1f))
                ChatBox(
                    messages = uiState.messages,
                    showMessages = uiState.messagesAreShown
                )
                BottomBar(
                    mainViewModel = mainViewModel,
                    uiState = uiState,
                    isRecordingEnabled = isRecordingEnabled
                )
            }
        }
    }
}


fun saveBitmapToFile(context: Context, bitmap: Bitmap) {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    val storageDir = File(context.filesDir, "MyImages")

    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }

    val imageFile = File(storageDir, "JPEG_${timeStamp}_.png")

    try {
        val fos = FileOutputStream(imageFile)

        // Compress the bitmap and save it as PNG format
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)

        fos.close()

        Log.i("SaveBitmap", "Bitmap saved to path: ${imageFile.absolutePath}")
    } catch (e: IOException) {
        Log.e("SaveBitmap", "Failed to save Bitmap.", e)
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

    val isRecordingReady by mainViewModel.isRecordingReady.collectAsState()

    ARAppTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surface),
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
                recordingPermissionsEnabled = isRecordingEnabled,
                isRecordingReady = isRecordingReady
            )
        }
    }
}

