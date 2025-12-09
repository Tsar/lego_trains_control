package ru.tsar_ioann.legotrainscontrol

import android.content.SharedPreferences
import java.util.UUID

/**
 * Persistent configuration for a locomotive (Pybricks hub).
 * Saved to SharedPreferences.
 */
data class LocomotiveConfig(
    val hubName: String,
    val invertDeviceA: Boolean = false,  // Invert motor direction on port A
    val invertDeviceB: Boolean = false,  // Invert motor direction on port B
) {
    fun saveToStorage(editor: SharedPreferences.Editor, prefix: String) {
        editor.putString("${prefix}hub_name", hubName)
        editor.putBoolean("${prefix}invert_a", invertDeviceA)
        editor.putBoolean("${prefix}invert_b", invertDeviceB)
    }

    companion object {
        fun fromStorage(prefs: SharedPreferences, prefix: String): LocomotiveConfig {
            return LocomotiveConfig(
                hubName = prefs.getString("${prefix}hub_name", "") ?: "",
                invertDeviceA = prefs.getBoolean("${prefix}invert_a", false),
                invertDeviceB = prefs.getBoolean("${prefix}invert_b", false),
            )
        }
    }
}

/**
 * Persistent configuration for a train (collection of locomotives).
 * Saved to SharedPreferences.
 */
data class TrainConfig(
    val id: String,  // UUID for unique identification
    val name: String,
    val locomotiveConfigs: List<LocomotiveConfig>,
) {
    fun saveToStorage(editor: SharedPreferences.Editor, index: Int) {
        val prefix = "train_${index}_"
        editor.putString("${prefix}id", id)
        editor.putString("${prefix}name", name)
        editor.putInt("${prefix}loco_count", locomotiveConfigs.size)
        locomotiveConfigs.forEachIndexed { locoIndex, loco ->
            loco.saveToStorage(editor, "${prefix}loco_${locoIndex}_")
        }
    }

    companion object {
        fun fromStorage(prefs: SharedPreferences, index: Int): TrainConfig {
            val prefix = "train_${index}_"
            val id = prefs.getString("${prefix}id", "") ?: ""
            val name = prefs.getString("${prefix}name", "") ?: ""
            val locoCount = prefs.getInt("${prefix}loco_count", 0)
            val locomotives = (0 until locoCount).map { locoIndex ->
                LocomotiveConfig.fromStorage(prefs, "${prefix}loco_${locoIndex}_")
            }
            return TrainConfig(id, name, locomotives)
        }

        fun create(name: String, locomotiveConfigs: List<LocomotiveConfig>): TrainConfig {
            return TrainConfig(
                id = UUID.randomUUID().toString(),
                name = name,
                locomotiveConfigs = locomotiveConfigs,
            )
        }
    }
}
