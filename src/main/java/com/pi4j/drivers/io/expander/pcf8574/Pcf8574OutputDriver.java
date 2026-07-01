package com.pi4j.drivers.io.expander.pcf8574;

import com.pi4j.io.i2c.I2C;

import java.io.Closeable;

/**
 * @deprecated Please use Pcf8574Driver instead.
 */
public class Pcf8574OutputDriver extends Pcf8574Driver implements Closeable {
    @Deprecated
    public static final int PCF8574_ADDRESS_BASE = Pcf8574Constants.PCF8574_ADDRESS_BASE;

    @Deprecated
    public static final int PCF8574A_ADDRESS_BASE = Pcf8574Constants.PCF8574A_ADDRESS_BASE;

    public Pcf8574OutputDriver(I2C i2c) {
        super(i2c, null, 0, 0xff);
    }
}
