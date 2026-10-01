package com.pi4j.drivers.display.graphics;

/**
 * An abstract base class for graphics display drivers that require some kind of complex coordinate mapping.
 *
 * For instance, in WS281x based matrix displays, the LEDs are typically arranged in a zig-zag or spiral pattern.
 */
public abstract class AbstractMappedGraphicsDisplayDriver implements GraphicsDisplayDriver {

    private final GraphicsDisplayDescriptor descriptor;
    protected final CoordinateMappingFunction mappingFunction;

    protected AbstractMappedGraphicsDisplayDriver(int width, int height, CoordinateMappingFunction mappingFunction) {
        this.descriptor = new GraphicsDisplayDescriptor(width, height, PixelFormat.RGB_888);
        this.mappingFunction = mappingFunction;
    }

    @Override
    public GraphicsDisplayDescriptor getDisplayInfo() {
        return descriptor;
    }

    @Override
    public void setPixels(int x0, int y0, int width, int height, byte[] data) {
        int sourceIndex = 0;
        for (int y = y0; y < y0 + height; y++) {
            for (int x = x0; x < x0 + width; x++) {
                int rgb = (data[sourceIndex] & 0xFF) << 16
                       | (data[sourceIndex + 1] & 0xFF) << 8
                       | (data[sourceIndex + 2] & 0xFF) << 16;
                int destinationIndex = mappingFunction.mapCoordinate(x, y);
                setPixel(destinationIndex, rgb);
                sourceIndex += 3;
            }
        }
    }

    /** Set the pixel at the translated index to the given color. */
    protected abstract void setPixel(int index, int rgb);

    /** Maps pixel coordinates to an index in a transfer buffer */
    interface CoordinateMappingFunction {
        int mapCoordinate(int x, int y);
    }

    // We should add some concrete mapping function implementations such as Zigzag here...
}
