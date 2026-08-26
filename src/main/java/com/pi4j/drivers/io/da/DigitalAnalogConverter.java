package com.pi4j.drivers.io.da;

public interface DigitalAnalogConverter {

    /** Convenience method setting the voltage for the first channel (index 0). */
    default void setVoltage(double value) {
        setVoltage(0, value);
    }

    /**
     * Sets the output voltage for the given channel. The available range typically depends on the chip, the reference
     * voltage and the gain.
     */
    void setVoltage(int outputChannel, double value);

    /** Returns the number of channels provided by this DAC. */
    int getChannelCount();
}
