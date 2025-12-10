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
import ru.tsar_ioann.legotrainscontrol.DeviceType
import ru.tsar_ioann.legotrainscontrol.LocomotiveConfig
import ru.tsar_ioann.legotrainscontrol.Train
import ru.tsar_ioann.legotrainscontrol.TrainConfig
import ru.tsar_ioann.legotrainscontrol.ui.theme.LegoTrainsControlTheme

@Composable
fun Main(uiData: UIData) {
    LegoTrainsControlTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (uiData.currentScreen.value) {
                    UIData.Screen.TRAINS_LIST -> TrainsList(uiData = uiData)
                    UIData.Screen.ADD_TRAIN -> AddEditTrainScreen(
                        isEditMode = false,
                        discoveredHubs = uiData.discoveredHubs,
                        onSave = { name, configs -> uiData.onAddTrain(name, configs) },
                        onCancel = uiData.onNavigateBack,
                        onStartDiscovery = uiData.onStartDiscovery,
                    )
                    UIData.Screen.EDIT_TRAIN -> {
                        val trainIndex = uiData.editingTrainIndex.value
                        if (trainIndex != null && trainIndex in uiData.trains.indices) {
                            val train = uiData.trains[trainIndex]
                            AddEditTrainScreen(
                                isEditMode = true,
                                initialName = train.name,
                                initialLocomotiveConfigs = train.config.locomotiveConfigs,
                                discoveredHubs = uiData.discoveredHubs,
                                onSave = { name, configs -> uiData.onUpdateTrain(trainIndex, name, configs) },
                                onDelete = { uiData.onDeleteTrain(trainIndex) },
                                onCancel = uiData.onNavigateBack,
                                onStartDiscovery = uiData.onStartDiscovery,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewMain() {
    Main(
        UIData(
            currentScreen = remember { mutableStateOf(UIData.Screen.TRAINS_LIST) },
            trains = listOf(
                Train(
                    config = TrainConfig("1", "Example Train 1", listOf(
                        LocomotiveConfig("Example Locomotive 1"),
                        LocomotiveConfig("Example Locomotive 2"),
                        LocomotiveConfig("Example Locomotive 3"),
                    )),
                    locomotives = listOf(
                        Train.Locomotive(
                            hubName = "Example Locomotive 1",
                            controllable = remember { mutableStateOf(true) },
                            voltage = remember { mutableIntStateOf(7575) },
                            current = remember { mutableIntStateOf(141) },
                            deviceBType = remember { mutableStateOf(DeviceType.LIGHT) },
                        ),
                        Train.Locomotive(
                            hubName = "Example Locomotive 2",
                            controllable = remember { mutableStateOf(true) },
                            voltage = remember { mutableIntStateOf(6789) },
                            current = remember { mutableIntStateOf(54) },
                        ),
                        Train.Locomotive(
                            hubName = "Example Locomotive 3",
                            controllable = remember { mutableStateOf(true) },
                            voltage = remember { mutableIntStateOf(8100) },
                            current = remember { mutableIntStateOf(115) },
                            deviceBType = remember { mutableStateOf(DeviceType.LIGHT) },
                        ),
                    ),
                ),
                Train(
                    config = TrainConfig("2", "Example Train 2", listOf(
                        LocomotiveConfig("Example Locomotive 4"),
                    )),
                    locomotives = listOf(Train.Locomotive(hubName = "Example Locomotive 4")),
                ),
                Train(
                    config = TrainConfig("3", "Example Train 3", listOf(
                        LocomotiveConfig("Example Locomotive 5"),
                        LocomotiveConfig("Example Locomotive 6"),
                    )),
                    locomotives = listOf(
                        Train.Locomotive(
                            hubName = "Example Locomotive 5",
                            controllable = remember { mutableStateOf(true) },
                            voltage = remember { mutableIntStateOf(7854) },
                            current = remember { mutableIntStateOf(250) },
                            deviceBType = remember { mutableStateOf(DeviceType.LIGHT) },
                        ),
                        Train.Locomotive(
                            hubName = "Example Locomotive 6",
                            deviceBType = remember { mutableStateOf(DeviceType.LIGHT) },
                        ),
                    ),
                ),
            ),
            onSpeedChanged = { _, _ -> },
            onLightsChanged = { _, _ -> },
            redWarning = remember { mutableStateOf(UIData.Warning.BLUETOOTH_NOT_ENABLED) },
            onRedWarningBoxClick = {},
            editingTrainIndex = remember { mutableStateOf(null) },
        )
    )
}
