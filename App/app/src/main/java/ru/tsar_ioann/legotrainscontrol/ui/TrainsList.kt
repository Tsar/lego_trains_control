package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrainsList(uiData: UIData) {
    Column {
        val warning = uiData.redWarning.value
        if (warning != null) {
            RedWarningBox(text = warning.text, onClick = uiData.onRedWarningBoxClick)
        }
        if (uiData.trains.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No trains configured.\nTap + to add a train.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn {
                itemsIndexed(uiData.trains, key = { _, train -> train.config.id }) { index, train ->
                    TrainControls(
                        train = train,
                        index = index,
                        trainCount = uiData.trains.size,
                        settingsMode = uiData.settingsMode.value,
                        onSpeedChanged = { uiData.onSpeedChanged(train, it) },
                        onLightsChanged = uiData.onLightsChanged,
                        onDeleteClick = { uiData.onDeleteTrain(index) },
                        onMoveUp = { uiData.onMoveTrain(index, index - 1) },
                        onMoveDown = { uiData.onMoveTrain(index, index + 1) },
                    )
                    Divider()
                }
            }
        }
    }
}
