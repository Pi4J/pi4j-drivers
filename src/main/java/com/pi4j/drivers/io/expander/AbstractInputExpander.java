package com.pi4j.drivers.io.expander;

import com.pi4j.io.ListenableOnOffRead;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * An abstract base implementation of an input expander.
 */
public abstract class AbstractInputExpander implements InputExpander, Closeable {
    private final ListenableOnOffRead.Impl[] inputs;
    private final ListenableOnOffRead<?> interruptPin;
    private final List<IntConsumer> inputStateListeners = new ArrayList<>();
    private final int size;

    protected int inputStates;

    protected AbstractInputExpander(int size, ListenableOnOffRead<?> interruptPin) {
        this.size = size;
        this.inputs = new ListenableOnOffRead.Impl[size];
        this.interruptPin = interruptPin;
        for (int i = 0; i < size; i++) {
            inputs[i] = new ListenableOnOffRead.Impl();
        }
        if (interruptPin != null) {
            interruptPin.addConsumer(value -> {
                // We poll on any flank to be sure not to miss anything...
                poll();
            });
        }
    }

    public final int getSize() {
        return size;
    }

    /**
     * Adds a listener that will be notified on a state change on any of the pins
     */
    @Override
    public final void addInputStateListener(IntConsumer listener) {
        inputStateListeners.add(listener);
    }


    @Override
    public final ListenableOnOffRead<ListenableOnOffRead.Impl> getInput(int index) {
        return inputs[index];
    }


    @Override
    public final int getInputStates() {
        return inputStates;
    }


    @Override
    public final int poll() {
        int newState = readInputsImpl();
        if (newState != inputStates) {
            this.inputStates = newState;
            for (int i = 0; i < size; i++) {
                inputs[i].setState((newState & (1 << i)) != 0);
            }
            inputStateListeners.forEach(listener -> listener.accept(newState));
        }
        return newState;
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

    /**
     * Implementations should need to only override onyl this method.
     */
    abstract protected int readInputsImpl();

}