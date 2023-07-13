package com.example.arapp

import android.annotation.SuppressLint
import android.graphics.Bitmap.Config
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.example.arapp.ui.theme.ARAppTheme
import io.github.sceneview.ar.ArSceneView
import com.google.ar.core.Config.LightEstimationMode

class MainActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ARAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ArView()
                }
            }
        }
    }
}

@SuppressLint("InflateParams")
@Composable
fun ArView() {
    var sceneView: ArSceneView

    fun initArFragment(view: View){
        sceneView = view.findViewById<ArSceneView>(R.id.sceneView).apply {
            lightEstimationMode = LightEstimationMode.ENVIRONMENTAL_HDR
            planeRenderer.isVisible = false
        }
    }

    AndroidView(factory = { context ->
        val view: View = LayoutInflater.from(context).inflate(R.layout.fragment_ar, null)
        initArFragment(view)
        view
    })
}

@Preview(showBackground = true)
@Composable
fun ArViewPreview() {
    ARAppTheme {
        ArView()
    }
}