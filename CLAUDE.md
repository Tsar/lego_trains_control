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
  - Hardcoded train configurations at lines 75-99 (Train name → Locomotive hub names mapping)
  - Uses `BluetoothLeScanner` with scan filters for Pybricks service UUID
  - `GattCallback` inner class manages BLE connection state and characteristic read/write

- **Train.kt**: Data model for trains and locomotives with Compose state holders

- **ui/**: Jetpack Compose UI components
  - `Main.kt`: Root composable with screen navigation
  - `TrainsList.kt`, `TrainControls.kt`: Train selection and control UI
  - `SpeedSlider.kt`, `LocomotiveControls.kt`: Individual control widgets
  - `UIData.kt`: UI state container

### Communication Protocol

Binary protocol over BLE (big-endian, network byte order):
- Magic: `0x58AB`
- Commands: `STATUS=0x00`, `SET_SPEED=0x01`, `SET_LIGHT=0x02`

App sends to locomotive:
```
[1 byte COMMAND_WRITE_STDIN] [2 bytes MAGIC] [1 byte cmd] [2 bytes payload]
```

Locomotive responds via stdout:
```
[2 bytes MAGIC] [1 byte STATUS] [4 bytes voltage] [4 bytes current] [2 bytes speed] [2 bytes brightness]
```

### MicroPython Script (`MicroPython_Script/Universal_Train_Program.py`)

- Runs on LEGO CityHub with Pybricks firmware v3.4.0
- Hardcoded locomotive configs at lines 34-40 (hub name → motor type, lights, invert settings)
- Reports status every 1000ms via stdout
- Receives commands via stdin

### Pybricks BLE Service

- Service UUID: `c5f50001-8280-46da-89f4-6d8051e4aeef`
- Command/Event Characteristic: `c5f50002-8280-46da-89f4-6d8051e4aeef`

## Adding a New Train

1. In `MainActivity.kt` (lines 75-99): Add `Train(name, locomotives)` to the `trains` list
2. In `Universal_Train_Program.py` (lines 34-40): Add hub name conditionals if special config needed
3. Flash locomotive with Pybricks firmware, set the hub name to match

## Key Technical Notes

- Requires Pybricks firmware v3.4.0 specifically (protocol compatibility issues with newer versions)
- Android minSdk 31, targetSdk 34
- Uses Jetpack Compose with Material 3
- Train configurations are currently hardcoded (future: SharedPreferences)
