package com.pi4j.drivers.display.graphics;

import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeDisplayDriver implements DisplayDriver {

    private static Logger log = LoggerFactory.getLogger(FakeDisplayDriver.class);

    private byte[] data;
    private DisplayInfo displayInfo;

    public FakeDisplayDriver(DisplayInfo displayInfo) {
        this.displayInfo = displayInfo;
        this.data = new byte[(displayInfo.getWidth() * displayInfo.getHeight()
                * displayInfo.getPixelFormat().getBitCount() + 7) / 8];
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

        if (x < 0 || x + width > displayInfo.getWidth()) {
            throw new IllegalArgumentException(
                    "x " + x + " + width " + width + " exceeds display width " + displayInfo.getWidth());
        }
        if (y < 0 || y + height > displayInfo.getHeight()) {
            throw new IllegalArgumentException(
                    "y " + y + " + height " + height + " exceeds display height " + displayInfo.getHeight());
        }

        PixelFormat pixelFormat = displayInfo.getPixelFormat();

        checkAlignment(x, "x-position");
        checkAlignment(width, "width");

        for (int i = 0; i < height; i++) {
            int srcPos = (i * width * pixelFormat.getBitCount() + 7) / 8;
            int dstPos = (((i + y) * getDisplayInfo().getWidth() + x) * pixelFormat.getBitCount() + 7) / 8;
            int count = (width * pixelFormat.getBitCount() + 7) / 8;
            System.arraycopy(data, srcPos, this.data, dstPos, count);
        }
    }

    private void checkAlignment(int x, String target) {
        if (x * displayInfo.getPixelFormat().getBitCount() % 8 != 0) {
            throw new IllegalArgumentException("misaligned for " + target + " -- must be aligned on byte address");
        }
    }
}
