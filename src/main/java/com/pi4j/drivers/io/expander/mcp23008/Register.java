package com.pi4j.drivers.io.expander.mcp23008;

class Register {
    static final int IODIR = 0;
    static final int IPOL = 1;
    static final int GPINTEN = 2;
    static final int DEFVAL = 3;
    static final int INTCON = 4;
    static final int IOCON = 5;
    static final int GPPU = 6;
    static final int INTF = 7;
    static final int INTCAP = 8; // Read-only)
    static final int GPIO = 9;
    static final int OLAT = 0xa;
}
