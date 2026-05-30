package com.pi4j.drivers.io.expander.mcp23008;

import com.pi4j.drivers.io.expander.OutputExpander;
import com.pi4j.io.OnOffWrite;
import com.pi4j.io.exception.IOException;
import com.pi4j.io.i2c.I2C;

/**
 * Driver for the MCP 23008 io expander. Supports output only currently.
 *
 * Datasheet:
 * https://ww1.microchip.com/downloads/aemDocuments/documents/APID/ProductDocuments/DataSheets/MCP23008-MCP23S08-Data-Sheet-DS20001919.pdf
 */
public class Mcp23008Driver implements OutputExpander {
    private final I2C i2c;
    private final OnOffWrite<?>[] onOffWriteArray = new OnOffWrite[8];

    private int outputBits = 0x0;
    private int triggerMask = -1;
    private int ioDir = -1;

    public Mcp23008Driver(I2C i2c) {
        this.i2c = i2c;
        for (int i = 0; i < 8; i++) {
            onOffWriteArray[i] = new OnOffWriteImpl(i);
        }
    }

    @Override
    public void setOutputTriggerMask(int mask) {
        this.triggerMask = mask;
    }

    /** Set each bit to 0 for output and 1 for input to configure the corresponding pin. */
    public void setIoDir(int ioDir) {
        this.ioDir = ioDir;
        i2c.writeRegister(Register.IODIR, ioDir);
    }

    @Override
    public void setOutputState(int index, boolean state) {
        int mask = 1 << index;
        if ((ioDir & mask) != 0) {
            throw new IllegalStateException("Pin " + index + " is configured for output.");
        }
        if (state) {
            setOutputState(outputBits | mask);
        } else {
            setOutputState(outputBits & ~mask);
        }
    }

    @Override
    public void setOutputState(int bits) {
        int changedBits = outputBits ^ bits;
        outputBits = bits;
        if ((changedBits & triggerMask) != 0) {
            this.i2c.writeRegister(Register.GPIO, outputBits);
        }
    }

    @Override
    public OnOffWrite<?> getOutput(int index) {
        return onOffWriteArray[index];
    }

    private class OnOffWriteImpl implements OnOffWrite<OnOffWriteImpl> {
        final int index;

        OnOffWriteImpl(int index) {
            this.index = index;
        }

        @Override
        public OnOffWriteImpl on() throws IOException {
            setOutputState(index, true);
            return this;
        }

        @Override
        public OnOffWriteImpl off() throws IOException {
            setOutputState(index, false);
            return this;
        }
    }
}
