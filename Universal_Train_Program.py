from pybricks.hubs import CityHub
from pybricks.pupdevices import DCMotor
from pybricks.pupdevices import Light
from pybricks.parameters import Port
from pybricks.tools import wait

from ustruct import pack, unpack
from usys import stdin, stdout
from uselect import poll
from micropython import kbd_intr

# Protocol constants
MAGIC = 0x58AB
STATUS      = 0x00
SET_SPEED   = 0x01
SET_LIGHT   = 0x02
BAD_COMMAND = 0x03
BAD_DATA    = 0x04

hub = CityHub()
motor = DCMotor(Port.A)
light = Light(Port.B)

speed = 0
brightness = 5

light.on(brightness)

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

while True:
    while not uart.poll(0):
        wait(10)
    data = stdin.buffer.read(5)
    magic, cmd, payload = unpack('!HBh', data)
    if magic == MAGIC:
        if cmd == STATUS:
            reportStatus()
        elif cmd == SET_SPEED:
            speed = payload
            motor.dc(speed)
            reportStatus()
        elif cmd == SET_LIGHT:
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
