package com.pi4j.drivers.io.da.mcp492x;

import com.pi4j.io.spi.Spi;

public class Mcp4922 {

    Spi spi;

    final int ABMask = 0b1000_0000_0000_0000;
    final int bufferedMask = 0b0100_0000_0000_0000;
    final int ga2xMask = 0b0010_0000_0000_0000;
    final int shdnMask = 0b0001_0000_0000_0000;

    public Mcp4922(Spi spi) {
        super();
        this.spi = spi;
    }


    public void writeTwelve(int twelveBit, boolean AB, boolean buffered, boolean ga2x, boolean shdn) {

        byte[] spiData = new byte[2];
        twelveBit = ((AB) ? (twelveBit | ABMask) : (twelveBit & ~ABMask));
        twelveBit = ((buffered) ? (twelveBit | bufferedMask) : (twelveBit & ~bufferedMask));
        twelveBit = ((ga2x) ? (twelveBit | ga2xMask) : (twelveBit & ~ga2xMask));
        twelveBit = ((shdn) ? (twelveBit | shdnMask) : (twelveBit & ~shdnMask));

        spiData[0] = (byte) (spiData[0] | (byte) ((twelveBit & 0xFF00) >> 8));
        spiData[1] = (byte) (twelveBit & 0x00FF);
        this.spi.write(spiData);

    }

    public void writeTwelvePerVoltage(double vout, double vref, boolean AB, boolean buffered, boolean ga2x, boolean shdn) {
        int gain = (ga2x) ? 1 : 2;
        int twelveBit = (int) ((vout * 4096) / (gain * vref));
        writeTwelve(twelveBit, AB, buffered, ga2x, shdn);
    }

}
