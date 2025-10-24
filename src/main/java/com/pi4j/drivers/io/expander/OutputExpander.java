package com.pi4j.drivers.io.expander;

import com.pi4j.io.OnOffWrite;

public interface OutputExpander {

    /** Returns an OnOffWrite object for the output pin with the given index. */
    OnOffWrite<?> getOutput(int index);

    /** Sets the state for the output pin with the given index. */
    void setOutputState(int index, boolean state);

    /**
     * Sets all pins at once, mapping each bit to the corresponding pin number.
     */
    void setOutputState(int bits);

    /**
     * Sets a mask for which bit changes trigger sending the changed state over i2c. By default,
     * the mask is -1 and all bit changes trigger an update. 
     */
    void setOutputTriggerMask(int mask);
}
