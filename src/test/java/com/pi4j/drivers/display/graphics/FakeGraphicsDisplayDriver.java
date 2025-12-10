package com.pi4j.drivers.display.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    public void assertPixel(int x, int y, int expectedColor) {
        if (getDisplayInfo().getPixelFormat() != PixelFormat.RGB_888) {
            throw new RuntimeException("AssertPixel is only supported for RGB_888");
        }
        int expectedBits = getDisplayInfo().getPixelFormat().fromRgb(expectedColor);
        int bitAddress = (y * displayInfo.getWidth() + x) * getDisplayInfo().getPixelFormat().getBitCount();
        int address = bitAddress / 8;
        int actualBits = ((data[address] & 0xff) << 16) | ((data[address+1] & 0xff) << 8) | (data[address+2] & 0xff);
        assertEquals(Integer.toHexString(expectedBits), Integer.toHexString(actualBits), "At pixel position "+ x + ", " + y);
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
