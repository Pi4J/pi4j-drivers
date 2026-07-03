package com.pi4j.drivers.io.expander;

import com.pi4j.io.ListenableOnOffRead;

import java.util.function.IntConsumer;

/**
 * Models an input expander. Typically, IO expanders support input and output. This interface only covers the input
 * aspects and can be used to isolate code from configuration options.
 */
public interface InputExpander {
    /** Note that this will only trigger automatically if an interrupt pin is connected */
    void addInputStateListener(IntConsumer listener);

    /** Returns a listenable OnOffRead object for the given pin. */
    ListenableOnOffRead<ListenableOnOffRead.Impl> getInput(int index);

    /**
     * @deprecated Replaced with getInputStates();
     */
    @Deprecated
    default int getInputState() {
        return getInputStates();
    }

    /**
     * Returns the input state of all inputs encode in an integer (for each input that is on, the corresponding bit is
     * set. Note that this returns the current internal state and does not query the chip. If no interrupt line
     * is set up, please use poll() to request an updated state from the chip.
     */
    int getInputStates();

    /**
     * Returns the input state of all a single pin. Note that if no interrupt pin is connected, this value will
     * not update automatically. Call poll() to refresh the state in this case.
     */
    default boolean getInputState(int pinIndex) {
        return (getInputStates() & (1 << pinIndex)) != 0;
    }

    /**
     * Reads the current state from the chip, updates the internal state and notifies all listeners. The returned
     * value matches the getInputStates format.
     */
    int poll();

    /** Returns the number of pins on this chip. */
    int getSize();

}
