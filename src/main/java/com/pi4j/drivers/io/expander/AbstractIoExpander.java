package com.pi4j.drivers.io.expander;

import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.OnOffWrite;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Abstract base class for IO expander drivers. Implementations should set a defined state for inputStates, outputStates
 * and inputDirectionBits in the constructor and implement the 3 abstract "impl" methods to read data, write data and
 * configure the pin directions.
 */
public abstract class AbstractIoExpander implements ConfigurableIoExpander, Closeable {
    private final int size;
    private final ListenableOnOffRead<?> interruptPin;
    private final ListenableOnOffRead.Impl[] onOffReadArray;
    private final OnOffWrite<?>[] onOffWriteArray;
    private final List<IntConsumer> inputStateListeners = new ArrayList<>();

    // Mask bits from triggering an output write.
    private int triggerMask = -1;

    // These three fields shadow the internal state of the IO expander to avoid unnecessary transfers.
    protected int inputDirectionBits;
    protected int inputStates;
    protected int outputStates;

    protected AbstractIoExpander(int size, ListenableOnOffRead<?> interruptPin) {
        this.size = size;
        this.interruptPin = interruptPin;
        this.onOffReadArray = new ListenableOnOffRead.Impl[size];
        this.onOffWriteArray = new OnOffWrite[size];
        for (int i = 0; i < size; i++) {
            onOffReadArray[i] = new ListenableOnOffRead.Impl();
            onOffWriteArray[i] = new OnOffWriteImpl(i);
        }
        if (interruptPin != null) {
            interruptPin.addConsumer(value -> {
                // We poll on any flank to be sure not to miss anything...
                poll();
            });
        }
    }

    @Override
    public final void addInputStateListener(IntConsumer listener) {
        inputStateListeners.add(listener);
    }

    @Override
    public void close() {
        if (interruptPin instanceof Closeable) {
            try {
                ((Closeable) interruptPin).close();
            } catch (IOException e) {
                throw new com.pi4j.io.exception.IOException(e);
            }
        }
    }

    @Override
    public final ListenableOnOffRead<ListenableOnOffRead.Impl> getInput(int index) {
        return onOffReadArray[index];
    }

    @Override
    public final int getInputStates() {
        return interruptPin == null ? poll() : inputStates;
    }

    @Override
    public OnOffWrite<?> getOutput(int index) {
        return onOffWriteArray[index];
    }

    @Override
    public final int getSize() {
        return size;
    }

    @Override
    public final int poll() {
        int newState = readInputsImpl();
        if (newState != inputStates) {
            this.inputStates = newState;
            for (int i = 0; i < size; i++) {
                onOffReadArray[i].setState((newState & (1 << i)) != 0);
            }
            inputStateListeners.forEach(listener -> listener.accept(newState));
        }
        return newState;
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
    public void setOutputTriggerMask(int mask) {
        this.triggerMask = mask;
    }

    //
    // Abstract methods to be implemented by IO expander drivers -------------------------------------------------------
    //

    /**
     * Implementations should need to only override onyl this method.
     */
    abstract protected int readInputsImpl();
    /**
     * Subclasses are supposed to implement this, setting pins to input mode for bits set in inputPins and to
     * output mode otherwise.
     */
    abstract protected void setIoDirectionsImpl(int inputPins);

    abstract protected void writeOutputsImpl(int bits);

    //
    // Inner helper class ----------------------------------------------------------------------------------------------
    //

    private class OnOffWriteImpl implements OnOffWrite<OnOffWriteImpl> {
        final int index;

        OnOffWriteImpl(int index) {
            this.index = index;
        }

        @Override
        public OnOffWriteImpl on() {
            AbstractIoExpander.this.setOutputState(index, true);
            return this;
        }

        @Override
        public OnOffWriteImpl off()  {
            AbstractIoExpander.this.setOutputState(index, false);
            return this;
        }
    }
}