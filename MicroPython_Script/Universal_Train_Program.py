# 1. Open the website: https://code.pybricks.com/
# 2. Install pybricks-cityhub-v3.4.0.zip on your LEGO City Hub.
# 3. Run this code on the LEGO City Hub.

from pybricks.hubs import CityHub
from pybricks.pupdevices import DCMotor, Motor
from pybricks.pupdevices import Light
from pybricks.parameters import Port, Direction
from pybricks.tools import wait

from ustruct import pack, unpack
from usys import stdin, stdout
from uselect import poll
from micropython import kbd_intr

REPORT_STATUS_INTERVAL_MS = 1000

# Protocol constants
MAGIC = 0x58AB
STATUS      = 0x00
SET_SPEED   = 0x01
SET_LIGHT   = 0x02
BAD_COMMAND = 0x03
BAD_DATA    = 0x04

# Defaults
usesDCMotor = True
hasLights = False
invertSpeed = False

hub = CityHub()
hubName = hub.system.name()

if hubName == "Express_P1":
    hasLights = True
elif hubName == "Express_P2":
    hasLights = True
    invertSpeed = True
elif hubName == "Orient_Express":
    usesDCMotor = False

if usesDCMotor:
    motor = DCMotor(Port.A, Direction.COUNTERCLOCKWISE if invertSpeed else Direction.CLOCKWISE)
else:
    motor = Motor(Port.A, positive_direction=Direction.COUNTERCLOCKWISE)

if hasLights:
    light = Light(Port.B)
    light.off()

speed = 0
brightness = 0

kbd_intr(-1) # to allow binary data in stdin
uart = poll()
uart.register(stdin)

def reportStatus():
    stdout.buffer.write(pack(
        '!HBiihh',
        MAGIC,
        STATUS,
        hub.battery.voltage(),
        hub.battery.current(),
        speed,
        brightness,
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
        elif cmd == SET_SPEED:
            speed = payload
            #if usesDCMotor:
            motor.dc(speed)
            #else:
            #    motor.run(speed * 5)
            reportStatus()
        elif cmd == SET_LIGHT and hasLights:
            brightness = payload
            if brightness == 0:
                light.off()
            else:
                light.on(brightness)
            reportStatus()
        else:
            stdout.buffer.write(pack('!HB', MAGIC, BAD_COMMAND))
    else:
        stdout.buffer.write(pack('!HB', MAGIC, BAD_DATA))
