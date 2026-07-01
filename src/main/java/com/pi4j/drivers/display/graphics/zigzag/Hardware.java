package com.pi4j.drivers.display.graphics.zigzag;

public interface Hardware {

    public void sendBuffer();

    public void setPixel(Address addr, int color);

    public void close();
}
