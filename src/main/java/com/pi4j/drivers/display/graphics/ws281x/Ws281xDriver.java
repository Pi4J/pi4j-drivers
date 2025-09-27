package com.pi4j.drivers.display.graphics.ws281x;

import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.GraphicsDisplayInfo;
import com.pi4j.drivers.display.graphics.PixelFormat;
import com.pi4j.io.spi.Spi;

/**
 * Implements a driver for WS 281x LED strips using a SPI interface. Note that the baud rate of the
 * SPI channel needs to be set to SPI_BAUD.
 * <p>
 * This driver is based on timing information form this article:
 * https://wp.josh.com/2014/05/13/ws2812-neopixels-are-not-so-finicky-once-you-get-to-know-them/
 */
public class Ws281xDriver implements GraphicsDisplayDriver {
    /** The number of color channels (r, g, b); each using one byte. */
    private static final int COLOR_CHANNELS = 3;
    /**
     * How many bits it takes to send a single bit via SPI. 4 Bit seem to be a reasonable compromise between
     * timing precision and keeping the amount of data and the frequency low. Finding a frequency that would work
     * for three bit seemed tricky, as the minimum "low" phase after the variable length bit pulse is significantly
     * longer than the 1-bit pulse width. Note that this constant is just here to avoid magical numbers --
     * changing the bit stretch will require mode code adjustments than just changing this constant.
     */
    private static final int BIT_STRETCH = 4;
    /** The baud rate the SPI channel needs to be configured to. */
    public static final int SPI_BAUD = 800_000 * BIT_STRETCH;

    /** A buffer of the transformed pixels in the format they will be sent over SPI */
    private final byte[] spiBuffer;
    private final GraphicsDisplayInfo displayInfo;
    private final Spi spi;
    private final boolean zigzag;

    /**
     * Creates a WS281x driver instance.
     * <p>
     * Note that the baud rate of the SPI channel handed in needs to configured to SPI_BAUD.
     * <p>
     * For two-dimensional LED strip arrangement, the driver assumes that LED index 0 is at coordinates (0,0)
     * and the start LED index of each subsequent row is at y * width. For a "zigzag" arrangement, use the
     * other constructor.
     */
    public Ws281xDriver(Spi spi, int width, int height) {
        this(spi, width, height, false);
    }

    /**
     * Creates a WS281x driver instance.
     * <p>
     * Note that the baud rate of the SPI channel handed in needs to configured to SPI_BAUD.
     * <p>
     * If the zigzag parameter is set to true, for two-dimensional LED strip arrangement, the driver assumes that LED
     * order reverses direction in each row, starting with index 0 is at coordinates (0,0).
     */
    public Ws281xDriver(Spi spi, int width, int height, boolean zigzag) {
        this.spi = spi;
        this.zigzag = zigzag;

        // We just check it's not the default to allow device-depending adjustments while still avoiding the
        // most simple footgun here.
        if (spi.config().getBaud() == Spi.DEFAULT_BAUD) {
            throw new IllegalArgumentException("Please set the baud rate to Ws281xDriver.SPI_BAUD");
        }

        spiBuffer = new byte[width * height * COLOR_CHANNELS * BIT_STRETCH];
        displayInfo = new GraphicsDisplayInfo(width, height, PixelFormat.RGB_888);
    }

    @Override
    public GraphicsDisplayInfo getDisplayInfo() {
        return displayInfo;
    }

    @Override
    public void setPixels(int x, int y, int width, int height, byte[] bytes) {
        int src = 0;
        int lastChangedByte = 0;
        for (int i = 0; i < height; i++) {
            int dy = y + i;
            int dst;
            int dstStride;
            if (zigzag && (dy & 1) == 1) {
                dst = ((dy + 1) * displayInfo.getWidth() - x - 1) * COLOR_CHANNELS * BIT_STRETCH;
                dstStride = -2 * COLOR_CHANNELS * BIT_STRETCH;
            } else {
                dst = (dy * displayInfo.getWidth() + x) * COLOR_CHANNELS * BIT_STRETCH;
                dstStride = 0;
            }
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < COLOR_CHANNELS; k++) {
                    byte b = bytes[src++];
                    for (int bitIdex = 0; bitIdex < 8; bitIdex += 2) {
                        byte newValue = (byte) ((((b << bitIdex) & 0x80) == 0 ? 0b1000_0000 : 0b1100_0000)
                                | (((b << bitIdex) & 0x40) == 0 ? 0b1000 : 0b1100));
                        if (newValue != spiBuffer[dst]) {
                            spiBuffer[dst] = newValue;
                            lastChangedByte = dst;
                        }
                        dst++;
                    }
                }
                dst += dstStride;
            }
        }
        // We always need to start at 0, but we only need to send up to the last changed pixel.
        // Dividing and then multiplying makes sure we always send full pixel data and don't stop somewhere in the
        // middle of a pixel.
        int lastChangedPixel = lastChangedByte / COLOR_CHANNELS / BIT_STRETCH;
        if (lastChangedPixel > 0) {
            spi.write(spiBuffer, 0, lastChangedPixel * COLOR_CHANNELS * BIT_STRETCH);
        }
    }

    @Override
    public void close() {
        spi.close();
    }
}
