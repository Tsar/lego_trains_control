package ru.tsar_ioann.legotrainscontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tsar_ioann.legotrainscontrol.ui.theme.LegoTrainsControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AllTrains()
        }
    }
}

@Composable
fun StatusCircle(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        drawCircle(style = Fill, color = color, radius = size.minDimension / 2)
        drawCircle(style = Stroke(width = 3f), color = Color.White, radius = size.minDimension / 2)
    }
}

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

@Composable
fun Train(name: String, controllable: Boolean = false, hasLights: Boolean = false) {
    var speed by remember { mutableFloatStateOf(0f) }
    var lights by remember { mutableFloatStateOf(0f) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(15.dp)) {
        StatusCircle(color = if (controllable) Color.Green else Color.Red)
        Column(modifier = Modifier.padding(start = 15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, fontSize = 20.sp)
                if (hasLights) {
                    Spacer(modifier = Modifier.weight(1f))
                    Slider(
                        enabled = controllable,
                        valueRange = 0f..100f,
                        value = lights,
                        onValueChange = { lights = it },
                        onValueChangeFinished = {},
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    enabled = controllable,
                    valueRange = -100f..100f,
                    value = speed,
                    onValueChange = { speed = it },
                    onValueChangeFinished = {},
                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                )
                CircularStopButton(enabled = controllable, onClick = { speed = 0f })
            }
        }
    }
}

@Preview
@Composable
fun AllTrains() {
    LegoTrainsControlTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {
                Train(name = "Green Express", hasLights = true, controllable = true)
                Train(name = "Cargo Train")
                Train(name = "Orient Express")
            }
        }
    }
}
