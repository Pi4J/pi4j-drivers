package com.pi4j.drivers.io.ad.mcp300x;

import com.pi4j.io.spi.Spi;

/**
 * A simple driver for the MCP3004/8 analog to digital converter.
 */
public abstract class Mcp300xDriver {

    protected final Spi spi;
    protected final byte[] buffer = new byte[3];

    public Mcp300xDriver(Spi spi) {
        this.spi = spi;
    }

    public int readChannel(int index) {
        return readChannel(index, false);
    }

    public int readDifferentialChannel(int index) {
        return readChannel(index, true);
    }

    protected abstract int readChannel(int index, boolean differential);
}
