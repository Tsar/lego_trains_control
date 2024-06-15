package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tsar_ioann.legotrainscontrol.Train

@Composable
fun TrainControls(train: Train, onSpeedChanged: (Float) -> Unit, onLightsChanged: (Train.Locomotive, Float) -> Unit) {
    var speed by remember { mutableFloatStateOf(0f) }
    val controllable = train.locomotives.all { it.controllable.value }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(15.dp)) {
        StatusCircle(color = if (controllable) Color.Green else Color.Red)
        Column(modifier = Modifier.padding(start = 15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = train.name, fontSize = 20.sp, modifier = Modifier.weight(3f), textAlign = TextAlign.Left)
                Column(modifier = Modifier.weight(2f)) {
                    train.locomotives.forEach { locomotive ->
                        LocomotiveControls(
                            locomotive = locomotive,
                            onLightsChanged = { onLightsChanged(locomotive, it) },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpeedSlider(
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                    enabled = controllable,
                    valueRange = -100f..100f,
                    value = speed,
                    onValueChange = { speed = it },
                    onValueChangeFinished = { onSpeedChanged(speed) },
                )
                CircularStopButton(enabled = controllable, onClick = {
                    speed = 0f
                    onSpeedChanged(speed)
                })
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
