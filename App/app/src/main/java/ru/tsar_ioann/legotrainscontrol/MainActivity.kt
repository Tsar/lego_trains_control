package ru.tsar_ioann.legotrainscontrol

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
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
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import ru.tsar_ioann.legotrainscontrol.ui.Main
import ru.tsar_ioann.legotrainscontrol.ui.UIData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

@OptIn(ExperimentalStdlibApi::class)
class MainActivity : ComponentActivity() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        private const val PERMISSIONS_REQUEST_CODE = 1
        private const val REQUEST_ENABLE_BLUETOOTH = 1

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

    private val currentScreen = mutableStateOf(UIData.Screen.TRAINS_LIST)
    private val redWarning = mutableStateOf<UIData.Warning?>(null)
    private var bleScanner: BluetoothLeScanner? = null
    private var gattCallbacks = ConcurrentHashMap<String, GattCallback>()

    private val trains = listOf(
        Train(
            name = "Green Express",
            locomotives = listOf(
                Train.Locomotive(hubName = "Express_P1", hasLights = true),
                Train.Locomotive(hubName = "Express_P2", hasLights = true),
            ),
        ),
        Train(
            name = "Cargo Train",
            locomotives = listOf(Train.Locomotive(hubName = "Cargo_Train")),
        ),
        Train(
            name = "White & Yellow",
            locomotives = listOf(Train.Locomotive(hubName = "White & Yellow")),
        ),
        Train(
            name = "Orient Express",
            locomotives = listOf(Train.Locomotive(hubName = "Orient_Express")),
        ),
    )

    private val allLocomotives = trains.flatMap { train -> train.locomotives }.associateBy { it.hubName }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Main(
                UIData(
                    currentScreen = currentScreen,
                    trains = trains,
                    onSpeedChanged = { train, speed -> train.setSpeed(speed) },
                    onLightsChanged = { locomotive, lights -> locomotive.setLights(lights) },
                    redWarning = redWarning,
                    onRedWarningBoxClick = this::onRedWarningBoxClick,
                )
            )
        }

        discoverTrains()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDiscoveringTrains()
    }

    private fun discoverTrains() {
        if (REQUIRED_PERMISSIONS.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            redWarning.value = UIData.Warning.REQUIRED_PERMISSIONS_MISSING
            showPermissionsRequest()
            return
        }

        val bluetoothAdapter = getSystemService<BluetoothManager>()?.adapter
        if (bluetoothAdapter == null) {
            redWarning.value = UIData.Warning.BLUETOOTH_NOT_SUPPORTED
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            redWarning.value = UIData.Warning.BLUETOOTH_NOT_ENABLED
            showRequestToEnableBluetooth()
            return
        }

        val bleScanner = bluetoothAdapter.bluetoothLeScanner
        this.bleScanner = bleScanner
        if (bleScanner == null) {
            redWarning.value = UIData.Warning.BLE_SCANNER_UNAVAILABLE
            return
        }

        val scanFilters = allLocomotives.keys.map { locomotiveName ->
            ScanFilter.Builder()
                .setServiceUuid(PYBRICKS_SERVICE_UUID)
                .setDeviceName(locomotiveName)
                .build()
        }
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bleScanner.startScan(scanFilters, scanSettings, scanCallback)
    }

    private fun stopDiscoveringTrains() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bleScanner?.stopScan(scanCallback)
        }
    }

    private fun onRedWarningBoxClick() {
        when (redWarning.value) {
            UIData.Warning.REQUIRED_PERMISSIONS_MISSING -> showPermissionsRequest()
            UIData.Warning.BLUETOOTH_NOT_ENABLED -> showRequestToEnableBluetooth()
            UIData.Warning.BLUETOOTH_NOT_SUPPORTED,
            UIData.Warning.BLE_SCANNER_UNAVAILABLE,
            null -> {}
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
        if (deviceName != null && serviceUuids?.contains(PYBRICKS_SERVICE_UUID) == true && deviceName in allLocomotives) {
            val locomotive = allLocomotives.getValue(deviceName)
            val callback = GattCallback(
                locomotiveName = deviceName,
                onReadyForCommands = { locomotive.updateControllableState() },
                onStatusUpdate = { voltage, current, speed, brightness -> locomotive.handleStatusUpdate(voltage, current, speed, brightness) },
                onDisconnected = {
                    gattCallbacks.remove(deviceName)
                    locomotive.updateControllableState()
                }
            )
            if (gattCallbacks.putIfAbsent(deviceName, callback) == null) {
                Log.i("BLE_SCAN", "Found $deviceName, connecting")
                scanResult.device.connectGatt(this, false, callback)
            }
        }
    }

    private fun Train.Locomotive.updateControllableState() {
        runOnUiThread {
            controllable.value = isReadyForCommands()
        }
    }

    private fun Train.Locomotive.handleStatusUpdate(voltage: Int, current: Int, speed: Short, brightness: Short) {
        runOnUiThread {
            this.voltage.intValue = voltage
            this.current.intValue = current
        }
    }

    private fun Train.areAllLocomotivesReady() = locomotives.all { it.isReadyForCommands() }

    private fun Train.setSpeed(speed: Float) {
        if (areAllLocomotivesReady()) {
            sendToAllLocomotives(PROTO_SET_SPEED, speed.roundToInt().toShort())
        }
    }

    private fun Train.sendToAllLocomotives(protoCmd: Byte, payload: Short) {
        locomotives.forEach {  locomotive ->
            locomotive.sendToLocomotive(protoCmd, payload)
        }
    }

    private fun Train.Locomotive.isReadyForCommands(): Boolean = gattCallbacks[hubName]?.isReadyForCommands() == true

    private fun Train.Locomotive.setLights(lights: Float) = sendToLocomotive(PROTO_SET_LIGHT, lights.roundToInt().toShort())

    private fun Train.Locomotive.sendToLocomotive(protoCmd: Byte, payload: Short) {
        val byteArray = ByteBuffer.allocate(6)
            .order(ByteOrder.BIG_ENDIAN)
            .put(COMMAND_WRITE_STDIN)
            .putShort(PROTO_MAGIC)
            .put(protoCmd)
            .putShort(payload)
            .array()
        gattCallbacks[hubName]?.writeByteArray(byteArray)
    }

    private class GattCallback(
        private val locomotiveName: String,
        private val onReadyForCommands: () -> Unit,
        private val onStatusUpdate: (Int, Int, Short, Short) -> Unit,
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
                    Log.i("GATT_CALLBACK", "Connected to $locomotiveName, discovering services")
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i("GATT_CALLBACK", "$locomotiveName: Disconnected")
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
                        Log.i("GATT_CALLBACK", "$locomotiveName: PYBRICKS_COMMAND_EVENT characteristic discovered, notifications set")
                    } else {
                        Log.e("GATT_CALLBACK", "$locomotiveName: PYBRICKS_COMMAND_EVENT characteristic discovered, but it does not have descriptor CLIENT_CHARACTERISTIC_CONFIG")
                    }
                } else {
                    Log.e("GATT_CALLBACK", "$locomotiveName: PYBRICKS_COMMAND_EVENT characteristic not found")
                }
            } else {
                Log.e("GATT_CALLBACK", "$locomotiveName: Failed to discover BLE device services, status: $status")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            Log.i("GATT_CALLBACK", "$locomotiveName: onDescriptorWrite ${descriptor?.uuid}, status = $status")
        }

        @Deprecated("Deprecated, but must be used to support older Androids (API 31, for example)")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            //Log.i("GATT_CALLBACK", "$deviceName: onCharacteristicChanged ${characteristic.uuid}")
            if (characteristic.uuid != PYBRICKS_COMMAND_EVENT_UUID) {
                Log.w("GATT_CALLBACK", "$locomotiveName: Got unexpected notification about some other characteristic")
                return
            }

            val value = characteristic.value.clone() // let's copy asap for safety
            if (value.isEmpty()) {
                Log.w("GATT_CALLBACK", "$locomotiveName: Command characteristic unexpectedly sent empty value")
                return
            }
            //Log.i("GATT_CALLBACK", "$locomotiveName: Received command characteristic: ${value.toHexString(format = HexFormat.UpperCase)}")

            val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
            val event = buffer.get()
            val payloadSize = buffer.remaining()
            if (event == EVENT_STATUS_REPORT) {
                if (payloadSize != 4) {
                    Log.w("GATT_CALLBACK", "$locomotiveName: Unexpected size of EVENT_STATUS_REPORT: $payloadSize")
                    return
                }
                val statusFlags = buffer.getInt()
                if ((statusFlags and STATUS_FLAG_POWER_BUTTON_PRESSED) == 0 && (statusFlags and STATUS_FLAG_USER_PROGRAM_RUNNING) == 0) {
                    this.gatt = gatt
                    if (writeByteArray(byteArrayOf(COMMAND_START_USER_PROGRAM))) {
                        writtenStartUserProgram = true
                        Log.i("GATT_CALLBACK", "$locomotiveName: Sent command to start MicroPython program")
                    } else {
                        Log.e("GATT_CALLBACK", "$locomotiveName: Failed to send command to start MicroPython program")
                    }
                }
            } else if (event == EVENT_WRITE_STDOUT) {
                if (payloadSize != 15) {
                    Log.w("GATT_CALLBACK", "$locomotiveName: Unexpected size of EVENT_WRITE_STDOUT: $payloadSize")
                    return
                }
                buffer.order(ByteOrder.BIG_ENDIAN) // switching to network order
                val magic = buffer.getShort()
                if (magic != PROTO_MAGIC) {
                    Log.w("GATT_CALLBACK", "$locomotiveName: Got incorrect value for MAGIC")
                    return
                }
                val protoCmd = buffer.get()
                if (protoCmd != PROTO_STATUS) {
                    Log.w("GATT_CALLBACK", "$locomotiveName: Got something else than status report, unexpected")
                    return
                }
                val batteryVoltage = buffer.getInt()
                val batteryCurrent = buffer.getInt()
                val speed = buffer.getShort()
                val brightness = buffer.getShort()

                onStatusUpdate(batteryVoltage, batteryCurrent, speed, brightness)
                //Log.i("GATT_CALLBACK", "$locomotiveName: Battery voltage = $batteryVoltage, battery current = $batteryCurrent, speed = $speed, brightness = $brightness")
            } else {
                Log.w("GATT_CALLBACK", "$locomotiveName: Unexpected event came: 0x${value.toHexString(format = HexFormat.UpperCase)}")
            }
        }

        /*
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            Log.i("GATT_CALLBACK", "$deviceName: onCharacteristicChanged[NEW] ${characteristic.uuid}: ${value.toHexString(format = HexFormat.UpperCase)}")
            super.onCharacteristicChanged(gatt, characteristic, value) // this will call the deprecated one
        }
        */

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            //Log.i("GATT_CALLBACK", "$locomotiveName: onCharacteristicWrite ${characteristic?.uuid}, status = $status")

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
                //Log.i("GATT_CALLBACK", "$locomotiveName: Written byte array to command characteristic: ${byteArray.toHexString(format = HexFormat.UpperCase)}")
                return true
            }
            return false
        }
    }

    private fun showPermissionsRequest() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                redWarning.value = null
                discoverTrains()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showRequestToEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                redWarning.value = null
                discoverTrains()
            }
        }
    }
}
