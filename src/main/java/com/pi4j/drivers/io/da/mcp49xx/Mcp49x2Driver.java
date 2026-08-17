package com.pi4j.drivers.io.da.mcp49xx;

import com.pi4j.drivers.io.da.DigitalAnalogConverter;
import com.pi4j.io.spi.Spi;


/**
 *  Driver class for MCP49x2 two channel DAC chips with external reference voltages.
 *
 *  A single driver class is sufficient for MCP4902 (8 bit), MCP4912 (10 bit) and MCP4922 (12 bit) as the lower
 *  precision models just drop the lower bits.
 *
 *  @see <a href="https://www.mouser.com/datasheet/2/268/22250A-14454.pdf">MCP4922 Datasheet</a>
 */

 public class Mcp49x2Driver implements DigitalAnalogConverter {
    private final Spi spi;
    private final int[] flags = new int[2];
    private final byte[] spiData = new byte[2];
    private final double[] referenceVoltages = new double[2];

    public Mcp49x2Driver(Spi spi, double referenceVoltage0, double referenceVoltage1) {
        this.spi = spi;
        referenceVoltages[0] = referenceVoltage0;
        referenceVoltages[1] = referenceVoltage1;
    }

    public Mcp49x2Driver(Spi spi, double referenceVoltage) {
        this(spi, referenceVoltage, referenceVoltage);
    }


    @Override
    public int getChannelCount() {
        return 2;
    }

    /**
     * Sets 2x gain according to the enable parameter, expanding the voltage range available in setVoltage()
     * accordingly. Note that this will only take effect when then next voltage is written.
     */
    public void set2xGain(int channel, boolean enable) {
        setFlag(channel, Constants.GAIN_2X_MASK, !enable);
    }

    /**
     * Sets the given channel to buffered mode according to the enable flag. Note that this will only take
     * effect when then next voltage is written.
     */
    public void setBuffered(int channel, boolean enable) {
        setFlag(channel, Constants.BUFFERED_MASK, enable);
    }

    /** Powers down the given channel (0 or 1). The channel is automatically re-enabled when a voltage is set. */
    public void shutdown(int channel) {
        checkChannelRange(channel);
        writeWord(channel * Constants.AB_MASK);
    }

    /** Calculates the digital output value based on the reference voltage and gain and writes it to the chip */
    @Override
    public void setVoltage(int channel, double value) {
        checkChannelRange(channel);
        int gainFactor = (flags[channel] & Constants.GAIN_2X_MASK) != 0 ? 1 : 2;
        setDigitalValue(channel, (int) (value * Constants.MAX_VALUE / (gainFactor * referenceVoltages[channel])));
    }

    /**
     * Writes the 12-bit digital value (0..4095) and all pending settings directly.
     * For lower resolution chips, the lower bits are ignored.
     */
    public void setDigitalValue(int channel, int value) {
        checkChannelRange(channel);
        if (value < 0 || value > Constants.MAX_VALUE) {
            throw new IllegalArgumentException("Digitized value out of range (0.." + Constants.MAX_VALUE + "): " + value);
        }
        writeWord( Constants.SHUTDOWN_MASK | (channel * Constants.AB_MASK) | flags[channel] | value);
    }

    // Private helpers -------------------------------------------------------------------------------------------------

    private void checkChannelRange(int channel) {
        if (channel < 0 || channel > 1) {
            throw new IllegalArgumentException("Channel must be 0 or 1; was: " + channel);
        }
    }

    private void writeWord(int value) {
        spiData[0] = (byte) (value >> 8);
        spiData[1] = (byte) value;
        spi.write(spiData, 0, 2);
    }

    private void setFlag(int channel, int mask, boolean enable) {
        checkChannelRange(channel);
        if (enable) {
            flags[channel] |= mask;
        } else {
            flags[channel] &= ~mask;
        }
    }
}
