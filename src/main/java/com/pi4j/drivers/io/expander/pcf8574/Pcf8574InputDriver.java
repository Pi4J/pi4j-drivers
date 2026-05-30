package com.pi4j.drivers.io.expander.pcf8574;

import com.pi4j.drivers.io.expander.InputExpander;
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
public class Pcf8574InputDriver implements Closeable, InputExpander {
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
    @Override
    public void addInputStateListener(IntConsumer listener) {
        inputStateListeners.add(listener);
    }

    
    @Override
    public ListenableOnOffRead<ListenableOnOffRead.Impl> getInput(int index) {
        return inputs[index];
    }


    @Override
    public int getInputState() {
        return inputState;
    }


    @Override
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
