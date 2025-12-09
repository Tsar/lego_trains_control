package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.tsar_ioann.legotrainscontrol.DeviceType
import ru.tsar_ioann.legotrainscontrol.DiscoveredHub
import ru.tsar_ioann.legotrainscontrol.LocomotiveConfig

/**
 * Mutable state holder for locomotive config during editing.
 */
private data class EditableLocomotiveConfig(
    val hubName: String,
    var invertDeviceA: Boolean = false,
    var invertDeviceB: Boolean = false,
) {
    fun toLocomotiveConfig() = LocomotiveConfig(hubName, invertDeviceA, invertDeviceB)
}

@Composable
fun AddEditTrainScreen(
    isEditMode: Boolean,
    initialName: String = "",
    initialLocomotiveConfigs: List<LocomotiveConfig> = emptyList(),
    discoveredHubs: List<DiscoveredHub>,
    onSave: (name: String, locomotiveConfigs: List<LocomotiveConfig>) -> Unit,
    onDelete: (() -> Unit)? = null,
    onCancel: () -> Unit,
    onStartDiscovery: () -> Unit,
) {
    var trainName by remember { mutableStateOf(initialName) }
    // Map hubName -> EditableLocomotiveConfig for selected locomotives
    val selectedLocomotives = remember {
        mutableStateMapOf<String, EditableLocomotiveConfig>().also { map ->
            initialLocomotiveConfigs.forEach { config ->
                map[config.hubName] = EditableLocomotiveConfig(
                    hubName = config.hubName,
                    invertDeviceA = config.invertDeviceA,
                    invertDeviceB = config.invertDeviceB,
                )
            }
        }
    }

    // Start discovery when screen opens
    LaunchedEffect(Unit) {
        onStartDiscovery()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (isEditMode) "Edit Train" else "Add New Train",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = trainName,
            onValueChange = { trainName = it },
            label = { Text("Train Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select Locomotives:",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (discoveredHubs.isEmpty() && selectedLocomotives.isEmpty()) {
            Text(
                text = "Scanning for Pybricks hubs...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(discoveredHubs) { hub ->
                val isSelected = hub.name in selectedLocomotives
                val config = selectedLocomotives[hub.name]
                val deviceAType = hub.deviceAType.value
                val deviceBType = hub.deviceBType.value

                LocomotiveSelectionItem(
                    hubName = hub.name,
                    subtitle = hub.macAddress,
                    deviceAType = deviceAType,
                    deviceBType = deviceBType,
                    isSelected = isSelected,
                    invertDeviceA = config?.invertDeviceA ?: false,
                    invertDeviceB = config?.invertDeviceB ?: false,
                    onSelectedChange = { selected ->
                        if (selected) {
                            selectedLocomotives[hub.name] = EditableLocomotiveConfig(hub.name)
                        } else {
                            selectedLocomotives.remove(hub.name)
                        }
                    },
                    onInvertAChange = { invert ->
                        selectedLocomotives[hub.name]?.let {
                            selectedLocomotives[hub.name] = it.copy(invertDeviceA = invert)
                        }
                    },
                    onInvertBChange = { invert ->
                        selectedLocomotives[hub.name]?.let {
                            selectedLocomotives[hub.name] = it.copy(invertDeviceB = invert)
                        }
                    },
                )
            }

            // Show already selected hubs that aren't currently discovered
            val notDiscoveredSelected = selectedLocomotives.keys.filter { name ->
                discoveredHubs.none { it.name == name }
            }
            if (notDiscoveredSelected.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Previously selected (not found):",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
                items(notDiscoveredSelected) { hubName ->
                    val config = selectedLocomotives[hubName]

                    LocomotiveSelectionItem(
                        hubName = hubName,
                        subtitle = null,
                        deviceAType = DeviceType.NONE,  // Unknown for not-discovered hubs
                        deviceBType = DeviceType.NONE,
                        isSelected = true,
                        invertDeviceA = config?.invertDeviceA ?: false,
                        invertDeviceB = config?.invertDeviceB ?: false,
                        isGrayedOut = true,
                        onSelectedChange = { selected ->
                            if (!selected) {
                                selectedLocomotives.remove(hubName)
                            }
                        },
                        onInvertAChange = { invert ->
                            selectedLocomotives[hubName]?.let {
                                selectedLocomotives[hubName] = it.copy(invertDeviceA = invert)
                            }
                        },
                        onInvertBChange = { invert ->
                            selectedLocomotives[hubName]?.let {
                                selectedLocomotives[hubName] = it.copy(invertDeviceB = invert)
                            }
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            ) {
                Text("Cancel")
            }

            if (isEditMode && onDelete != null) {
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                ) {
                    Text("Delete")
                }
            }

            Button(
                onClick = {
                    val configs = selectedLocomotives.values.map { it.toLocomotiveConfig() }
                    onSave(trainName, configs)
                },
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                enabled = trainName.isNotBlank() && selectedLocomotives.isNotEmpty(),
            ) {
                Text("Save")
            }
        }
    }
}

private fun DeviceType.displayName(): String = when (this) {
    DeviceType.NONE -> "—"
    DeviceType.DC_MOTOR -> "DC Motor"
    DeviceType.MOTOR -> "Motor"
    DeviceType.LIGHT -> "Light"
}

@Composable
private fun LocomotiveSelectionItem(
    hubName: String,
    subtitle: String?,
    deviceAType: DeviceType,
    deviceBType: DeviceType,
    isSelected: Boolean,
    invertDeviceA: Boolean,
    invertDeviceB: Boolean,
    isGrayedOut: Boolean = false,
    onSelectedChange: (Boolean) -> Unit,
    onInvertAChange: (Boolean) -> Unit,
    onInvertBChange: (Boolean) -> Unit,
) {
    val textColor = if (isGrayedOut) Color.Gray else Color.Unspecified
    val hasAnyDevice = deviceAType != DeviceType.NONE || deviceBType != DeviceType.NONE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectedChange(!isSelected) }
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectedChange,
            )
            Column {
                Text(text = hubName, color = textColor)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
                // Show device types if detected
                if (hasAnyDevice) {
                    Text(
                        text = "A: ${deviceAType.displayName()}, B: ${deviceBType.displayName()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
            }
        }

        // Show invert options only when selected and only for motor devices
        if (isSelected && (deviceAType.isMotor || deviceBType.isMotor)) {
            Row(
                modifier = Modifier.padding(start = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (deviceAType.isMotor) {
                    Checkbox(
                        checked = invertDeviceA,
                        onCheckedChange = onInvertAChange,
                    )
                    Text(text = "Invert A", style = MaterialTheme.typography.bodySmall)
                }

                if (deviceAType.isMotor && deviceBType.isMotor) {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (deviceBType.isMotor) {
                    Checkbox(
                        checked = invertDeviceB,
                        onCheckedChange = onInvertBChange,
                    )
                    Text(text = "Invert B", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
