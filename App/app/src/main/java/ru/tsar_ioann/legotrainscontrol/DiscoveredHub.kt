package ru.tsar_ioann.legotrainscontrol

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Represents a Pybricks hub discovered via BLE scanning but not yet assigned to a train.
 * Used in discovery mode when adding new trains.
 */
data class DiscoveredHub(
    val name: String,
    val macAddress: String,
    val isConnected: MutableState<Boolean> = mutableStateOf(false),
    val deviceAType: MutableState<DeviceType> = mutableStateOf(DeviceType.NONE),
    val deviceBType: MutableState<DeviceType> = mutableStateOf(DeviceType.NONE),
)
