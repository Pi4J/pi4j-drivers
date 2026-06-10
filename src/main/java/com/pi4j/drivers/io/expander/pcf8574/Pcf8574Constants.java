package com.pi4j.drivers.io.expander.pcf8574;

public class Pcf8574Constants {

    /** PCF8574 and HLF8574 support a range of 8 addresses starting from 0x20 */
    public static final int PCF8574_ADDRESS_BASE = 0x20;

    /** PCF8574A supports a range of 8 addresses starting from 0x38 */
    public static final int PCF8574A_ADDRESS_BASE = 0x38;

    /** PCF8574T supports 8 addresses starting from 0x40 in increments of 2. */
    public static final int PCF8574T_ADDRESS_BASE = 0x40;  // Odd addresses used for input

    private Pcf8574Constants() {}
}