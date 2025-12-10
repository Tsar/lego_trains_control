# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LEGO Trains Control is a project for controlling multiple LEGO trains via Bluetooth Low Energy (BLE). It consists of:
- **Android App** (Kotlin, Jetpack Compose): Controls trains from a single UI
- **MicroPython Script**: Runs on Pybricks-flashed LEGO CityHub locomotives

## Build Commands

### Android App

```bash
# Build debug APK
cd App && ./gradlew assembleDebug

# Build release APK
cd App && ./gradlew assembleRelease

# Install on connected device
cd App && ./gradlew installDebug

# Run tests
cd App && ./gradlew test
```

## Architecture

### Android App (`App/`)

- **MainActivity.kt**: Core BLE logic - scanning for locomotives, GATT connections, protocol handling
  - Uses `BluetoothLeScanner` with scan filters for Pybricks service UUID
  - `GattCallback` inner class manages BLE connection state and characteristic read/write
  - Supports both normal mode (connect to known hubs) and discovery mode (find new hubs)

- **TrainsConfigManager.kt**: Persistent train configuration storage using SharedPreferences
  - Stores train configs as JSON
  - Notifies listeners on config changes to rebuild UI state

- **TrainConfig.kt**: Configuration data classes for serialization
  - `TrainConfig`: Train name + list of locomotive configs
  - `LocomotiveConfig`: Hub name for a locomotive

- **Train.kt**: Runtime data model for trains and locomotives with Compose state holders
  - `Train.fromConfig()`: Factory method to create trains from stored configs

- **DiscoveredHub.kt**: Data class for hubs found during BLE discovery

- **DeviceType.kt**: Enum for device types (None, DCMotor, Motor, Light)

- **ui/**: Jetpack Compose UI components
  - `Main.kt`: Root composable with screen navigation (TRAINS_LIST, TRAIN_CONTROLS, ADD_TRAIN)
  - `TrainsList.kt`: Train selection list with settings mode for reordering/deleting
  - `TrainControls.kt`: Control UI for a selected train
  - `AddTrainScreen.kt`: BLE discovery UI to add new trains
  - `LocomotiveControls.kt`: Per-locomotive controls (speed/brightness per port)
  - `UIData.kt`: UI state container

### Communication Protocol

Binary protocol over BLE (big-endian, network byte order):
- Magic: `0x58AB`
- Commands: `STATUS=0x05`, `SET_DEVICE_A=0x0A`, `SET_DEVICE_B=0x0B`
- Error responses: `BAD_COMMAND=0x03`, `BAD_DATA=0x04`

App sends to locomotive:
```
[1 byte COMMAND_WRITE_STDIN] [2 bytes MAGIC] [1 byte cmd] [2 bytes payload]
```

Locomotive responds via stdout:
```
[2 bytes MAGIC] [1 byte STATUS] [4 bytes voltage] [4 bytes current] [1 byte deviceA_type] [2 bytes deviceA_value] [1 byte deviceB_type] [2 bytes deviceB_value]
```

Device types: `NONE=0x00`, `DC_MOTOR=0x01`, `MOTOR=0x02`, `LIGHT=0x03`

### MicroPython Script (`MicroPython_Script/Universal_Train_Program.py`)

- Runs on LEGO CityHub with Pybricks firmware v3.4.0
- **Auto-detects** devices on Port A and Port B (DCMotor, Motor, or Light)
- `ControllableDevice` class wraps device with unified interface for any device type
- Reports status every 1000ms via stdout (includes device types and values per port)
- Receives commands via stdin

### Pybricks BLE Service

- Service UUID: `c5f50001-8280-46da-89f4-6d8051e4aeef`
- Command/Event Characteristic: `c5f50002-8280-46da-89f4-6d8051e4aeef`

## Adding a New Train

1. Flash locomotive CityHub with Pybricks firmware v3.4.0
2. Set the hub name in Pybricks (this identifies the locomotive)
3. Upload `Universal_Train_Program.py` to the hub
4. In the app: Use the "+" button to enter Add Train mode
5. The app discovers available hubs via BLE
6. Select hubs to form a train and give it a name
7. Train configuration is automatically saved to SharedPreferences

## Key Technical Notes

- Requires Pybricks firmware v3.4.0 specifically (protocol compatibility issues with newer versions)
- Android minSdk 31, targetSdk 34
- Uses Jetpack Compose with Material 3
- Train configurations stored in SharedPreferences (JSON format)
- MicroPython script is universal - works with any motor/light configuration
- Settings mode allows reordering and deleting trains from the UI
