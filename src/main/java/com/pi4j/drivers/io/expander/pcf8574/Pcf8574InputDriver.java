package com.pi4j.drivers.io.expander.pcf8574;

import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.i2c.I2C;

/**
 * @deprecated Please use Pcf8574Driver instead.
 */
@Deprecated
public class Pcf8574InputDriver extends Pcf8574Driver {

    /**
     * Creates a new PCF 8574 input driver. The interrupt pin will be used to trigger update
     * requests from the chip. If null, state changes can still be observed via the poll() method.
     */
    public Pcf8574InputDriver(I2C i2c, ListenableOnOffRead<?> interruptPin) {
        super (i2c, interruptPin, 0xff, 0xff);
    }
}
