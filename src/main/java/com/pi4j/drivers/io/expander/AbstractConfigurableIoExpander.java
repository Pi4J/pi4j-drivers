package com.pi4j.drivers.io.expander;

import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.OnOffWrite;

public abstract class AbstractConfigurableIoExpander extends AbstractInputExpander implements ConfigurableIoExpander {

    private final OnOffWrite<?>[] onOffWriteArray;

    private int triggerMask = -1;

    /** Implementations are expected to obain the initial output and input state in the constructor. */
    protected int outputStates = -1;
    /** Implementations are expected to set this to the chip state in the constructor. */
    protected int inputDirectionBits = -1;

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
        setOutputState(state ? outputStates | mask : outputStates & ~mask);
    }

    @Override
    public void setOutputStates(int bits) {
        int changedBits = outputStates ^ bits;
        outputStates = bits;
        if ((changedBits & triggerMask) != 0) {
            writeOutputsImpl(outputStates);
        }
    }

    @Override
    public void setIoDirections(int pinMask, Direction direction) {
        int newInputDirectionBits = direction == Direction.INPUT ? inputDirectionBits | pinMask : inputDirectionBits &~ pinMask;
        int changedPins = newInputDirectionBits ^ inputDirectionBits;
        if (changedPins != 0) {
            setIoDirectionsImpl(newInputDirectionBits);
            if (direction == Direction.INPUT) {
                // We silently update the pins that were changed to input direction without triggering any events.
                inputStates = inputStates & ~changedPins | (readInputsImpl() & changedPins);
            } else {
                writeOutputsImpl(outputStates);
            }
            inputDirectionBits = newInputDirectionBits;
        }
    }

    /**
     * Subclasses are supposed to implement this, setting pins to input mode for bits set in inputPins and to
     * output mode otherwise.
     */
    abstract protected void setIoDirectionsImpl(int inputPins);

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