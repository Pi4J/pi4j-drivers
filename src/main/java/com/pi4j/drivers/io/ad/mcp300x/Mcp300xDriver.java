package com.pi4j.drivers.io.ad.mcp300x;

import com.pi4j.io.spi.Spi;

/**
 * A simple driver for the MCP3004/8 analog to digital converter.
 */
public class Mcp300xDriver {

    private final Spi spi;
    private final byte[] buffer = new byte[3];

    public Mcp300xDriver(Spi spi) {
        this.spi = spi;
    }

    public int readChannel(int index) {
        return readChannel(index, false);
    }

    public int readDifferentialChannel(int index) {
        return readChannel(index, true);
    }

    private int readChannel(int index, boolean differential) {
        buffer[0] = 1;
        buffer[1] = (byte) (differential ? 0 : (1 << 7) | (index << 4));

        spi.transfer(buffer, buffer);

        return ((buffer[1] & 0x07) << 8) | (buffer[2] & 0xff);
    }
}
