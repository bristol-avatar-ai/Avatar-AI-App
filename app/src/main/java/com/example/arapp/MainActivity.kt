package com.example.arapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.arapp.audio.AudioRecorder
import com.example.arapp.controller.Controller
import com.example.arapp.network.TranscriptionAPIServiceException
import com.example.arapp.network.TranscriptionApi
import com.example.arapp.ui.ArScreen
import com.example.arapp.ui.ArUiState
import com.example.arapp.ui.ArViewModel
import com.example.arapp.ui.theme.ARAppTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    // Delegate to viewModels to retain its value through
    // configuration changes.
    private val arViewModel: ArViewModel by viewModels()

    /*
    * Save an instance of ActivityResultLauncher by registering
    * the permissions callback. This handles the user's response
    * to the system permissions dialog.
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Set recordAudioReady to true if permission is granted.
            arViewModel.setRecordingEnabled(it ?: false)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arViewModel.initController(this)
        // Request audio permission.
        requestRecordAudioPermissionIfMissing()

        setContent {
            ARAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    ArScreen(arViewModel)
                }
            }
        }
    }

    /*
    * This function checks for audio recording permission
    * and requests it if missing.
     */
    private fun requestRecordAudioPermissionIfMissing() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            arViewModel.setRecordingEnabled(true)
        } else {
            // Permission is missing, request for it.
            requestRecordAudioPermission()
        }
    }

    /*
    * This function request for audio recording permission, displaying
    * a reason in a dialog box first if shouldShowRequestPermissionRationale
    * is true.
     */
    private fun requestRecordAudioPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            // Display a message explaining why the permission is required.
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.audio_permission_dialog_title)
                .setMessage(R.string.audio_permission_dialog_message)
                .setCancelable(false)
                .setPositiveButton(R.string.audio_permission_dialog_button) { _, _ ->
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                .show()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        arViewModel.controller.release()
        arViewModel.resetControllerState()
    }
}