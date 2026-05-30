package com.pi4j.drivers.sensor.environment.tcs3400;

public class Register {
    static final int ENABLE  = 0x80; // R/W Enables states and interrupts 0x00
    static final int ATIME   = 0x81; // R/W RGBC integration time 0xFF
    static final int WTIME   = 0x83; // R/W Wait time 0xFF
    static final int AILTL   = 0x84; // R/W Clear interrupt low threshold low byte 0x00
    static final int AILTH   = 0x85; // R/W Clear interrupt low threshold high byte 0x00
    static final int AIHTL   = 0x86; // R/W Clear interrupt high threshold low byte 0x00
    static final int AIHTH   = 0x87; // R/W Clear interrupt high threshold high byte 0x00
    static final int PERS    = 0x8C; // R/W Interrupt persistence filter 0x00
    static final int CONFIG  = 0x8D; // R/W Configuration 0x40
    static final int CONTROL = 0x8F; // R/W Gain control register 0x00
    static final int AUX     = 0x90; // R/W Auxiliary control register 0x00
    static final int REVID   = 0x91; // R Revision ID Rev
    static final int ID      = 0x92; // R Device ID ID
    static final int STATUS  = 0x93; // R Device status 0x00
    static final int CDATAL  = 0x94; // R Clear / IR channel low data register 0x00
    static final int CDATAH  = 0x95; // R Clear / IR channel high data register 0x00
    static final int RDATAL  = 0x96; // R Red ADC low data register 0x00
    static final int RDATAH  = 0x97; // R Red ADC high data register 0x00
    static final int GDATAL  = 0x98; // R Green ADC low data register 0x00
    static final int GDATAH  = 0x99; // R Green ADC high data register 0x00
    static final int BDATAL  = 0x9A; // R Blue ADC low data register 0x00
    static final int BDATAH  = 0x9B; // R Blue ADC high data register 0x00
    static final int IR      = 0xC0; // R/W Access IR Channel 0x00
    static final int IFORCE  = 0xE4; // W Force Interrupt 0x00
    static final int CICLEAR = 0xE6; // W Clear channel interrupt clear 0x00
    static final int AICLEAR = 0xE7; // W Clear all interrupts
}
