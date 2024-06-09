package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.runtime.MutableState
import ru.tsar_ioann.legotrainscontrol.Train

data class UIData(
    val currentScreen: MutableState<Screen>,
    val trains: List<Train>,
    val onSpeedChanged: (Train, Float) -> Unit,
    val onLightsChanged: (Train.Locomotive, Float) -> Unit,
    val redWarningBoxText: MutableState<String?>,
    val onBluetoothNotEnabledBoxClick: () -> Unit,
) {
    enum class Screen {
        TRAINS_LIST,
    }
}
