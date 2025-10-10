package com.pi4j.drivers.io.expander.pcf8574;

import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.i2c.I2C;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;


/**
 * Input driver for the Pcf8574.
 * <p>
 * As the input and output functionality of this chip uses separate addresses, it seemed most straightforward
 * to implement these as separate classes.
 */
public class Pcf8574InputDriver implements Closeable {
    private final I2C i2c;
    private final ListenableOnOffRead.Impl[] inputs = new ListenableOnOffRead.Impl[8];
    private final ListenableOnOffRead<?> interruptPin;
    private final List<IntConsumer> inputStateListeners = new ArrayList<>();

    private int inputState;

    /**
     * Creates a new PCF 8574 input driver. The interrupt pin will be used to trigger update
     * requests from the chip. If null, state changes can still be observed via the poll() method.
     */
    public Pcf8574InputDriver(I2C i2c, ListenableOnOffRead<?> interruptPin) {
        this.i2c = i2c;
        this.interruptPin = interruptPin;
        for (int i = 0; i < 8; i++) {
            inputs[i] = new ListenableOnOffRead.Impl();
        }
        if (interruptPin != null) {
            interruptPin.addConsumer(value -> {
                if (value) {
                    poll();
                }
            });
        }
    }

    /** Adds a listener that will be notified on a state change on any of the pins */
    public void addInputStateListener(IntConsumer listener) {
        inputStateListeners.add(listener);
    }

    /**
     * Returns a representation of the given input pin.
     */
    public ListenableOnOffRead<ListenableOnOffRead.Impl> getInputPin(int bitIndex) {
        return inputs[bitIndex];
    }

    /** Returns the current state of the pins encoded in an integer, as sent by the expander chip. */
    public int getInputState() {
        return inputState;
    }

    /**
     * Reads the current state from the chip, updates the internal state and notifies all listeners.
     */
    public int poll() {
        int newState = i2c.read();
        if (newState != inputState) {
            this.inputState = newState;
            for (int i = 0; i < 8; i++) {
                inputs[i].setState((newState & (1 << i)) != 0);
            }
            inputStateListeners.forEach(listener -> listener.accept(newState));
        }
        return newState;
    }

    @Override
    public void close() throws IOException {
        i2c.close();
        if (interruptPin instanceof Closeable) {
            try {
                ((Closeable) interruptPin).close();
            } catch (IOException e) {
                throw new com.pi4j.io.exception.IOException(e);
            }
        }
    }
}
