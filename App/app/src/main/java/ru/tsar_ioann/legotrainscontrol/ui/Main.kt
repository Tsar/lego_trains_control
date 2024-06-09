package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ru.tsar_ioann.legotrainscontrol.Train
import ru.tsar_ioann.legotrainscontrol.ui.theme.LegoTrainsControlTheme

@Composable
fun Main(uiData: UIData) {
    LegoTrainsControlTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (uiData.activeScreen) {
                    UIData.Screen.TRAINS_LIST -> TrainsList(uiData = uiData)
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewMain() {
    Main(
        UIData(
            activeScreen = UIData.Screen.TRAINS_LIST,
            trains = listOf(
                Train(
                    name = "Example Train 1",
                    locomotives = listOf(
                        Train.Locomotive(
                            hubName = "Example Locomotive 1",
                            hasLights = true,
                            controllable = remember { mutableStateOf(true) },
                            voltage = remember { mutableIntStateOf(7575) },
                            current = remember { mutableIntStateOf(141) },
                        ),
                        Train.Locomotive(
                            hubName = "Example Locomotive 2",
                            controllable = remember { mutableStateOf(true) },
                            voltage = remember { mutableIntStateOf(6789) },
                            current = remember { mutableIntStateOf(54) },
                        ),
                        Train.Locomotive(
                            hubName = "Example Locomotive 3",
                            hasLights = true,
                            controllable = remember { mutableStateOf(true) },
                            voltage = remember { mutableIntStateOf(8100) },
                            current = remember { mutableIntStateOf(115) },
                        ),
                    ),
                ),
                Train(
                    name = "Example Train 2",
                    locomotives = listOf(Train.Locomotive(hubName = "Example Locomotive 4")),
                ),
                Train(
                    name = "Example Train 3",
                    locomotives = listOf(
                        Train.Locomotive(
                            hubName = "Example Locomotive 5",
                            hasLights = true,
                            controllable = remember { mutableStateOf(true) },
                            voltage = remember { mutableIntStateOf(7854) },
                            current = remember { mutableIntStateOf(250) },
                        ),
                        Train.Locomotive(hubName = "Example Locomotive 6", hasLights = true),
                    ),
                ),
            ),
            onSpeedChanged = { _, _ -> },
            onLightsChanged = { _, _ -> },
            redWarningBoxText = remember { mutableStateOf("Bluetooth is not enabled") },
            onBluetoothNotEnabledBoxClick = {},
        )
    )
}
