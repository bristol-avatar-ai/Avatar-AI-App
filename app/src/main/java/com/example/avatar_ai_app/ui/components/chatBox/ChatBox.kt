package com.example.avatar_ai_app.ui.components.chatBox

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.avatar_ai_app.chat.ChatMessage
import com.example.avatar_ai_app.shared.MessageType
import com.example.avatar_ai_app.ui.theme.ARAppTheme
import com.example.avatar_ai_app.ui.theme.spacing

@Composable
fun ChatBox(
    messages: List<ChatMessage>,
    showMessages: Boolean,
) {
    val density = LocalDensity.current
    val scrollState = rememberLazyListState()
    var draggedHeight by remember { mutableStateOf(200.dp) }
    val draggableState = rememberDraggableState(onDelta = {
        draggedHeight += with(density) { it.toDp() }
    })
    val maxHeight = if (draggedHeight >= 200.dp) draggedHeight else 200.dp

    val interactionSource = remember { MutableInteractionSource() }

    val boxColor by rememberUpdatedState(
        if (interactionSource.collectIsDraggedAsState().value) MaterialTheme.colorScheme.inverseOnSurface
        else MaterialTheme.colorScheme.onSurface
    )

    val backGroundColor by rememberUpdatedState(
        if (showMessages && messages.isNotEmpty()) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface
    )

    ARAppTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
                .graphicsLayer()
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    interactionSource = interactionSource,
                    reverseDirection = true,
                )
                .background(backGroundColor)
                .animateContentSize(

                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.small),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 5.dp)
                        .background(
                            color = boxColor,
                            shape = RoundedCornerShape(20.dp)
                        )
                )
            }
            if (messages.isNotEmpty() && showMessages) {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(min = 0.dp, max = maxHeight)
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.BottomCenter),
                    state = scrollState,
                    reverseLayout = true

                ) {
                    messages.forEach { message ->
                        when (message.type) {
                            MessageType.USER -> item {
                                Message(
                                    type = message.type,
                                    string = message.string
                                )
                            }

                            MessageType.RESPONSE -> item {
                                Message(
                                    type = message.type,
                                    string = message.string
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}