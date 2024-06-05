package ru.tsar_ioann.legotrainscontrol

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class Train(
    val name: String,
    val locomotives: List<Locomotive>,
) {
    data class Locomotive(
        val hubName: String,  // name which you gave to Pybricks Hub when installing Pybricks Firmware
        val hasLights: Boolean = false,
        val controllable: MutableState<Boolean> = mutableStateOf(false),
    )
}
