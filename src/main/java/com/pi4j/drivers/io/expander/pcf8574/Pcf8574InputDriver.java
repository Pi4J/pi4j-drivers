package com.pi4j.drivers.io.expander.pcf8574;

import com.pi4j.drivers.io.expander.InputExpander;
import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.i2c.I2C;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;


/**
 * Input driver for the Pcf8574.
 * <p>
 * As the input and output functionality of this chip uses separate addresses, it seemed most straightforward
 * to implement these as separate classes.
 */
public class Pcf8574InputDriver extends AbstractInputDriver {
    private final I2C i2c;

    /**
     * Creates a new PCF 8574 input driver. The interrupt pin will be used to trigger update
     * requests from the chip. If null, state changes can still be observed via the poll() method.
     */
    public Pcf8574InputDriver(I2C i2c, ListenableOnOffRead<?> interruptPin) {
        super (8, interruptPin)
        this.i2c = i2c;
    }

    @Override
    public int poll() {
        return i2c.read();

    }

    @Override
    public void close() {
        super.close();
        i2c.close();
    }
}
