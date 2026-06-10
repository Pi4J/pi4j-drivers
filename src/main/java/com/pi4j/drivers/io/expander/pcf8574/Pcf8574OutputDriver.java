package com.pi4j.drivers.io.expander.pcf8574;

import com.pi4j.drivers.io.expander.OutputExpander;
import com.pi4j.io.OnOffWrite;
import com.pi4j.io.exception.IOException;
import com.pi4j.io.i2c.I2C;

/**
 * As the input and output functionality of this chip uses separate addresses, it seemed most straightforward
 * to implement these as separate classes.
 */
public class Pcf8574OutputDriver extends AbstractOutputExpander {
    @Deprecated
    public static final int PCF8574_ADDRESS_BASE = Pcf8574Constants.PCF8574_ADDRESS_BASE

    @Deprecated
    public static final int PCF8574A_ADDRESS_BASE = Pcf8574Constants.PCF8574A_ADDRESS_BASE

    @Deprecated
    public static final int PCF8574T_ADDRESS_BASE = Pcf8574Constants.PCF8574T_ADDRESS_BASE  // Odd addresses used for input

    private final I2C i2c;

    public Pcf8574OutputDriver(I2C i2c) {
        this.i2c = i2c;
    }

    @Override
    protected writeOutputImpl(int outputBits) {
        i2c.write(outputBits);
    }

    @Override
    public void close() {
        super.close();
        i2c.close();
    }
}
