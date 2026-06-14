package com.pi4j.drivers.io.expander;

import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.OnOffWrite;

public abstract class AbstractConfigurableIoExpander extends AbstractInputExpander implements ConfigurableIoExpander {

    private final OnOffWrite<?>[] onOffWriteArray;

    private int outputBits = -1;
    private int triggerMask = -1;

    public AbstractConfigurableIoExpander(int size, ListenableOnOffRead<?> interruptPin) {
        super(size, interruptPin);
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
    public void setOutputStates(int mask, boolean state) {
        if (state) {
            setOutputState(outputBits | mask);
        } else {
            setOutputState(outputBits & ~mask);
        }
    }

    @Override
    public void setOutputStates(int bits) {
        int changedBits = outputBits ^ bits;
        outputBits = bits;
        if ((changedBits & triggerMask) != 0) {
            writeOutputsImpl(outputBits);
        }
    }

    abstract protected void writeOutputsImpl(int bits);

    private class OnOffWriteImpl implements OnOffWrite<OnOffWriteImpl> {
        final int index;

        OnOffWriteImpl(int index) {
            this.index = index;
        }

        @Override
        public OnOffWriteImpl on() {
            AbstractConfigurableIoExpander.this.setOutputState(index, true);
            return this;
        }

        @Override
        public OnOffWriteImpl off()  {
            AbstractConfigurableIoExpander.this.setOutputState(index, false);
            return this;
        }
    }

}