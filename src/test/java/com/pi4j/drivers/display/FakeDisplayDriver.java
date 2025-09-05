package com.pi4j.drivers.display;

import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeDisplayDriver implements GraphicsDisplayDriver {

    private static Logger log = LoggerFactory.getLogger(FakeDisplayDriver.class);

    private byte[] data;
    private DisplayInfo displayInfo;

    public FakeDisplayDriver(DisplayInfo displayInfo) {
        this.displayInfo = displayInfo;
        this.data = new byte[(displayInfo.getWidth() * displayInfo.getHeight() * displayInfo.getPixelFormat().getBitCount() + 7) / 8];
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public DisplayInfo getDisplayInfo() {
        return displayInfo;
    }

    @Override
    public void setPixels(int x, int y, int width, int height, byte[] data) {
        log.trace("setPixels: {} {} {} {}", x, y, width, height);
        log.trace("\t" + HexFormat.of().formatHex(data));

        PixelFormat pixelFormat = displayInfo.getPixelFormat();

        if (x * pixelFormat.getBitCount() % 8 != 0) {
            throw new IllegalArgumentException("misaligned x address");
        }

        for (int i = 0; i < height; i++) {
            System.arraycopy(
                    data, (i * width * pixelFormat.getBitCount() + 7) / 8,
                    this.data, ((i + y) * getDisplayInfo().getWidth() * pixelFormat.getBitCount() + x + 7) / 8,
                    (width * pixelFormat.getBitCount() + 7) / 8);
        }
    }
}
