package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import ru.tsar_ioann.legotrainscontrol.Train

@Composable
fun LocomotiveControls(locomotive: Train.Locomotive, onLightsChanged: (Float) -> Unit) {
    var lights by remember { mutableFloatStateOf(0f) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = locomotive.hubName, fontSize = 5.sp)
        if (locomotive.hasLights) {
            Slider(
                enabled = locomotive.controllable.value,
                valueRange = 0f..100f,
                value = lights,
                onValueChange = { lights = it },
                onValueChangeFinished = { onLightsChanged(lights) },
            )
        }
    }
}
