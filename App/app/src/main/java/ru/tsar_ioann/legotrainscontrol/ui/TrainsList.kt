package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable

@Composable
fun TrainsList(uiData: UIData) {
    Column {
        val warning = uiData.redWarning.value
        if (warning != null) {
            RedWarningBox(text = warning.text, onClick = uiData.onRedWarningBoxClick)
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
