package com.example.avatar_ai_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun LoadingScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Text(
            text = "Loading",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}