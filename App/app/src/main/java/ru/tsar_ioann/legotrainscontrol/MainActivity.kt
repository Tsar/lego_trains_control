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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MainActivity : ComponentActivity() {

    companion object {
        private val PYBRICKS_SERVICE_UUID = ParcelUuid.fromString("c5f50001-8280-46da-89f4-6d8051e4aeef")
        private val PYBRICKS_COMMAND_EVENT_UUID = "c5f50002-8280-46da-89f4-6d8051e4aeef".asUUID()
        private val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb".asUUID()

        private const val COMMAND_START_USER_PROGRAM: Byte = 0x01
        private const val COMMAND_WRITE_STDIN: Byte = 0x06

        private const val GREEN_EXPRESS_P1_NAME = "Express_P1"
        private const val GREEN_EXPRESS_P2_NAME = "Express_P2"
        private const val CARGO_TRAIN_NAME = "Cargo_Train"
        private const val ORIENT_EXPRESS_NAME = "Orient_Express"

        private val namesOfAllTrains = setOf(
            GREEN_EXPRESS_P1_NAME,
            GREEN_EXPRESS_P2_NAME,
            CARGO_TRAIN_NAME,
            ORIENT_EXPRESS_NAME
        )

        private fun String.asUUID(): UUID = UUID.fromString(this)
    }

    private var bleScanner: BluetoothLeScanner? = null
    private var gattCallbacks = ConcurrentHashMap<String, GattCallback>()

    private val greenExpressControllable = mutableStateOf(false)
    private val cargoTrainControllable = mutableStateOf(false)
    private val orientExpressControllable = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AllTrains(
                greenExpressControllable = greenExpressControllable,
                cargoTrainControllable = cargoTrainControllable,
                orientExpressControllable = orientExpressControllable,
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
        }

        bleScanner = bluetoothAdapter.bluetoothLeScanner
        if (bleScanner == null) {
            Toast.makeText(this, "Can't scan BLE devices: bluetooth is not enabled!", Toast.LENGTH_LONG).show()
            return
        }

        val scanFilters = namesOfAllTrains.map { trainName ->
            ScanFilter.Builder()
                .setServiceUuid(PYBRICKS_SERVICE_UUID)
                .setDeviceName(trainName)
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
                bleDeviceFound(result)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach {
                bleDeviceFound(it)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Failed to scan BLE devices! Error code: $errorCode", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun bleDeviceFound(scanResult: ScanResult) {
        val serviceUuids = scanResult.scanRecord?.serviceUuids
        val deviceName = scanResult.scanRecord?.deviceName
        if (deviceName != null && serviceUuids?.contains(PYBRICKS_SERVICE_UUID) == true && namesOfAllTrains.contains(deviceName)) {
            val callback = GattCallback(
                deviceName = deviceName,
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

    private fun updateControllableStates() {
        runOnUiThread {
            greenExpressControllable.value = (gattCallbacks[GREEN_EXPRESS_P1_NAME]?.isReadyForCommands() == true && gattCallbacks[GREEN_EXPRESS_P2_NAME]?.isReadyForCommands() == true)
            cargoTrainControllable.value = (gattCallbacks[CARGO_TRAIN_NAME]?.isReadyForCommands() == true)
            orientExpressControllable.value = (gattCallbacks[ORIENT_EXPRESS_NAME]?.isReadyForCommands() == true)
        }
    }

    private class GattCallback(
        private val deviceName: String,
        private val onReadyForCommands: () -> Unit,
        private val onDisconnected: () -> Unit,
    ) : BluetoothGattCallback() {

        private var writtenStartUserProgram = false
        private var readyForCommands = false

        fun isReadyForCommands(): Boolean = readyForCommands

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("GATT_CALLBACK", "Connected to $deviceName, discovering services")
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i("GATT_CALLBACK", "$deviceName: Disconnected")
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
                        Log.i("GATT_CALLBACK", "$deviceName: PYBRICKS_COMMAND_EVENT characteristic discovered, notifications set")
                    } else {
                        Log.e("GATT_CALLBACK", "$deviceName: PYBRICKS_COMMAND_EVENT characteristic discovered, but it does not have descriptor CLIENT_CHARACTERISTIC_CONFIG")
                    }
                } else {
                    Log.e("GATT_CALLBACK", "$deviceName: PYBRICKS_COMMAND_EVENT characteristic not found")
                }
            } else {
                Log.e("GATT_CALLBACK", "$deviceName: Failed to discover BLE device services, status: $status")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            Log.i("GATT_CALLBACK", "$deviceName: onDescriptorWrite ${descriptor?.uuid}, status = $status")

            if (status == BluetoothGatt.GATT_SUCCESS && writeByteArray(gatt, byteArrayOf(COMMAND_START_USER_PROGRAM))) {
                writtenStartUserProgram = true
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            Log.i("GATT_CALLBACK", "$deviceName: onCharacteristicChanged ${characteristic.uuid}")
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            Log.i("GATT_CALLBACK", "$deviceName: onCharacteristicRead ${characteristic.uuid}")
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            Log.i("GATT_CALLBACK", "$deviceName: onCharacteristicWrite ${characteristic?.uuid}, status = $status")

            if (status == BluetoothGatt.GATT_SUCCESS && writtenStartUserProgram) {
                // MicroPython program is started
                writtenStartUserProgram = false
                readyForCommands = true
                onReadyForCommands()

                //writeByteArray(gatt, byteArrayOf(COMMAND_WRITE_STDIN, 0x58, 0xAB.toByte(), 0x00, 0x00, 0x00))
                //writeByteArray(gatt, byteArrayOf(COMMAND_WRITE_STDIN, 0x58, 0xAB.toByte(), 0x01, 0x00, 0x60))
            }
        }

        @SuppressLint("MissingPermission")
        private fun writeByteArray(gatt: BluetoothGatt?, byteArray: ByteArray): Boolean {
            val characteristic = gatt?.getService(PYBRICKS_SERVICE_UUID.uuid)?.getCharacteristic(PYBRICKS_COMMAND_EVENT_UUID)
            if (characteristic != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(characteristic, byteArray, WRITE_TYPE_DEFAULT)
                } else {
                    characteristic.setValue(byteArray)
                    gatt.writeCharacteristic(characteristic)
                }
                Log.i("GATT_CALLBACK", "$deviceName: Written byte array to characteristic: $byteArray")
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
fun Train(name: String, controllable: MutableState<Boolean>, hasLights: Boolean = false) {
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
                        onValueChangeFinished = {},
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
                    onValueChangeFinished = {},
                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                )
                CircularStopButton(enabled = controllable.value, onClick = { speed = 0f })
            }
        }
    }
}

//@Preview
@Composable
fun AllTrains(
    greenExpressControllable: MutableState<Boolean>,
    cargoTrainControllable: MutableState<Boolean>,
    orientExpressControllable: MutableState<Boolean>,
) {
    LegoTrainsControlTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {
                Train(name = "Green Express", controllable = greenExpressControllable, hasLights = true)
                Train(name = "Cargo Train", controllable = cargoTrainControllable)
                Train(name = "Orient Express", controllable = orientExpressControllable)
            }
        }
    }
}
