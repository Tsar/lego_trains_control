package ru.tsar_ioann.legotrainscontrol

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/**
 * Runtime state for a train (used by UI).
 * TrainConfig is the persisted configuration, Train is the live state.
 */
data class Train(
    val config: TrainConfig,
    val locomotives: List<Locomotive>,
) {
    val name: String get() = config.name

    data class Locomotive(
        val hubName: String,
        val invertDeviceA: Boolean = false,
        val invertDeviceB: Boolean = false,
        // Runtime state (not persisted)
        val controllable: MutableState<Boolean> = mutableStateOf(false),
        val voltage: MutableIntState = mutableIntStateOf(0),
        val current: MutableIntState = mutableIntStateOf(0),
        // Device types auto-detected by MicroPython
        val deviceAType: MutableState<DeviceType> = mutableStateOf(DeviceType.NONE),
        val deviceBType: MutableState<DeviceType> = mutableStateOf(DeviceType.NONE),
        val deviceAValue: MutableIntState = mutableIntStateOf(0),
        val deviceBValue: MutableIntState = mutableIntStateOf(0),
        // Target value set by UI (used to restore on reconnect)
        val targetLightValue: MutableIntState = mutableIntStateOf(0),
    )

    companion object {
        /**
         * Create Train runtime state from TrainConfig.
         * Reuses existing locomotive objects if available to preserve BLE callback closures.
         */
        fun fromConfig(
            config: TrainConfig,
            existingLocomotives: Map<String, Locomotive> = emptyMap()
        ): Train {
            return Train(
                config = config,
                locomotives = config.locomotiveConfigs.map { locoConfig ->
                    // Reuse existing locomotive object to preserve callback closures
                    existingLocomotives[locoConfig.hubName] ?: Locomotive(
                        hubName = locoConfig.hubName,
                        invertDeviceA = locoConfig.invertDeviceA,
                        invertDeviceB = locoConfig.invertDeviceB,
                    )
                }
            )
        }
    }
}
