package ru.tsar_ioann.legotrainscontrol

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class Train(
    val name: String,
    val controllable: MutableState<Boolean> = mutableStateOf(false),
    val hasLights: Boolean = false,
    val bleDevices: List<String> = emptyList(),
)
