package com.pi4j.drivers.io.expander;

import com.pi4j.drivers.io.expander.OutputExpander;
import com.pi4j.io.OnOffWrite;
import com.pi4j.io.exception.IOException;
import com.pi4j.io.i2c.I2C;


public class AbstractOutputExpander implements OutputExpander, Closeable {
    private final int size;
    private final OnOffWrite<?>[] onOffWriteArray;

    // At power on, the I/Os are high.
    private int outputBits = -1;
    private int triggerMask = -1;

    public AbstractOutputExpander(int size) {
        this.size = size;
        this.onOffWriteArray = new OnOffWrite[size];
        for (int i = 0; i < size; i++) {
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
            writeOutputImpl(outputBits);
        }
    }

    abstract protected void writeOutputImpl(int bits);

    private class OnOffWriteImpl implements OnOffWrite<OnOffWriteImpl> {
        final int index;

        OnOffWriteImpl(int index) {
            this.index = index;
        }

        @Override
        public OnOffWriteImpl on() throws IOException {
            AbstractOutputExpander.this.setOutputState(index, true);
            return this;
        }

        @Override
        public OnOffWriteImpl off() throws IOException {
            AbstractOutputExpander.this.setOutputState(index, false);
            return this;
        }
    }
}
