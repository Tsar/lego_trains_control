package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp

class BooleanPreviewParameterProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(true, false)
}

@Preview
@Composable
fun StopIcon(@PreviewParameter(BooleanPreviewParameterProvider::class) enabled: Boolean) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val size = size.minDimension
        drawRect(
            color = if (enabled) Color.Red else Color.Gray,
            size = Size(size, size)
        )
    }
}

@Composable
fun CircularStopButton(enabled: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        Button(
            enabled = enabled,
            onClick = onClick,
            shape = CircleShape,
            modifier = Modifier.size(50.dp)
        ) {}
        StopIcon(enabled = enabled)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCircularStopButton(@PreviewParameter(BooleanPreviewParameterProvider::class) enabled: Boolean) {
    CircularStopButton(enabled = enabled) {}
}
