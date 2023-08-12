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
import androidx.lifecycle.ViewModelProvider
import com.example.avatar_ai_app.ar.ArViewModel
import com.example.avatar_ai_app.ar.ArViewModelFactory
import com.example.avatar_ai_app.chat.ChatViewModel
import com.example.avatar_ai_app.chat.ChatViewModelFactory
import com.example.avatar_ai_app.data.DatabaseViewModel
import com.example.avatar_ai_app.data.DatabaseViewModelFactory
import com.example.avatar_ai_app.language.Language
import com.example.avatar_ai_app.shared.ErrorType
import com.example.avatar_ai_app.ui.ArScreen
import com.example.avatar_ai_app.ui.MainViewModel
import com.example.avatar_ai_app.ui.MainViewModelFactory
import com.example.avatar_ai_app.ui.components.CameraPermissionRequestProvider
import com.example.avatar_ai_app.ui.components.PermissionDialog
import com.example.avatar_ai_app.ui.components.RecordAudioPermissionRequestProvider
import com.example.avatar_ai_app.ui.theme.ARAppTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder

private const val TAG = "MainActivity"

/*
* This listener should be passed into the ViewModels and
* contains a function for showing error dialog boxes.
 */
interface ErrorListener {
    fun onError(errorType: ErrorType)
}

class MainActivity : ComponentActivity(), ErrorListener {

    private lateinit var databaseViewModel: DatabaseViewModel
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var arViewModel: ArViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]

        chatViewModel = ViewModelProvider(
            this,
            ChatViewModelFactory(this, Language.ENGLISH, this)
        )[ChatViewModel::class.java]
        //TODO: Remember to update the ChatViewModel's feature list

        arViewModel = ViewModelProvider(
            this,
            ArViewModelFactory(this.application)
        )[ArViewModel::class.java]

        mainViewModel =
            ViewModelProvider(
                this,
                MainViewModelFactory(chatViewModel, this)
            )[MainViewModel::class.java]

        setContent {
            ARAppTheme {
                val dialogQueue = mainViewModel.visiblePermissionDialogQueue
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    ArScreen(
                        mainViewModel,
                        arViewModel
                    )
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

    /*
    * This ErrorListener callback function displays an error dialog box.
     */
    override fun onError(errorType: ErrorType) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.error_title))
            .setMessage(
                when (errorType) {
                    ErrorType.GENERIC -> getString(R.string.error_message)
                    ErrorType.NETWORK -> getString(R.string.network_error_message)
                    ErrorType.RECORDING -> getString(R.string.recording_error_message)
                    ErrorType.RECORDING_LENGTH -> getString(R.string.recording_length_error_message)
                    ErrorType.SPEECH -> getString(R.string.speech_error_message)
                }
            )
            .setCancelable(false)
            .setPositiveButton(getString(R.string.error_ok_button)) { _, _ -> }
            .show()
    }

}

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}