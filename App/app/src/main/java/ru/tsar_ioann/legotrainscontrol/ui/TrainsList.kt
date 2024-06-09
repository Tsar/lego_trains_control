package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable

@Composable
fun TrainsList(uiData: UIData) {
    Column {
        val warningText = uiData.redWarningBoxText.value
        if (warningText != null) {
            RedWarningBox(text = warningText, onClick = uiData.onBluetoothNotEnabledBoxClick)
        }
        LazyColumn {
            items(uiData.trains) { train ->
                TrainControls(
                    train = train,
                    onSpeedChanged = { uiData.onSpeedChanged(train, it) },
                    onLightsChanged = uiData.onLightsChanged,
                )
                Divider()
            }
        }
    }
}
