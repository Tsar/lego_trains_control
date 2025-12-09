package ru.tsar_ioann.legotrainscontrol

/**
 * Device types auto-detected by MicroPython on hub ports.
 * Must match values in Universal_Train_Program.py.
 */
enum class DeviceType(val code: Byte) {
    NONE(0x00),
    DC_MOTOR(0x01),
    MOTOR(0x02),
    LIGHT(0x03);

    val isMotor: Boolean
        get() = this == DC_MOTOR || this == MOTOR

    companion object {
        fun fromCode(code: Byte): DeviceType = entries.find { it.code == code } ?: NONE
    }
}
