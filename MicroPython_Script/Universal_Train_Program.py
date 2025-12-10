# 1. Open the website: https://code.pybricks.com/
# 2. Install pybricks-cityhub-v3.4.0.zip on your LEGO City Hub.
# 3. Run this code on the LEGO City Hub.

from pybricks.hubs import CityHub
from pybricks.pupdevices import DCMotor, Motor
from pybricks.pupdevices import Light
from pybricks.parameters import Port
from pybricks.tools import wait

from ustruct import pack, unpack
from usys import stdin, stdout
from uselect import poll
from micropython import kbd_intr

REPORT_STATUS_INTERVAL_MS = 1000

# Protocol constants
MAGIC = 0x58AB
STATUS       = 0x05
SET_DEVICE_A = 0x0A
SET_DEVICE_B = 0x0B
BAD_COMMAND  = 0x03
BAD_DATA     = 0x04

DEVICE_NONE     = 0x00
DEVICE_DC_MOTOR = 0x01
DEVICE_MOTOR    = 0x02
DEVICE_LIGHT    = 0x03

hub = CityHub()

class ControllableDevice:
    def __init__(self, port):
        self.device = None
        self.deviceType = DEVICE_NONE
        self.value = 0  # speed or brightness (depending on device type)
        # Auto-detection of device type
        try:
            self.device = DCMotor(port)
            self.deviceType = DEVICE_DC_MOTOR
            return
        except:
            pass
        try:
            self.device = Motor(port)
            self.deviceType = DEVICE_MOTOR
            return
        except:
            pass
        try:
            self.device = Light(port)
            self.deviceType = DEVICE_LIGHT
            return
        except:
            pass

    def getType(self):
        return self.deviceType

    def getValue(self):
        return self.value

    def setValue(self, value):
        self.value = value
        if self.deviceType == DEVICE_DC_MOTOR:
            self.device.dc(value)
        elif self.deviceType == DEVICE_MOTOR:
            self.device.dc(value)  # could also be: self.device.run(value * 5)
        elif self.deviceType == DEVICE_LIGHT:
            if value == 0:
                self.device.off()
            else:
                self.device.on(value)

deviceA = ControllableDevice(Port.A)
deviceB = ControllableDevice(Port.B)

kbd_intr(-1) # to allow binary data in stdin
uart = poll()
uart.register(stdin)

def reportStatus():
    stdout.buffer.write(pack(
        '!HBiiBhBh',
        MAGIC,
        STATUS,
        hub.battery.voltage(),
        hub.battery.current(),
        deviceA.getType(),
        deviceA.getValue(),
        deviceB.getType(),
        deviceB.getValue(),
    ))

reportStatus()
timeCounter = 0

while True:
    while not uart.poll(0):
        wait(10)
        timeCounter += 10
        if timeCounter >= REPORT_STATUS_INTERVAL_MS:
            reportStatus()
            timeCounter = 0

    data = stdin.buffer.read(5)
    magic, cmd, payload = unpack('!HBh', data)
    if magic == MAGIC:
        if cmd == STATUS:
            reportStatus()
        elif cmd == SET_DEVICE_A:
            deviceA.setValue(payload)
            reportStatus()
        elif cmd == SET_DEVICE_B:
            deviceB.setValue(payload)
            reportStatus()
        else:
            stdout.buffer.write(pack('!HB', MAGIC, BAD_COMMAND))
    else:
        stdout.buffer.write(pack('!HB', MAGIC, BAD_DATA))
