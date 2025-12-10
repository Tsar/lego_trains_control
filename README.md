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

1. Install **[pybricks-cityhub-v3.4.0.zip](https://github.com/pybricks/pybricks-micropython/releases/download/v3.4.0/pybricks-cityhub-v3.4.0.zip)** to your LEGO locomotive. Follow [this guide](https://pybricks.com/install/technic-boost-city/), but **make sure to set up some Hub name and remember it**.
   **Important note: latest Pybricks Firmware version won't work with the App, there is some protocol incompatibility at the moment! You should install from the ZIP archive.**
   ![image](https://github.com/Tsar/lego_trains_control/assets/213696/29247bf9-7199-4494-a107-2ec816afb964)
2. Load the MircoPython script to all your locomotives ([the guide](https://pybricks.com/install/running-programs/)).
3. Build and install the App to your Android device using Android Studio.

## How to use

1. Open the App and turn on the locomotives by pressing the buttons on the hubs. The order of these actions does not matter.
2. Press the "+" button in the top right corner to add a new train.
3. The App will discover all available locomotives. Select one or more to form a train and give it a name.
4. The train will appear on the main screen with controls. Have fun!
5. If the App was force-closed or crashed while locomotives were connected, short-press the hub button on each locomotive to restore Bluetooth advertising and reopen the App.
6. Remember to switch off the locomotives by long-pressing the hub buttons after use. The LED should stop blinking.

## Future plans

1. **Firmware Management**: Integrate functionality within the app to install Pybricks firmware and upload the MicroPython script to the locomotive, streamlining the setup process.
2. **App Release**: Publish the app on the Google Play Store to make it widely accessible to users.
