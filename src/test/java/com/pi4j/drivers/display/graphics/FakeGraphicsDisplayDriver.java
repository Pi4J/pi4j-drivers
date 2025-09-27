package com.pi4j.drivers.display.graphics;

import java.io.IOException;

public class FakeGraphicsDisplayDriver implements GraphicsDisplayDriver {

    private final byte[] data;
    private final GraphicsDisplayInfo displayInfo;

    public FakeGraphicsDisplayDriver(int width, int height, PixelFormat pixelFormat) {
        this.displayInfo = new GraphicsDisplayInfo(width, height, pixelFormat);
        this.data = new byte[(displayInfo.getWidth() * displayInfo.getHeight()
                * displayInfo.getPixelFormat().getBitCount() + 7) / 8];
        checkAlignment(width, "Display width");
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public GraphicsDisplayInfo getDisplayInfo() {
        return displayInfo;
    }

    @Override
    public void setPixels(int x, int y, int width, int height, byte[] data) {
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

    @Override
    public void close() {
    }

    private void checkAlignment(int x, String target) {
        if ((x * displayInfo.getPixelFormat().getBitCount()) % 8 != 0) {
            throw new IllegalArgumentException("misaligned for " + target + " -- must be aligned on byte address");
        }
    }
}
