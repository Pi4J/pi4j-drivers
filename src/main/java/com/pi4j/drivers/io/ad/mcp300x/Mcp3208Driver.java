package com.pi4j.drivers.io.ad.mcp300x;

import com.pi4j.io.spi.Spi;

public class Mcp3208Driver extends Mcp300xDriver{

    public Mcp3208Driver(Spi spi) {
        super( spi) ;
    }


    @Override
    protected int readChannel(int index, boolean differential) {
        buffer[0] = 1;
        buffer[1] = (byte) (differential ? 0 : (1 << 7) | (index << 4));

        spi.transfer(buffer, buffer);

        return ((buffer[1] & 0x0F) << 8) | (buffer[2] & 0xff);
    }

}
