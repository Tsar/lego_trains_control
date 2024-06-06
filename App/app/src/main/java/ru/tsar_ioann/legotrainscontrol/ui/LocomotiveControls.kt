package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tsar_ioann.legotrainscontrol.Train

@Composable
fun LocomotiveControls(locomotive: Train.Locomotive, onLightsChanged: (Float) -> Unit) {
    var lights by remember { mutableFloatStateOf(0f) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (locomotive.hasLights) {
            Slider(
                enabled = locomotive.controllable.value,
                valueRange = 0f..100f,
                value = lights,
                onValueChange = { lights = it },
                onValueChangeFinished = { onLightsChanged(lights) },
                modifier = Modifier.weight(2f),
            )
        } else {
            Spacer(modifier = Modifier.weight(2f))
        }
        Text(
            text = "${locomotive.voltage.intValue} mV\n${locomotive.current.intValue} mA",
            textAlign = TextAlign.Right,
            fontSize = 8.sp,
            lineHeight = 12.sp,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        )
    }
}

class LocomotivePreviewParameterProvider : PreviewParameterProvider<Train.Locomotive> {
    override val values = sequenceOf(
        Train.Locomotive(
            hubName = "Pybricks Hub X",
            hasLights = true,
            voltage = mutableIntStateOf(7777),
            current = mutableIntStateOf(120)
        ),
        Train.Locomotive(
            hubName = "Pybricks Hub Y",
            hasLights = false,
            voltage = mutableIntStateOf(6878),
            current = mutableIntStateOf(522)
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewLocomotiveControls(@PreviewParameter(LocomotivePreviewParameterProvider::class) locomotive: Train.Locomotive) {
    LocomotiveControls(locomotive = locomotive) {}
}
