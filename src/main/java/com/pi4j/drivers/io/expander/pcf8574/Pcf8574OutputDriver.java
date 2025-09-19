package com.pi4j.drivers.io.expander.pcf8574;

import com.pi4j.io.OnOffWrite;
import com.pi4j.io.exception.IOException;
import com.pi4j.io.i2c.I2C;

/**
 * As the input and output functionality of this chip uses separate addresses, it seemed most straightforward
 * to implement these as separate classes.
 */
public class Pcf8574OutputDriver {
    /** PCF8574 and HLF8574 support a range of 8 addresses starting from 0x20 */
    public static final int PCF8574_ADDRESS_BASE = 0x20;

    /** PCF8574A supports a range of 8 addresses starting from 0x38 */
    public static final int PCF8574A_ADDRESS_BASE = 0x38;

    /** PCF8574T supports 8 addresses starting from 0x40 in increments of 2. */
    public static final int PCF8574T_ADDRESS_BASE = 0x40;  // Odd addresses used for input

    private final I2C i2c;
    private final OnOffWrite<?>[] onOffWriteArray = new OnOffWrite[8];

    // At power on, the I/Os are high.
    private int outputBits = 0xff;
    private int triggerMask = 0xff;

    public Pcf8574OutputDriver(I2C i2c) {
        this.i2c = i2c;
        for (int i = 0; i < 8; i++) {
            final int bitIndex = i;
            onOffWriteArray[bitIndex] = new OnOffWrite<>() {
                @Override
                public Object on() throws IOException {
                    setPin(bitIndex, true);
                    return this;
                }

                @Override
                public Object off() throws IOException {
                    setPin(bitIndex, false);
                    return this;
                }
            };
        }
    }

    /**
     * Sets a mask for which bit changes trigger sending the changed state over i2c. By default,
     * the mask is 0xff and all bit changes trigger an update.
     */
    public void setTriggerMask(int mask) {
        this.triggerMask = mask;
    }

    /**
     * Writing to this output will set the corresponding output pin of the chip. This allows handing a pin
     * instance to other drivers, e.g. for a HD44780 display, where this chip is commonly used.
     */
    public OnOffWrite<?> getOnOffWrite(int bitIndex) {
        return onOffWriteArray[bitIndex];
    }

    /**
     * Sets the pin with the given index to the given state. Returns true if an update was sent,
     * i.e. the bit changed and is covered by the trigger mask.
     */
    public boolean setPin(int index, boolean state) {
        int mask = 1 << index;
        return state ? setOutput(outputBits | mask) :  setOutput(outputBits & ~mask);
    }

    /**
     * Sets all pins at once, mapping each bit to the corresponding pin number.
     * Returns true if an update was sent.
     */
    public boolean setOutput(int bits) {
        int changedBits = outputBits ^ bits;
        outputBits = bits;
        if ((changedBits & triggerMask) == 0) {
            return false;
        }
        this.i2c.write(outputBits);
        return true;
    }
}
