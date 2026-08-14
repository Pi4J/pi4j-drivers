package com.pi4j.drivers.io.da.mcp492x;

import com.pi4j.io.spi.Spi;


/**
 * Driver class for MCP4921 DAC chip.
 *
 *  @see <a href="https://ww1.microchip.com/downloads/en/DeviceDoc/22248a.pdf">MCP4921 Datasheet</a>
 *
 */
public class Mcp4921 extends Mcp4922 {
    public Mcp4921(Spi spi) {
        super(spi);
    }

    /**
     *                 12 bits
     * A/B BUF GA SHDN D11 D10 D9 D8 D7 D6 D5 D4 D3 D2 D1 D0
     *
     * @param twelveBit

     * @param buffered  HW specific operation
     * @param ga2x      true 2x operation else 1x
     * @param shdn      true chip is operational
     */
    public void writeTwelve(int twelveBit,  boolean buffered, boolean ga2x, boolean shdn){
        writeTwelve(twelveBit, false, buffered, ga2x, shdn);
    }

    /**
     * Calculate DAC value to accomplish requested vout
     * @param vout      float requested output voltage
     * @param vref      float reference voltage
     * @param buffered  HW specific operation
     * @param ga2x      true 2x operation else 1x
     * @param shdn      true chip is operational
     */
    public void writeTwelvePerVoltage(double vout, double vref, boolean buffered, boolean ga2x, boolean shdn) {
        int twelveBit = (int) ( (vout * 4095) / vref);
        writeTwelve(twelveBit, buffered, ga2x, shdn);

    }
}
