package com.pi4j.drivers.io.da.mcp49xx;

class Constants {
    static final int AB_MASK = 0b1000_0000_0000_0000;
    static final int BUFFERED_MASK = 0b0100_0000_0000_0000;
    static final int GAIN_2X_MASK = 0b0010_0000_0000_0000;  // Note that this value is inverted (0 = 2x Gain)
    static final int SHUTDOWN_MASK = 0b0001_0000_0000_0000;  // Note that this value is inverted (0 = shutdown)

    static final int MAX_VALUE = ((1 << 12) - 1);
}
