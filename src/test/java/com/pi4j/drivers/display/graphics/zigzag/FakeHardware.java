package com.pi4j.drivers.display.graphics.zigzag;

import java.util.HashMap;

public class FakeHardware implements Hardware {
    private HashMap<Integer, Integer> values = new HashMap<>();

    public HashMap<Integer, Integer> getPixels() {
        return values;
    }

    @Override
    public void sendBuffer() {
    }

    @Override
    public void setPixel(Address addr, int color) {
        values.put(addr.pixel, color);
    }

    @Override
    public void close() {
    }
}
