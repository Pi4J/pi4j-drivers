package com.pi4j.drivers.io.expander.mcp23017;

import com.pi4j.drivers.io.expander.mcp23008.Mcp23008Driver;
import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.i2c.I2C;

/**
 * A 16-bit variant of the MCP23008. For documentation, please refer to the Mcp23008Driver class.
 * <p>
 * Note that this driver represents all A/B registers as single 16 bit registers.
 */
public class Mcp23017Driver extends Mcp23008Driver {

    /** Contains the bit mask for MIRROR ({@code 1<<6}) used in the setIoConfiguration() call. */
    public static final int MIRROR = 1 << 6;

    /** Contains the bit mask for BANK ({@code 1<<7}) used in the setIoConfiguration() call. */
    public static final int BANK = 1 << 7;


    public Mcp23017Driver(I2C i2c) {
        this(i2c, null);
    }

    /**
     * If an interruptPin is provided, we enable change interrupts on all pins and set the pins to MIRROR mode.
     * For more details, please refer to the superclass documentation.
     */
    public Mcp23017Driver(I2C i2c, ListenableOnOffRead<?> interruptPin) {
        super(i2c, 16, interruptPin);
        if (interruptPin != null) {
            // Enable mirror, so we see interrupts for all pins on one interrupt pin.
            setIoConfiguration(getIoConfiguration() | MIRROR);
        }
    }

    protected void writeRegister(int register, int value) {
        // In unbanked mode, the 8 bit register addresses basically double.
        super.writeRegister(2 * register, value);
        // IOCON(5) is an 8 bit register and we don't want to overwrite the value with potential junk from the
        // higher bits.
        if (register != 5) {
            super.writeRegister(2 * register + 1, value >>> 8);
        }
    }

    protected int readRegister(int register) {
        // In unbanked mode, the 8 bit register addresses basically double.
        return super.readRegister(2 * register) | (super.readRegister(2 * register + 1) << 8);
    }
}
