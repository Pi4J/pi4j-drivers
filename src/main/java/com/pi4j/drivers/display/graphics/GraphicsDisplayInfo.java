package com.pi4j.drivers.display.graphics;

public class GraphicsDisplayInfo {

    private final int width;
    private final int height;
    private final PixelFormat pixelFormat;

    /** x-coordinates must be a multiple of this value when sending data to the driver. */
    private final int xGranularity;
    /** Drivers might implicitly rotate the screen to simplify rendering data from the graphics display. */
    private final GraphicsDisplay.Rotation implicitRotation;

    private static int granularityForBits(int bitCount) {
        int xGranularity = 1;
        while ((xGranularity * bitCount) % 8 != 0) {
            xGranularity *= 2;
        }
        return xGranularity;
    }

    public GraphicsDisplayInfo(int width, int height, PixelFormat pixelFormat, int xGranularity, GraphicsDisplay.Rotation rotation) {
        this.width = width;
        this.height = height;
        this.pixelFormat = pixelFormat;
        this.xGranularity = xGranularity;
        this.implicitRotation = rotation;
    }

    public GraphicsDisplayInfo(int width, int height, PixelFormat pixelFormat) {
        this(width, height, pixelFormat, granularityForBits(pixelFormat.getBitCount()), GraphicsDisplay.Rotation.ROTATE_0);
    }

        /** The width of the display in pixel. */
    public int getWidth() {
        return width;
    }

    /** The height of the display in pixel. */
    public int getHeight() {
        return height;
    }

    public PixelFormat getPixelFormat() {
        return pixelFormat;
    }

    /** x-coordinates must be a multiple of this value when sending data to the driver. */
    public int getXGranularity() {
        return xGranularity;
    }

    /**
     * Implicit rotation of the screen relative to the default rotation of the device. This will be subtracted
     * from rotations requested in the graphics display in order to normalize the roation.
     */
    public GraphicsDisplay.Rotation getImplicitRotation() {
        return implicitRotation;
    }
}
