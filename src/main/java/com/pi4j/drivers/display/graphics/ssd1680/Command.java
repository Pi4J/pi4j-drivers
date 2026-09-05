package com.pi4j.drivers.display.graphics.ssd1680;

public enum Command {
    /*
     * Sets the number of gate outputs (rows) and scanning direction.
     * Requires 3 bytes: rows-1 low, rows-1 high, scanning mode.
     */
    DRIVER_OUTPUT_CONTROL(0x01, 3),

    /** 1 byte; 0x0 = normal mode; 0x3 = deep sleep */
    DEEP_SLEEP_MODE(0x10, 1),

    /** Address counter direction; Bit 0: x (0:-; 1:+) Bit 1: y (0:-; 1:+), Bit 2: primary direction 0=x; 1: y) */
    DATA_ENTRY_MODE_SETTING(0x11, 1),

    SW_RESET(0x12, 0),

    /** 0x80: Internal temperature sensor; 0x48: External */
    TEMPERATURE_SENSOR_CONTROL(0x18, 1),

    MASTER_ACTIVATION(0x20, 0),
    /**
     * Controls which ram sources are used for a display update.
     * 00: normal; 0x40 ignore red.
     */
    DISPLAY_UPDATE_CONTROL_1(0x21, 2),

    DISPLAY_UPDATE_CONTROL_2(0x22, 1),

    WRITE_RAM(0x24, -1),
    WRITE_RAM_2(0x26, -1),

    BORDER_WAVEFORM_CONTROL(0x3C, 1),

    SET_RAM_X_ADDRESS_RANGE(0x44, 2),
    SET_RAM_Y_ADDRESS_RANGE(0x45, 4),

    SET_RAM_X_ADDRESS(0x4E, 1),
    SET_RAM_Y_ADDRESS(0x4F, 2),
    ;

    final int code;
    final int dataCount;

    Command(int code, int dataCount) {
        this.code = code;
        this.dataCount = dataCount;
    }
}
