package com.example.avatar_ai_app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.avatar_ai_app.ar.ArViewModel
import com.example.avatar_ai_app.ar.ArViewModelFactory
import com.example.avatar_ai_app.chat.ChatViewModel
import com.example.avatar_ai_app.chat.ChatViewModelFactory
import com.example.avatar_ai_app.data.DatabaseViewModel
import com.example.avatar_ai_app.data.DatabaseViewModelFactory
import com.example.avatar_ai_app.imagerecognition.ImageRecognitionViewModel
import com.example.avatar_ai_app.imagerecognition.ImageRecognitionViewModelFactory
import com.example.avatar_ai_app.language.Language
import com.example.avatar_ai_app.shared.ErrorType
import com.example.avatar_ai_app.ui.MainScreen
import com.example.avatar_ai_app.ui.MainViewModel
import com.example.avatar_ai_app.ui.MainViewModelFactory
import com.example.avatar_ai_app.ui.components.CameraPermissionRequestProvider
import com.example.avatar_ai_app.ui.components.PermissionDialog
import com.example.avatar_ai_app.ui.components.RecordAudioPermissionRequestProvider
import com.example.avatar_ai_app.ui.theme.ARAppTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MainActivity"

/**
 * Interface to be implemented by listeners that handle error events.
 *
 * This listener should be passed into the ViewModels and contains a function for displaying error dialog boxes.
 */
interface ErrorListener {
    /**
     * Callback function to show an error dialog box based on the specified [errorType].
     *
     * @param errorType The type of error to handle.
     */
    fun onError(errorType: ErrorType)
}

class MainActivity : ComponentActivity(), ErrorListener {

    private lateinit var databaseViewModel: DatabaseViewModel
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var arViewModel: ArViewModel
    private lateinit var imageViewModel: ImageRecognitionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(application)
        )[DatabaseViewModel::class.java]

        chatViewModel = ViewModelProvider(
            this,
            ChatViewModelFactory(application, Language.ENGLISH, this)
        )[ChatViewModel::class.java]

        arViewModel = ViewModelProvider(
            this,
            ArViewModelFactory(application)
        )[ArViewModel::class.java]

        imageViewModel = ViewModelProvider(
            this,
            ImageRecognitionViewModelFactory(this.application, arViewModel, this)
        )[ImageRecognitionViewModel::class.java]

        mainViewModel =
            ViewModelProvider(
                this,
                MainViewModelFactory(
                    chatViewModel,
                    databaseViewModel,
                    arViewModel,
                    imageViewModel
                )
            )[MainViewModel::class.java]

        mainViewModel.initialiseObservers(this)

        setContent {
            ARAppTheme {
                val dialogQueue = mainViewModel.visiblePermissionDialogQueue
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    MainScreen(mainViewModel)
                    dialogQueue
                        .reversed()
                        .forEach { permission ->
                            PermissionDialog(
                                permissionTextProvider = when (permission) {
                                    Manifest.permission.CAMERA -> {
                                        CameraPermissionRequestProvider()
                                    }

                                    Manifest.permission.RECORD_AUDIO -> {
                                        RecordAudioPermissionRequestProvider()
                                    }

                                    else -> return@forEach
                                },
                                isPermanentlyDeclined = !shouldShowRequestPermissionRationale(
                                    permission
                                ),
                                onDismiss = mainViewModel::dismissDialog,
                                onEnableClick = mainViewModel::dismissDialog,
                                onGoToAppSettingsClick = ::openAppSettings
                            )
                        }
                }
            }
        }
    }

    /**
     * Displays an error dialog box based on the given [errorType].
     *
     * Strings are displayed in the selected language, if possible.
     *
     * @param errorType The type of error to handle.
     */
    override fun onError(errorType: ErrorType) {
        lifecycleScope.launch(Dispatchers.IO) {
            val errorStrings = getErrorStrings(errorType)

            withContext(Dispatchers.Main) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(errorStrings[0])
                    .setMessage(errorStrings[1])
                    .setCancelable(false)
                    .setPositiveButton(errorStrings[2]) { _, _ -> }
                    .show()
            }
        }
    }

    /**
     * Retrieves a set of translated error message strings if available; otherwise, returns them in English.
     *
     * @param errorType The type of error to fetch messages for.
     * @return A list of error message strings.
     */
    private suspend fun getErrorStrings(errorType: ErrorType): List<String> {
        return listOf(
            getString(R.string.error_title),
            when (errorType) {
                ErrorType.GENERIC -> getString(R.string.error_message)
                ErrorType.NETWORK -> getString(R.string.network_error_message)
                ErrorType.RECORDING -> getString(R.string.recording_error_message)
                ErrorType.RECORDING_LENGTH -> getString(R.string.recording_length_error_message)
                ErrorType.SPEECH -> getString(R.string.speech_error_message)
            },
            getString(R.string.error_ok_button)
        ).map {
            chatViewModel.translateOutput(it)
        }
    }

}

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}

