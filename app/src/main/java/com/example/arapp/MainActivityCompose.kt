package com.example.arapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.arapp.ui.theme.ARAppTheme
import com.google.ar.core.Config
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.Position
import kotlinx.coroutines.launch

class MainActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ARAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ArScreen()
                }
            }
        }
    }
}

lateinit var modelNode: ArModelNode

@Composable
fun ArScreen(){
    val nodes = remember { mutableStateListOf<ArNode>() }
    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current

    Column {
        Box(modifier = Modifier.fillMaxSize()) {
            ARScene(
                modifier = Modifier.fillMaxSize(),
                nodes = nodes,
                planeRenderer = false,
                onCreate = { sceneView ->
                    modelNode = ArModelNode(
                    ).apply {
                        coroutine.launch {
                            loadModelGlb(
                                context = context,
                                glbFileLocation = "models/robot_playground.glb",
                                scaleToUnits = 0.8f,
                                centerOrigin = Position(y = -1.0f)
                            )
                            sceneView.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                            sceneView.addChild(child = modelNode)
                        }
                    }
                }
            )
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Button(
                    enabled = true,
                    onClick = {
                        modelNode.anchor()
                    },
                    modifier = Modifier.padding(all = 16.dp)
                ) {
                    Text(text = "Place Model")

                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ArScreenPreview() {
    ARAppTheme {
        ArScreen()
    }
}