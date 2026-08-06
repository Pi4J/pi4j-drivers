package com.pi4j.drivers.io.da.mcp492x;

import com.pi4j.io.spi.Spi;


public class Mcp4921 extends Mcp4922 {
    public Mcp4921(Spi spi) {
        super(spi);
    }

    public void writeTwelve(int twelveBit,  boolean buffered, boolean ga2x, boolean shdn){
        super.writeTwelve(twelveBit, false, buffered, ga2x, shdn);
    }
    public void writeTwelvePerVoltage(double vout, double vref, boolean buffered, boolean ga2x, boolean shdn) {
        int gain = (ga2x) ? 2 : 1;
        int twelveBit = Math.toIntExact(Math.round(vout * 4096 / gain * vref));
        writeTwelve(twelveBit, buffered, ga2x, shdn);

    }
}
