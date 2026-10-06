package com.pi4j.drivers.display.graphics.zigzag;

import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.GraphicsDisplayInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZigZagDriver implements GraphicsDisplayDriver {

    private static Logger log = LoggerFactory.getLogger(ZigZagDriver.class);

    private final Address[][] mapping;
    private final GraphicsDisplayInfo displayInfo;
    private final Hardware hardware;

    public ZigZagDriver(GraphicsDisplayInfo displayInfo, Address[][] mapping, Hardware hardware) {
        this.displayInfo = displayInfo;
        this.mapping = mapping;
        this.hardware = hardware;
    }

    public void setPixel(int x, int y, int color) {
        Address to = mapping[x][y];
        log.debug("setPixel {} {} {} -> Port:{} Pixel:{}", x, y, String.format("0x%08x ", color), to.port, to.pixel);

        hardware.setPixel(to, color);
    }

    @Override
    public GraphicsDisplayInfo getDisplayInfo() {
        return displayInfo;
    }

    @Override
    public void setPixels(int x, int y, int width, int height, byte[] data) {
        log.debug("setPixels {} {} {} {} {}", x, y, width, height, data.length);

        int bitIndex = 0;
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int r = data[bitIndex++] & 0xFF;
                int g = data[bitIndex++] & 0xFF;
                int b = data[bitIndex++] & 0xFF;

                int color = (r << 16) | (g << 8) | b;

                setPixel(x + px, y + py, color);
            }
        }

        hardware.sendBuffer();

    }

    @Override
    public void close() {

        hardware.close();
    }
}
