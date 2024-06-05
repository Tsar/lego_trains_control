package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ru.tsar_ioann.legotrainscontrol.Train
import ru.tsar_ioann.legotrainscontrol.ui.theme.LegoTrainsControlTheme

@Composable
fun AllTrainControls(
    trains: List<Train>,
    onSpeedChanged: (Train, Float) -> Unit,
    onLightsChanged: (Train.Locomotive, Float) -> Unit,
) {
    LegoTrainsControlTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                trains.forEach { train ->
                    TrainControls(
                        train = train,
                        onSpeedChanged = { onSpeedChanged(train, it) },
                        onLightsChanged = onLightsChanged,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewAllTrainControls() {
    AllTrainControls(
        trains = listOf(
            Train(
                name = "Example Train 1",
                locomotives = listOf(
                    Train.Locomotive("Pybricks Hub 1", hasLights = true, controllable = remember { mutableStateOf(true) }),
                    Train.Locomotive("Pybricks Hub 2", controllable = remember { mutableStateOf(true) }),
                    Train.Locomotive("Pybricks Hub 3", hasLights = true, controllable = remember { mutableStateOf(true) }),
                ),
            ),
            Train(
                name = "Example Train 2",
                locomotives = listOf(Train.Locomotive("Pybricks Hub 4")),
            ),
            Train(
                name = "Example Train 3",
                locomotives = listOf(
                    Train.Locomotive("Pybricks Hub 5", hasLights = true, controllable = remember { mutableStateOf(true) }),
                    Train.Locomotive("Pybricks Hub 6", hasLights = true),
                ),
            ),
        ),
        onSpeedChanged = { _, _-> },
        onLightsChanged = { _, _-> },
    )
}
