package com.example.avatar_ai_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.avatar_ai_app.R

@Composable
fun ChatResponse(
    responseText: String = "",
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .padding(5.dp)
            .background(
                color = colorResource(R.color.text_response),
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, Color.Gray, RoundedCornerShape(20.dp))
            .clickable {
                onClick()
            }
    ) {
        Text(
            text = responseText,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(5.dp),
        )
    }
}