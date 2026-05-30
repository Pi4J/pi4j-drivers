package com.pi4j.drivers.io.expander;

import com.pi4j.io.ListenableOnOffRead;

import java.util.function.IntConsumer;

public interface InputExpander {
    /** Note that this will only trigger automatically if an interrupt pin is connected */
    void addInputStateListener(IntConsumer listener);

    /** Returns a listenable OnOffRead object for the given pin. */
    ListenableOnOffRead<ListenableOnOffRead.Impl> getInput(int index);

    /**
     * Returns the input state of all inputs encode in an integer (for each input that is on, the corresponding bit is
     * set. Note that this returns the current internal state and does not query the chip. If no interrupt line
     * is set up, please use poll() to request an updated state from the chip. $
     */
    int getInputState();

    /**
     * Reads the current state from the chip, updates the internal state and notifies all listeners.
     */
    int poll();
}
