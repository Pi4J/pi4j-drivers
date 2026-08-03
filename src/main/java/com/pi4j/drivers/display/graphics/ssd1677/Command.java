package com.pi4j.drivers.display.graphics.ssd1677;

public enum Command {
    /*
     * Sets the number of gate outputs (rows) and scanning direction.
     * Requires 3 bytes: rows-1 low, rows-1 high, scanning mode.
     */
    DRIVER_OUTPUT_CONTROL(0x01, 3),

    /** VGH */
    GATE_VOLTAGE(0x03, 1),

    /** VSH1, VSH2, VSL */
    SOURCE_DRIVING_VOLTAGE_CONTROL(0x04, 3),

    /** Controls the power-on sequence of the booster circuit. */
    BOOSTER_SOFT_START_CONTROL(0x0C, 5),

    /** 1 byte; 0x0 = normal mode; 0x3 = deep sleep */
    DEEP_SLEEP_MODE(0x10, 1),

    /** Address counter direction; Bit 0: x (0:-; 1:+) Bit 1: y (0:-; 1:+), Bit 2: primary direction 0=x; 1: y) */
    DATA_ENTRY_MODE_SETTING(0x11, 1),

    SW_RESET(0x12, 0),

    /** 0x80: Internal temperature sensor; 0x48: External */
    TEMPERATURE_SENSOR_CONTROL(0x18, 1),

    WRITE_TEMPERATURE_REGISTER(0x1a, 2),

    MASTER_ACTIVATION(0x20, 0),
    /**
     * Controls which ram sources are used for a display update.
     * 00: normal; 0x40 ignore red.
     */
    DISPLAY_UPDATE_CONTROL_1(0x21, 1),

    /**
     * Controls the display update sequence (power on/off, load LUT, etc); values are panel-specific
     *
     * 0x01: Enable clock
     * 0x02: Enable analog
     * 0x04: Load temperature value
     * 0x08: Load LUT
     * 0x10: Initial display (disable bypass)
     * 0x20: Pattern display (refresh)
     * 0x40: Disable analog
     * 0x80: Disable clock
     */
    DISPLAY_UPDATE_CONTROL_2(0x22, 1),

    WRITE_RAM_BW(0x24, -1),

    WRITE_RAM_RED(0x26, -1),

    WRITE_VCOM_REGISTER(0x2C, 1),

    /** Should be 105 bytes */
    WRITE_LUT_REGISTER(0x32, -1),

    WRITE_REGISTER_FOR_DISPLAY_OPTION(0x37, 10),

    /** Controls the border color and transition */
    BORDER_WAVEFORM_CONTROL(0x3C, 1),

    /** Fill the whole bw ram with the given value */
    AUTO_WRITE_BW_RAM(0x46, 1),

    /** Fill the whole red ram with the given value */
    AUTO_WRITE_RED_RAM(0x47, 1),

    SET_RAM_X_ADDRESS_RANGE(0x44, 4),
    SET_RAM_Y_ADDRESS_RANGE(0x45, 4),
    SET_RAM_X_ADDRESS(0x4E, 2),
    SET_RAM_Y_ADDRESS(0x4F, 2),
    ;

    final int code;
    final int dataCount;

    Command(int code, int dataCount) {
        this.code = code;
        this.dataCount = dataCount;
    }
}
