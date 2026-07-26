package com.pi4j.drivers.io.ad.ads111x;

import com.pi4j.io.i2c.I2C;

/** Extends the base chip model with support for multiplexing. */
public class Ads1115Driver extends Ads1114Driver {
    public Ads1115Driver(I2C i2c) {
        super(i2c);
    }

    public Multiplexer getMultiplexer() {
        int index = readConfigBits(Constants.MUX_OFFSET, Constants.MUX_COUNT);
        return Multiplexer.values()[index];
    }

    public void setMultiplexer(Multiplexer multiplexer) {
        writeConfigBits(Constants.MUX_OFFSET, Constants.MUX_COUNT, multiplexer.ordinal());
    }
}
