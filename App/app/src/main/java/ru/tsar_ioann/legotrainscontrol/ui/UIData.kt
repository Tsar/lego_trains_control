package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.runtime.MutableState
import ru.tsar_ioann.legotrainscontrol.DiscoveredHub
import ru.tsar_ioann.legotrainscontrol.LocomotiveConfig
import ru.tsar_ioann.legotrainscontrol.Train

data class UIData(
    val currentScreen: MutableState<Screen>,
    val trains: List<Train>,
    val onSpeedChanged: (Train, Float) -> Unit,
    val onLightsChanged: (Train.Locomotive, Float) -> Unit,
    val redWarning: MutableState<Warning?>,
    val onRedWarningBoxClick: () -> Unit,
    // Train CRUD
    val discoveredHubs: List<DiscoveredHub> = emptyList(),
    val onAddTrain: (name: String, locomotiveConfigs: List<LocomotiveConfig>) -> Unit = { _, _ -> },
    val onDeleteTrain: (index: Int) -> Unit = { },
    val onStartDiscovery: () -> Unit = { },
    val onNavigateBack: () -> Unit = { },
) {
    enum class Screen {
        TRAINS_LIST,
        ADD_TRAIN,
    }

    enum class Warning(val text: String) {
        REQUIRED_PERMISSIONS_MISSING("Required permissions missing"),
        BLUETOOTH_NOT_SUPPORTED("Your device does not support bluetooth"),
        BLUETOOTH_NOT_ENABLED("Bluetooth is not enabled"),
        BLE_SCANNER_UNAVAILABLE("BLE scanner unavailable"),
    }
}
