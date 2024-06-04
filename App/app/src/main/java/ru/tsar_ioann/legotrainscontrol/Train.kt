package ru.tsar_ioann.legotrainscontrol

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class Train(
    val name: String,
    val controllable: MutableState<Boolean> = mutableStateOf(false),
    val hasLights: Boolean = false,
    val locomotives: List<String> = emptyList(),  // names which you gave to Pybricks Hubs when installing Pybricks Firmware
)
