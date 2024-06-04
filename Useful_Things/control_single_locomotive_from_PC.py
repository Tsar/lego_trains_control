#!/usr/bin/env python3.10

import sys
import struct
import asyncio

from bleak import BleakScanner, BleakClient
from PyQt5.QtWidgets import QApplication, QWidget, QHBoxLayout, QVBoxLayout, QLabel, QPushButton, QDesktopWidget, QSlider
from PyQt5.QtCore import Qt
from qasync import QEventLoop, asyncSlot

from pybricks import PYBRICKS_COMMAND_EVENT_UUID, Command, Event, StatusFlag

HUB_NAME = 'Express_P2'

# Protocol constants
MAGIC = 0x58AB
STATUS      = 0x00
SET_SPEED   = 0x01
SET_LIGHT   = 0x02
BAD_COMMAND = 0x03
BAD_DATA    = 0x04

def createHorizontalSlider(parent, minimum, maximum):
    slider = QSlider(Qt.Horizontal, parent)
    slider.setMinimum(minimum)
    slider.setMaximum(maximum)
    slider.setValue(0)
    slider.setTickPosition(QSlider.TicksBelow)
    slider.setTickInterval(10)
    return slider

class ControlsUI(QWidget):
    def __init__(self):
        super().__init__()

        self.client = None

        self.title = QLabel('Green Express P2', self)
        self.btnDiscover = QPushButton("Discover", self)
        self.btnDiscover.clicked.connect(self.discoverTrain)
        self.sliderSpeed = createHorizontalSlider(self, -100, 100)
        self.sliderSpeed.setMinimumWidth(400)
        self.btnSetSpeed = QPushButton("Set", self)
        self.btnSetSpeed.clicked.connect(self.sendSpeed)
        self.btnStop = QPushButton("Stop", self)
        self.btnStop.clicked.connect(self.sendStop)
        self.sliderLights = createHorizontalSlider(self, 0, 100)
        self.btnSetLights = QPushButton("Set", self)
        self.btnSetLights.clicked.connect(self.sendBrightness)

        mainLayout = QHBoxLayout()
        mainLayout.addWidget(self.title)
        mainLayout.addWidget(self.btnDiscover)
        mainLayout.addWidget(self.sliderSpeed)
        mainLayout.addWidget(self.btnSetSpeed)
        mainLayout.addWidget(self.btnStop)
        mainLayout.addWidget(self.sliderLights)
        mainLayout.addWidget(self.btnSetLights)

        self.setLayout(mainLayout)
        self.setMinimumSize(800, 10)

        # Positioning at the center of the screen
        screenGeometry = QDesktopWidget().availableGeometry()
        windowGeometry = self.frameGeometry()
        centerPoint = screenGeometry.center()
        windowGeometry.moveCenter(centerPoint)
        self.move(windowGeometry.topLeft())

        self.setWindowTitle('Lego Locomotive Control')
        self.show()

    async def sendCommand(self, cmd, payload=0):
        await self.client.write_gatt_char(
            PYBRICKS_COMMAND_EVENT_UUID,
            struct.pack('!BHBh', Command.WRITE_STDIN, MAGIC, cmd, payload),
            response=True,
        )

    @asyncSlot()
    async def discoverTrain(self):
        mainTask = asyncio.current_task()

        def handleDisconnect(_):
            print('Hub was disconnected')
            if not mainTask.done():
                mainTask.cancel()

        async def dataHandler(_, data: bytearray):
            if data[0] == Event.STATUS_REPORT:
                (flags,) = struct.unpack_from('<I', data, 1)
                if not bool(flags & StatusFlag.POWER_BUTTON_PRESSED) and not bool(
                        flags & StatusFlag.USER_PROGRAM_RUNNING):
                    print('Starting MicroPython program...')
                    await self.client.write_gatt_char(
                        PYBRICKS_COMMAND_EVENT_UUID,
                        struct.pack('<B', Command.START_USER_PROGRAM),
                        response=True,
                    )
            elif data[0] == Event.WRITE_STDOUT:
                data = data[1:]
                if len(data) < 3:
                    print(f'Received too short data, only {len(data)} bytes')
                    return
                magic, cmd = struct.unpack('!HB', data[0:3])
                if magic == MAGIC:
                    if cmd == STATUS:
                        if len(data) != 15:
                            print(f'Received too short status package, only {len(data)} bytes')
                        voltage, current, speed, light = struct.unpack('!iihh', data[3:])
                        print('Status:', voltage, current, speed, light)
                    elif cmd == BAD_COMMAND:
                        print('Device says BAD_COMMAND')
                    elif cmd == BAD_DATA:
                        print('Device says BAD_DATA')
                else:
                    print('Received package which starts not from MAGIC')

        print(f'Searching for device: {HUB_NAME}')
        device = await BleakScanner.find_device_by_name(HUB_NAME)
        if device is None:
            print(f'Failed to find device: {HUB_NAME}')
            return
        print(device.details)

        async with BleakClient(device, handleDisconnect) as self.client:
            await self.client.start_notify(PYBRICKS_COMMAND_EVENT_UUID, dataHandler)
            while True:
                await asyncio.sleep(5)
                print('Requesting status from MicroPython')
                await self.sendCommand(STATUS)

    @asyncSlot()
    async def sendSpeed(self):
        await self.sendCommand(SET_SPEED, self.sliderSpeed.value())

    @asyncSlot()
    async def sendStop(self):
        self.sliderSpeed.setValue(0)
        await self.sendCommand(SET_SPEED, 0)

    @asyncSlot()
    async def sendBrightness(self):
        await self.sendCommand(SET_LIGHT, self.sliderLights.value())

async def main():
    app = QApplication(sys.argv)
    loop = QEventLoop(app)
    asyncio.set_event_loop(loop)
    window = ControlsUI()
    with loop:
        loop.run_forever()

if __name__ == "__main__":
    asyncio.run(main())
