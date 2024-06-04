package ru.tsar_ioann.legotrainscontrol

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import ru.tsar_ioann.legotrainscontrol.ui.theme.LegoTrainsControlTheme
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

@OptIn(ExperimentalStdlibApi::class)
class MainActivity : ComponentActivity() {

    companion object {
        private val PYBRICKS_SERVICE_UUID = ParcelUuid.fromString("c5f50001-8280-46da-89f4-6d8051e4aeef")
        private val PYBRICKS_COMMAND_EVENT_UUID = "c5f50002-8280-46da-89f4-6d8051e4aeef".asUUID()
        private val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb".asUUID()

        private const val COMMAND_START_USER_PROGRAM: Byte = 0x01
        private const val COMMAND_WRITE_STDIN: Byte = 0x06
        private const val EVENT_STATUS_REPORT: Byte = 0x00
        private const val EVENT_WRITE_STDOUT: Byte = 0x01
        private const val STATUS_FLAG_POWER_BUTTON_PRESSED: Int = 0x20
        private const val STATUS_FLAG_USER_PROGRAM_RUNNING: Int = 0x40

        private const val PROTO_MAGIC: Short = 0x58AB
        private const val PROTO_STATUS: Byte = 0x00
        private const val PROTO_SET_SPEED: Byte = 0x01
        private const val PROTO_SET_LIGHT: Byte = 0x02

        private fun String.asUUID(): UUID = UUID.fromString(this)
    }

    private var bleScanner: BluetoothLeScanner? = null
    private var gattCallbacks = ConcurrentHashMap<String, GattCallback>()

    private val trains = listOf(
        Train(
            name = "Green Express",
            hasLights = true,
            locomotives = listOf("Express_P1", "Express_P2"), // names which you gave to Pybricks Hubs when installing Pybricks Firmware
        ),
        Train(
            name = "Cargo Train",
            locomotives = listOf("Cargo_Train"),
        ),
        Train(
            name = "Orient Express",
            locomotives = listOf("Orient_Express"),
        ),
    )

    private val allLocomotives = trains.flatMap { it.locomotives }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AllTrainControls(
                trains = trains,
                onSpeedChanged = { train, speed -> train.setSpeed(speed) },
                onLightsChanged = { train, lights -> train.setLights(lights) },
            )
        }

        discoverTrains()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDiscoveringTrains()
    }

    private fun discoverTrains() {
        val requiredPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (requiredPermissions.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, requiredPermissions, 1)
            return
        }

        val bluetoothAdapter = getSystemService<BluetoothManager>()?.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Your device does not support bluetooth!", Toast.LENGTH_LONG).show()
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            bluetoothAdapter.enable()
            Thread.sleep(1500)
        }

        bleScanner = bluetoothAdapter.bluetoothLeScanner
        if (bleScanner == null) {
            Toast.makeText(this, "Can't scan for locomotives: bluetooth is not enabled!", Toast.LENGTH_LONG).show()
            return
        }

        val scanFilters = allLocomotives.map { locomotive ->
            ScanFilter.Builder()
                .setServiceUuid(PYBRICKS_SERVICE_UUID)
                .setDeviceName(locomotive)
                .build()
        }
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bleScanner?.startScan(scanFilters, scanSettings, scanCallback)
    }

    private fun stopDiscoveringTrains() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bleScanner?.stopScan(scanCallback)
        }
    }

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null && (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES || callbackType == ScanSettings.CALLBACK_TYPE_FIRST_MATCH)) {
                locomotiveFound(result)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach {
                locomotiveFound(it)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Failed to scan for locomotives! Error code: $errorCode", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun locomotiveFound(scanResult: ScanResult) {
        val serviceUuids = scanResult.scanRecord?.serviceUuids
        val deviceName = scanResult.scanRecord?.deviceName
        if (deviceName != null && serviceUuids?.contains(PYBRICKS_SERVICE_UUID) == true && allLocomotives.contains(deviceName)) {
            val callback = GattCallback(
                locomotive = deviceName,
                onReadyForCommands = { updateControllableStates() },
                onDisconnected = {
                    gattCallbacks.remove(deviceName)
                    updateControllableStates()
                }
            )
            if (gattCallbacks.putIfAbsent(deviceName, callback) == null) {
                Log.i("BLE_SCAN", "Found $deviceName, connecting")
                scanResult.device.connectGatt(this, false, callback)
            }
        }
    }

    private fun Train.areAllLocomotivesReady() = locomotives.all { locomotive -> gattCallbacks[locomotive]?.isReadyForCommands() == true }

    private fun updateControllableStates() {
        runOnUiThread {
            trains.forEach { train ->
                train.controllable.value = train.areAllLocomotivesReady()
            }
        }
    }

    private fun Train.setSpeed(speed: Float) = sendToLocomotives(PROTO_SET_SPEED, speed.roundToInt().toShort())

    private fun Train.setLights(lights: Float) = sendToLocomotives(PROTO_SET_LIGHT, lights.roundToInt().toShort())

    private fun Train.sendToLocomotives(protoCmd: Byte, payload: Short) {
        if (areAllLocomotivesReady()) {
            val byteArray = ByteBuffer.allocate(6)
                .order(ByteOrder.BIG_ENDIAN)
                .put(COMMAND_WRITE_STDIN)
                .putShort(PROTO_MAGIC)
                .put(protoCmd)
                .putShort(payload)
                .array()
            locomotives.forEach { locomotive ->
                gattCallbacks[locomotive]?.writeByteArray(byteArray)
            }
        }
    }

    private class GattCallback(
        private val locomotive: String,
        private val onReadyForCommands: () -> Unit,
        private val onDisconnected: () -> Unit,
    ) : BluetoothGattCallback() {

        private var gatt: BluetoothGatt? = null
        private var writtenStartUserProgram = false
        private var readyForCommands = false

        fun isReadyForCommands(): Boolean = readyForCommands

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            this.gatt = gatt
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("GATT_CALLBACK", "Connected to $locomotive, discovering services")
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i("GATT_CALLBACK", "$locomotive: Disconnected")
                    onDisconnected()
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val characteristic = gatt?.getService(PYBRICKS_SERVICE_UUID.uuid)?.getCharacteristic(PYBRICKS_COMMAND_EVENT_UUID)
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)

                    val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                    if (descriptor != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        } else {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            gatt.writeDescriptor(descriptor)
                        }
                        Log.i("GATT_CALLBACK", "$locomotive: PYBRICKS_COMMAND_EVENT characteristic discovered, notifications set")
                    } else {
                        Log.e("GATT_CALLBACK", "$locomotive: PYBRICKS_COMMAND_EVENT characteristic discovered, but it does not have descriptor CLIENT_CHARACTERISTIC_CONFIG")
                    }
                } else {
                    Log.e("GATT_CALLBACK", "$locomotive: PYBRICKS_COMMAND_EVENT characteristic not found")
                }
            } else {
                Log.e("GATT_CALLBACK", "$locomotive: Failed to discover BLE device services, status: $status")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            Log.i("GATT_CALLBACK", "$locomotive: onDescriptorWrite ${descriptor?.uuid}, status = $status")
        }

        @Deprecated("Deprecated, but must be used to support older Androids (API 31, for example)")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            //Log.i("GATT_CALLBACK", "$deviceName: onCharacteristicChanged ${characteristic.uuid}")
            if (characteristic.uuid != PYBRICKS_COMMAND_EVENT_UUID) {
                Log.w("GATT_CALLBACK", "$locomotive: Got unexpected notification about some other characteristic")
                return
            }

            val value = characteristic.value.clone() // let's copy asap for safety
            if (value.isEmpty()) {
                Log.w("GATT_CALLBACK", "$locomotive: Command characteristic unexpectedly sent empty value")
                return
            }
            Log.i("GATT_CALLBACK", "$locomotive: Received command characteristic: ${value.toHexString(format = HexFormat.UpperCase)}")

            val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
            val event = buffer.get()
            val payloadSize = buffer.remaining()
            if (event == EVENT_STATUS_REPORT) {
                if (payloadSize != 4) {
                    Log.w("GATT_CALLBACK", "$locomotive: Unexpected size of EVENT_STATUS_REPORT: $payloadSize")
                    return
                }
                val statusFlags = buffer.getInt()
                if ((statusFlags and STATUS_FLAG_POWER_BUTTON_PRESSED) == 0 && (statusFlags and STATUS_FLAG_USER_PROGRAM_RUNNING) == 0) {
                    this.gatt = gatt
                    if (writeByteArray(byteArrayOf(COMMAND_START_USER_PROGRAM))) {
                        writtenStartUserProgram = true
                        Log.i("GATT_CALLBACK", "$locomotive: Sent command to start MicroPython program")
                    } else {
                        Log.e("GATT_CALLBACK", "$locomotive: Failed to send command to start MicroPython program")
                    }
                }
            } else if (event == EVENT_WRITE_STDOUT) {
                if (payloadSize != 15) {
                    Log.w("GATT_CALLBACK", "$locomotive: Unexpected size of EVENT_WRITE_STDOUT: $payloadSize")
                    return
                }
                buffer.order(ByteOrder.BIG_ENDIAN) // switching to network order
                val magic = buffer.getShort()
                if (magic != PROTO_MAGIC) {
                    Log.w("GATT_CALLBACK", "$locomotive: Got incorrect value for MAGIC")
                    return
                }
                val protoCmd = buffer.get()
                if (protoCmd != PROTO_STATUS) {
                    Log.w("GATT_CALLBACK", "$locomotive: Got something else than status report, unexpected")
                    return
                }
                val batteryVoltage = buffer.getInt()
                val batteryCurrent = buffer.getInt()
                val speed = buffer.getShort()
                val brightness = buffer.getShort()
                Log.i("GATT_CALLBACK", "$locomotive: Battery voltage = $batteryVoltage, battery current = $batteryCurrent, speed = $speed, brightness = $brightness")
            } else {
                Log.w("GATT_CALLBACK", "$locomotive: Unexpected event came: 0x${value.toHexString(format = HexFormat.UpperCase)}")
            }
        }

        /*
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            Log.i("GATT_CALLBACK", "$deviceName: onCharacteristicChanged[NEW] ${characteristic.uuid}: ${value.toHexString(format = HexFormat.UpperCase)}")
            super.onCharacteristicChanged(gatt, characteristic, value) // this will call the deprecated one
        }
        */

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            Log.i("GATT_CALLBACK", "$locomotive: onCharacteristicWrite ${characteristic?.uuid}, status = $status")

            if (status == BluetoothGatt.GATT_SUCCESS && writtenStartUserProgram) {
                // MicroPython program is started
                writtenStartUserProgram = false
                readyForCommands = true
                onReadyForCommands()
            }
        }

        @SuppressLint("MissingPermission")
        fun writeByteArray(byteArray: ByteArray): Boolean {
            val characteristic = gatt?.getService(PYBRICKS_SERVICE_UUID.uuid)?.getCharacteristic(PYBRICKS_COMMAND_EVENT_UUID)
            if (characteristic != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt?.writeCharacteristic(characteristic, byteArray, WRITE_TYPE_DEFAULT)
                } else {
                    characteristic.setValue(byteArray)
                    gatt?.writeCharacteristic(characteristic)
                }
                Log.i("GATT_CALLBACK", "$locomotive: Written byte array to command characteristic: ${byteArray.toHexString(format = HexFormat.UpperCase)}")
                return true
            }
            return false
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            // If request is cancelled, the result arrays are empty
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                discoverTrains()
            } else {
                Toast.makeText(this, "You declined permission request, app can't scan BLE!", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun StatusCircle(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        drawCircle(style = Fill, color = color, radius = size.minDimension / 2)
        drawCircle(style = Stroke(width = 3f), color = Color.White, radius = size.minDimension / 2)
    }
}

@Composable
fun StopIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val size = size.minDimension
        drawRect(
            color = Color.Red,
            size = Size(size, size)
        )
    }
}

@Composable
fun CircularStopButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        enabled = enabled,
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(40.dp)
    ) {
        StopIcon()
    }
}

@Composable
fun TrainControls(name: String, controllable: MutableState<Boolean>, hasLights: Boolean = false, onSpeedChanged: (Float) -> Unit, onLightsChanged: (Float) -> Unit) {
    var speed by remember { mutableFloatStateOf(0f) }
    var lights by remember { mutableFloatStateOf(0f) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(15.dp)) {
        StatusCircle(color = if (controllable.value) Color.Green else Color.Red)
        Column(modifier = Modifier.padding(start = 15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, fontSize = 20.sp)
                if (hasLights) {
                    Spacer(modifier = Modifier.weight(1f))
                    Slider(
                        enabled = controllable.value,
                        valueRange = 0f..100f,
                        value = lights,
                        onValueChange = { lights = it },
                        onValueChangeFinished = { onLightsChanged(lights) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    enabled = controllable.value,
                    valueRange = -100f..100f,
                    value = speed,
                    onValueChange = { speed = it },
                    onValueChangeFinished = { onSpeedChanged(speed) },
                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                )
                CircularStopButton(enabled = controllable.value, onClick = {
                    speed = 0f
                    onSpeedChanged(speed)
                })
            }
        }
    }
}

@Preview
@Composable
fun AllTrainControls(
    trains: List<Train> = listOf(
        Train(name = "Example Train 1", controllable = mutableStateOf(true), hasLights = true),
        Train(name = "Example Train 2"),
        Train(name = "Example Train 3", controllable = mutableStateOf(true)),
    ),
    onSpeedChanged: (Train, Float) -> Unit = { _, _ -> },
    onLightsChanged: (Train, Float) -> Unit = { _, _ -> },
) {
    LegoTrainsControlTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {
                trains.forEach { info ->
                    TrainControls(
                        name = info.name,
                        controllable = info.controllable,
                        hasLights = info.hasLights,
                        onSpeedChanged = { onSpeedChanged(info, it) },
                        onLightsChanged = { onLightsChanged(info, it) },
                    )
                }
            }
        }
    }
}
