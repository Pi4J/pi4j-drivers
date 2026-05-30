package com.pi4j.drivers.io.expander.pcf8574;

import com.pi4j.drivers.io.expander.OutputExpander;
import com.pi4j.io.OnOffWrite;
import com.pi4j.io.exception.IOException;
import com.pi4j.io.i2c.I2C;

/**
 * As the input and output functionality of this chip uses separate addresses, it seemed most straightforward
 * to implement these as separate classes.
 */
public class Pcf8574OutputDriver implements OutputExpander {
    /** PCF8574 and HLF8574 support a range of 8 addresses starting from 0x20 */
    public static final int PCF8574_ADDRESS_BASE = 0x20;

    /** PCF8574A supports a range of 8 addresses starting from 0x38 */
    public static final int PCF8574A_ADDRESS_BASE = 0x38;

    /** PCF8574T supports 8 addresses starting from 0x40 in increments of 2. */
    public static final int PCF8574T_ADDRESS_BASE = 0x40;  // Odd addresses used for input

    private final I2C i2c;
    private final OnOffWrite<?>[] onOffWriteArray = new OnOffWrite[8];

    // At power on, the I/Os are high.
    private int outputBits = -1;
    private int triggerMask = -1;

    public Pcf8574OutputDriver(I2C i2c) {
        this.i2c = i2c;
        for (int i = 0; i < 8; i++) {
            onOffWriteArray[i] = new OnOffWriteImpl(i);
        }
    }

    @Override
    public void setOutputTriggerMask(int mask) {
        this.triggerMask = mask;
    }

    @Override
    public OnOffWrite<?> getOutput(int index) {
        return onOffWriteArray[index];
    }

    @Override
    public void setOutputState(int index, boolean state) {
        int mask = 1 << index;
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
            this.i2c.write(outputBits);
        }
    }

    private class OnOffWriteImpl implements OnOffWrite<OnOffWriteImpl> {
        final int index;

        OnOffWriteImpl(int index) {
            this.index = index;
        }

        @Override
        public OnOffWriteImpl on() throws IOException {
            Pcf8574OutputDriver.this.setOutputState(index, true);
            return this;
        }

        @Override
        public OnOffWriteImpl off() throws IOException {
            Pcf8574OutputDriver.this.setOutputState(index, false);
            return this;
        }
    }
}
