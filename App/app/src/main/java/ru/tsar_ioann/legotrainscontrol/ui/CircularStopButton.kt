package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun StopIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val size = size.minDimension
        drawRect(
            color = Color.Red,
            size = Size(size, size)
        )
    }
}

@Composable
fun CircularStopButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        enabled = enabled,
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(40.dp)
    ) {
        StopIcon()
    }
}

class BooleanPreviewParameterProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(true, false)
}

@Preview(showBackground = true)
@Composable
fun PreviewCircularStopButton(@PreviewParameter(BooleanPreviewParameterProvider::class) enabled: Boolean) {
    CircularStopButton(enabled = enabled) {}
}
