package com.example.arapp.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.sceneview.ar.ARScene

@Composable
fun ArScreen(
    arViewModel: ArViewModel = viewModel(),
) {
    val arUiState by arViewModel.uiState.collectAsState()
    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current

    Column {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            ARScene(
                modifier = Modifier.fillMaxSize(),
                nodes = remember { arViewModel.nodes },
                planeRenderer = false,
                onCreate = { arSceneView ->
                    arViewModel.addAvatarToScene(arSceneView, coroutine, context)
                }
            )
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    AvatarButtons(
                        onHideAvatar = { arViewModel.hideAvatar() },
                        onAnchorAvatar = { arViewModel.anchorAvatar() },
                        onDetachAvatar = { arViewModel.detachAvatar() },
                        onSummonAvatar = { arViewModel.summonAvatar() }
                    )
                }
            }
        }
    }
}

@Composable
fun AvatarButtons(
    onHideAvatar: () -> Unit,
    onAnchorAvatar: () -> Unit,
    onDetachAvatar: () -> Unit,
    onSummonAvatar: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        HideAvatarButton(
            onClick = onHideAvatar
        )
        AnchorAvatarButton(
            onClick = onAnchorAvatar
        )
        DetachAvatarButton(
            onClick = onDetachAvatar
        )
        SummonAvatarButton(
            onClick = onSummonAvatar
        )
    }
}

@Composable
fun SummonAvatarButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick
    )

     {
        Text(text = "Summon")
    }
}

@Composable
fun AnchorAvatarButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick
    ) {
        Text(text = "Anchor")
    }
}

@Composable
fun DetachAvatarButton(
    onClick: () -> Unit
){
    Button(
        onClick = onClick
    ) {
        Text(text = "Detach")
    }
}

@Composable
fun HideAvatarButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick
    ) {
        Text(text = "Hide")
    }
}

@Preview(showBackground = true)
@Composable
fun ArScreenPreview() {
    ARAppTheme {
        ArScreen()
    }
}