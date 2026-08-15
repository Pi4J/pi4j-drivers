package com.pi4j.drivers.io.da.mcp49xx;

import com.pi4j.drivers.io.da.DigitalAnalogConverter;
import com.pi4j.io.spi.Spi;

/**
 *  Driver for MCP49x1 single channel DAC chips with external reference voltages.
 *
 *  A single driver class is sufficient for MCP4901, MCP4911 and MCP4921 as the lower precision models just drop the
 *  lower bits.
 */
public class Mcp49x1Driver implements DigitalAnalogConverter {
    private final Spi spi;
    private final int maxValue;
    private final byte[] spiData = new byte[2];
    private final double referenceVoltage;
    private int flags = 0;

    public Mcp49x1Driver(Spi spi, int bitResolution, double referenceVoltage) {
        this.spi = spi;
        maxValue = (1 << bitResolution) - 1;
        this.referenceVoltage = referenceVoltage;
    }

    @Override
    public int getChannelCount() {
        return 1;
    }

    /**
     * Sets 2x gain according to the enable parameter, expanding the voltage range available in setVoltage()
     * accordingly. Note that this will only take effect when then next voltage is written.
     */
    public void set2xGain(boolean enable) {
        setFlag(Constants.GAIN_2X_MASK, !enable);
    }

    /**
     * Sets the buffered mode according to the enable flag. Note that this will only take
     * effect when then next voltage is written.
     */
    public void setBuffered(boolean enable) {
        setFlag(Constants.BUFFERED_MASK, enable);
    }

    /** Powers down the chip. The chip is automatically re-enabled when a voltage is set. */
    public void shutdown(int channel) {
        writeWord(0);
    }

    /** Calculates the digital output value based on the reference voltage and gain and writes it to the chip */
    @Override
    public void setVoltage(int channel, double value) {
        int gainFactor = (flags & Constants.GAIN_2X_MASK) != 0 ? 1 : 2;
        setDigitalValue(channel, (int) (value * maxValue / (gainFactor * referenceVoltage)));
    }

    /** Writes the n-bit (depending on the chip) digital value and all pending settings directly. */
    public void setDigitalValue(int channel, int value) {
        if (value < 0 || value > maxValue) {
            throw new IllegalArgumentException("Digitized value out of range (0.." + maxValue + "): " + value);
        }
        writeWord(Constants.SHUTDOWN_MASK | flags | value);
    }

    // Private helpers -------------------------------------------------------------------------------------------------

    private void writeWord(int value) {
        spiData[0] = (byte) (value >> 8);
        spiData[1] = (byte) value;
        spi.write(spiData, 0, 2);
    }

    private void setFlag(int mask, boolean enable) {
        if (enable) {
            flags |= mask;
        } else {
            flags &= ~mask;
        }
    }
}
