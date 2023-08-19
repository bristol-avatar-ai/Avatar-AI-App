package com.example.avatar_ai_app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.avatar_ai_app.R

@Composable
fun PermissionDialog(
    permissionTextProvider: PermissionTextProvider,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onEnableClick: () -> Unit,
    onGoToAppSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider()
                Text(
                    text = if(isPermanentlyDeclined) {
                        stringResource(id = R.string.grant_permission)
                    } else {
                        stringResource(id = R.string.permission_ok)
                    },
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isPermanentlyDeclined) {
                                onGoToAppSettingsClick()
                            } else {
                                onEnableClick()
                            }
                        }
                        .padding(16.dp)
                )
            }
        },
        title = {
            Text(text = stringResource(id = R.string.permission_required))
        },
        text = {
            Text(
                text = stringResource(id = permissionTextProvider.getDescription(isPermanentlyDeclined))
            )
        },
        modifier = modifier
    )
}

interface PermissionTextProvider {
    fun getDescription(isPermanentlyDeclined: Boolean): Int
}
class CameraPermissionRequestProvider: PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): Int{
        return if (isPermanentlyDeclined) R.string.camera_permissions_declined
         else R.string.camera_permissions_rationale
    }
}

class RecordAudioPermissionRequestProvider: PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): Int {
        return if (isPermanentlyDeclined) R.string.audio_permissions_declined
        else R.string.audio_permissions_rationale
    }
}