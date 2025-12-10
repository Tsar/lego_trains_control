package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tsar_ioann.legotrainscontrol.DeviceType
import ru.tsar_ioann.legotrainscontrol.Train

@Composable
fun LocomotiveControls(locomotive: Train.Locomotive, onLightsChanged: (Float) -> Unit) {
    val hasLight = locomotive.deviceAType.value == DeviceType.LIGHT ||
                   locomotive.deviceBType.value == DeviceType.LIGHT

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (hasLight) {
            Slider(
                enabled = locomotive.controllable.value,
                valueRange = 0f..100f,
                value = locomotive.targetLightValue.intValue.toFloat(),
                onValueChange = { locomotive.targetLightValue.intValue = it.toInt() },
                onValueChangeFinished = { onLightsChanged(locomotive.targetLightValue.intValue.toFloat()) },
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
            voltage = mutableIntStateOf(7777),
            current = mutableIntStateOf(120),
            deviceBType = androidx.compose.runtime.mutableStateOf(DeviceType.LIGHT),
        ),
        Train.Locomotive(
            hubName = "Pybricks Hub Y",
            voltage = mutableIntStateOf(6878),
            current = mutableIntStateOf(522),
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewLocomotiveControls(@PreviewParameter(LocomotivePreviewParameterProvider::class) locomotive: Train.Locomotive) {
    LocomotiveControls(locomotive = locomotive) {}
}
