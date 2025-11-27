# LEGO Trains Control

This project enables convenient control of multiple real LEGO trains from a single screen.

Each train can have multiple locomotives, and their speed is controlled simultaneously by a single slider. For example, that could be 2 x LEGO City Trains 60337.

Currently the interface of the App looks like this:

<img src="https://github.com/Tsar/lego_trains_control/assets/213696/0129282c-9bfa-45fc-8408-fdbe04582ad0" width="360" height="800">

Note: _the camera view is from the AZ Screen Recorder app and is not a part of this project._

## Project overview

The project consists of:
* Android Application;
* MicroPython script for Pybricks, only the **CityHub** is currently supported!

## How to set up

Currently setting everything up may be not an easy thing, but in future all these steps won't be required and the App will be available on Google Play Market (see "Future plans" section).

1. Install **[pybricks-cityhub-v3.4.0.zip](https://github.com/pybricks/pybricks-micropython/releases/download/v3.4.0/pybricks-cityhub-v3.4.0.zip)** to your LEGO locomotive. Follow [this guide](https://pybricks.com/install/technic-boost-city/), but **make sure to set up some Hub name and remember it**.
   **Important note: latest Pybricks Firmware version won't work with the App, there is some protocol incompatibility at the moment! You should install from the ZIP archive.**
   ![image](https://github.com/Tsar/lego_trains_control/assets/213696/29247bf9-7199-4494-a107-2ec816afb964)
2. Adjust MicroPython script to know your locomotive configurations. Here are [the hardcoded lines you need to change](https://github.com/Tsar/lego_trains_control/blob/11f647b088e48be239c11c9d900243ea5d77dfa9/MicroPython_Script/Universal_Train_Program.py#L34-L40).
3. Load the MircoPython script to all your locomotives ([the guide](https://pybricks.com/install/running-programs/)).
4. Adjust App to know your train and locomotive configurations. Here are [the hardcoded lines you need to change](https://github.com/Tsar/lego_trains_control/blob/11f647b088e48be239c11c9d900243ea5d77dfa9/App/app/src/main/java/ru/tsar_ioann/legotrainscontrol/MainActivity.kt#L76-L90).
5. Build and install App to your Android device using Android Studio.

## How to use

1. Open the App and turn on the locomotives by pressing the buttons on the hubs. The order of these actions does not matter.
2. The App will discover the trains, and the controls will become available. Have fun!
3. Remember to switch off the locomotives by holding down the hub buttons after use. The LED should stop blinking in any manner.

## Future plans

1. **Add/Edit Train Functionality**: Implement the ability to add and edit trains directly within the app, and store these configurations in `SharedPreferences`. Currently, the list of trains is hardcoded, which limits flexibility.
2. **Universal MicroPython Script**: Improve the MicroPython script to be more universal by allowing the app to communicate the specific features of each locomotive during connection establishment. This will eliminate the need for hardcoded configurations within the MicroPython script.
3. **Firmware Management**: Integrate functionality within the app to install Pybricks firmware and upload the MicroPython script to the locomotive, streamlining the setup process.
4. **App Release**: Publish the app on the Google Play Store to make it widely accessible to users.
