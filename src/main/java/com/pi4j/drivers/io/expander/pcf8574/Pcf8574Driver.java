package com.pi4j.drivers.io.expander.pcf8574;

import com.pi4j.drivers.io.expander.AbstractIoExpander;
import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.i2c.I2C;

/**
 * Input driver for the Pcf8574.
 * <p>
 * As the input and output functionality of this chip uses separate addresses, it seemed most straightforward
 * to implement these as separate classes.
 */
public class Pcf8574Driver extends AbstractIoExpander {
    private final I2C i2c;

    /**
     * Creates a new PCF 8574 driver with all pins set to 1 (matching the reset state), configured for output.
     */
    public static Pcf8574Driver createForOutput(I2C i2c) {
        return new Pcf8574Driver(i2c, null, 0x00, 0xff);
    }

    /** Creates a new PCF 8574 driver with all pins set to 1 (matching the default state) and configured for input. */
    public static Pcf8574Driver createForInput(I2C i2c, ListenableOnOffRead<?> interruptPin) {
        return new Pcf8574Driver(i2c, interruptPin, 0xff, 0xff);
    }

    /**
     * Creates a new PCF 8574 input driver. The interrupt pin will be used to trigger update
     * requests from the chip. If null, state changes can still be observed via the poll() method.
     */
    public Pcf8574Driver(I2C i2c, ListenableOnOffRead<?> interruptPin, int inputPins, int initialOutputStates) {
        super (8, interruptPin);
        this.i2c = i2c;
        this.outputStates = initialOutputStates;
        this.inputDirectionBits = inputPins;
        setIoDirectionsImpl(inputDirectionBits);
    }

    @Override
    protected int readInputsImpl() {
        return i2c.read();
    }

    // In order to use a pin exclusively as an input, the pin should be driven HIGH (default at startup) so the pin
    // remains weakly pulled to Vdd.
    @Override
    protected void writeOutputsImpl(int outputs) {
        i2c.write(outputs | inputDirectionBits);
    }

    // In order to use a pin exclusively as an input, the pin should be driven HIGH (default at startup) so the pin
    // remains weakly pulled to Vdd.
    @Override
    protected void setIoDirectionsImpl(int inputPins) {
        i2c.write(outputStates | inputPins);
    }

    @Override
    public void close() {
        super.close();
        i2c.close();
    }
}
