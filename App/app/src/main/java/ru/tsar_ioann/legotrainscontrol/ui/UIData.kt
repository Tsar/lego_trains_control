package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.runtime.MutableState
import ru.tsar_ioann.legotrainscontrol.Train

data class UIData(
    val currentScreen: MutableState<Screen>,
    val trains: List<Train>,
    val onSpeedChanged: (Train, Float) -> Unit,
    val onLightsChanged: (Train.Locomotive, Float) -> Unit,
    val redWarning: MutableState<Warning?>,
    val onRedWarningBoxClick: () -> Unit,
) {
    enum class Screen {
        TRAINS_LIST,
    }

    enum class Warning(val text: String) {
        REQUIRED_PERMISSIONS_MISSING("Required permissions missing"),
        BLUETOOTH_NOT_SUPPORTED("Your device does not support bluetooth"),
        BLUETOOTH_NOT_ENABLED("Bluetooth is not enabled"),
        BLE_SCANNER_UNAVAILABLE("BLE scanner unavailable"),
    }
}
