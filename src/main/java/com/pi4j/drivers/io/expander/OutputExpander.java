package com.pi4j.drivers.io.expander;

import com.pi4j.io.OnOffWrite;

/**
 * Models an output expander. Typically, IO expanders support input and output. This interface only covers the output
 * aspects and can be used to isolate code from configuration options.
 */
public interface OutputExpander {

    /**
     * Returns an OnOffWrite object (implementing the base interface of DigitalOutput) for the output pin with the
     * given index.
     */
    OnOffWrite<?> getOutput(int index);

    /** Sets the state for the output pin with the given index. */
    default void setOutputState(int index, boolean state) {
        setOutputStates(1 << index, state);
    }

    /**
     * @deprecated Replaced with setOutputStates for clarity.
     */
    @Deprecated
    default void setOutputState(int bits) {
        setOutputStates(bits);
    }

    /**
     * Sets all pins at once, mapping each bit to the corresponding pin number.
     */
    void setOutputStates(int bits);

    /**
     * Sets all the masked pins to the given new state.
     */
    void setOutputStates(int mask, boolean newState);

    /**
     * Sets a mask for which bit changes trigger sending the changed state over i2c. By default,
     * the mask is -1 and all bit changes trigger an update. This can be useful when
     */
    void setOutputTriggerMask(int mask);
}
