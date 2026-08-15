package com.pi4j.drivers.io.da.mcp492x;

import com.pi4j.io.spi.Spi;

/**
 *  Driver class for MCP4922 DAC chip
 *
 *  @see <a href="https://www.mouser.com/datasheet/2/268/22250A-14454.pdf">MCP4922 Datasheet</a>
 */
public class Mcp4922 {

    protected static Spi spi;

    final int ABMask = 0b1000_0000_0000_0000;
    final int bufferedMask = 0b0100_0000_0000_0000;
    final int ga2xMask = 0b0010_0000_0000_0000;
    final int shdnMask = 0b0001_0000_0000_0000;

    public Mcp4922(Spi spi) {
        super();
        this.spi = spi;
    }


    /**
     *                 12 bits
     * A/B BUF GA SHDN D11 D10 D9 D8 D7 D6 D5 D4 D3 D2 D1 D0
     *
     * @param twelveBit
     * @param AB        True voutA, flse voutB
     * @param buffered  HW specific operation
     * @param ga2x      true 2x operation else 1x
     * @param shdn      true chip is operational
     */
    public void writeTwelve(int twelveBit, boolean AB, boolean buffered, boolean ga2x, boolean shdn) {
        byte[] spiData = new byte[2];
        twelveBit = ((AB) ? (twelveBit | ABMask) : (twelveBit & ~ABMask));
        twelveBit = ((buffered) ? (twelveBit | bufferedMask) : (twelveBit & ~bufferedMask));
        twelveBit = ((ga2x) ?  (twelveBit & ~ga2xMask) : (twelveBit | ga2xMask));
        twelveBit = ((shdn) ? (twelveBit | shdnMask) : (twelveBit & ~shdnMask));

        spiData[0] = (byte) (twelveBit >> 8);
        spiData[1] = (byte) (twelveBit);
        this.spi.write(spiData);

    }

    /**
     * Calculate DAC value to accomplish requested vout
     * @param vout      float requested output voltage
     * @param vref      float reference voltage
     * @param AB        True voutA, else voutB
     * @param buffered  HW specific operation
     * @param ga2x      true 2x operation else 1x
     * @param shdn      true chip is operational
     */
    public void writeTwelvePerVoltage(double vout, double vref, boolean AB, boolean buffered, boolean ga2x, boolean shdn) {
        int twelveBit = (int) ((vout * 4095) / vref);
        writeTwelve(twelveBit, AB, buffered, ga2x, shdn);
    }

}
