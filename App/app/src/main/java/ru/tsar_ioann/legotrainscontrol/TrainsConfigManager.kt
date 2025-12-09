package ru.tsar_ioann.legotrainscontrol

import android.content.SharedPreferences

/**
 * Manages train configurations with SharedPreferences persistence.
 * Pattern adapted from Smart Home's DevicesList.java.
 */
class TrainsConfigManager(private val prefs: SharedPreferences) {

    private val trainConfigs = mutableListOf<TrainConfig>()
    private var listener: Listener? = null

    fun interface Listener {
        fun onTrainsChanged()
    }

    init {
        loadFromStorage()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun getTrains(): List<TrainConfig> = trainConfigs.toList()

    fun getTrainCount(): Int = trainConfigs.size

    fun getTrain(index: Int): TrainConfig? = trainConfigs.getOrNull(index)

    fun findTrainByLocomotiveHubName(hubName: String): TrainConfig? {
        return trainConfigs.find { train ->
            train.locomotiveConfigs.any { it.hubName == hubName }
        }
    }

    fun addTrain(config: TrainConfig) {
        trainConfigs.add(config)
        saveAllToStorage()
        listener?.onTrainsChanged()
    }

    fun updateTrain(index: Int, config: TrainConfig) {
        if (index in trainConfigs.indices) {
            trainConfigs[index] = config
            saveAllToStorage()
            listener?.onTrainsChanged()
        }
    }

    fun removeTrain(index: Int) {
        if (index in trainConfigs.indices) {
            trainConfigs.removeAt(index)
            saveAllToStorage()
            listener?.onTrainsChanged()
        }
    }

    fun swapTrains(index1: Int, index2: Int) {
        if (index1 in trainConfigs.indices && index2 in trainConfigs.indices) {
            val temp = trainConfigs[index1]
            trainConfigs[index1] = trainConfigs[index2]
            trainConfigs[index2] = temp
            saveAllToStorage()
            listener?.onTrainsChanged()
        }
    }

    fun getAllConfiguredHubNames(): Set<String> {
        return trainConfigs
            .flatMap { it.locomotiveConfigs }
            .map { it.hubName }
            .toSet()
    }

    fun getLocomotiveConfig(hubName: String): LocomotiveConfig? {
        for (train in trainConfigs) {
            for (loco in train.locomotiveConfigs) {
                if (loco.hubName == hubName) {
                    return loco
                }
            }
        }
        return null
    }

    private fun loadFromStorage() {
        val count = prefs.getInt(KEY_TRAIN_COUNT, 0)
        trainConfigs.clear()
        for (i in 0 until count) {
            trainConfigs.add(TrainConfig.fromStorage(prefs, i))
        }
    }

    private fun saveAllToStorage() {
        val editor = prefs.edit()
        // Clear old data first
        editor.clear()
        // Save new data
        editor.putInt(KEY_TRAIN_COUNT, trainConfigs.size)
        trainConfigs.forEachIndexed { index, config ->
            config.saveToStorage(editor, index)
        }
        editor.apply()
    }

    companion object {
        private const val KEY_TRAIN_COUNT = "train_count"
    }
}
