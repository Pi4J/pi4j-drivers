package com.pi4j.drivers.io.expander.mcp23008;

import com.pi4j.io.OnOffWrite;
import com.pi4j.io.exception.IOException;
import com.pi4j.io.i2c.I2C;

/**
 * Driver for the MCP 23008 io expander. Currently supports output only.
 */
public class Mcp23008Driver {
    private final I2C i2c;
    private final OnOffWrite<?>[] onOffWriteArray = new OnOffWrite[8];

    private int outputBits = 0x0;
    private int triggerMask = -1;
    private int ioDir = 0xff;

    public Mcp23008Driver(I2C i2c) {
        this.i2c = i2c;
        for (int i = 0; i < 8; i++) {
            int pinIndex = i;
            onOffWriteArray[pinIndex] = new OnOffWrite<>() {
                @Override
                public Object on() throws IOException {
                    setPin(pinIndex, true);
                    return this;
                }

                @Override
                public Object off() throws IOException {
                    setPin(pinIndex, false);
                    return this;
                }
            };
        }
    }

    /**
     * Sets which pin changes trigger sending the changed output (e.g. typically the "enable" line).
     * By default, all changes are sent immediately.
     */
    public void setTriggerMask(int mask) {
        this.triggerMask = mask;
    }

    /** Set each bit to 0 for output and 1 for input to configure the corresponding pin. */
    public void setIoDir(int ioDir) {
        this.ioDir = ioDir;
        i2c.writeRegister(Register.IODIR.ordinal(), ioDir);
    }

    public boolean setPin(int index, boolean state) {
        int mask = 1 << index;
        if ((ioDir & mask) != 0) {
            throw new IllegalStateException("Pin " + index + " is configured for output.");
        }
        return state ? setOutput(outputBits | mask) :  setOutput(outputBits & ~mask);
    }

    public boolean setOutput(int bits) {
        int changedBits = outputBits ^ bits;
        outputBits = bits;
        if ((changedBits & triggerMask) == 0) {
            return false;
        }
        this.i2c.writeRegister(Register.GPIO.ordinal(), outputBits);
        return true;
    }

    /** Allows handing the output pin interface to other drivers */
    public OnOffWrite<?> getOnOffWrite(int index) {
        return onOffWriteArray[index];
    }

    enum Register {
        IODIR,
        IPOL,
        GPINTEN,
        DEFVAL,
        INTCON,
        IOCON,
        GPPU,
        INTF,
        INTCAP, // Read-only)
        GPIO,
        OLAT
    }


}
