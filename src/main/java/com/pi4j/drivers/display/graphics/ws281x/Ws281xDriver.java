package com.pi4j.drivers.display.graphics.ws281x;

import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.GraphicsDisplayInfo;
import com.pi4j.drivers.display.graphics.PixelFormat;
import com.pi4j.io.spi.Spi;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Implements a driver for WS 281x LED strips using a SPI interface. Note that the baud rate of the
 * SPI channel needs to be set to SPI_BAUD.
 * <p>
 * This driver is based on timing information form this article:
 * https://wp.josh.com/2014/05/13/ws2812-neopixels-are-not-so-finicky-once-you-get-to-know-them/
 */
public class Ws281xDriver implements GraphicsDisplayDriver {

    public enum Pattern {
        ROWS,
        ZIGZAG,
    }

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
    private final int[] pixelMap;

    private Instant busyUntil = Instant.now();
    private int brightness = 64;

    public static int[] createPixelMap(int width, int height, Pattern pattern) {
        int[] result = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                 result[y * width + x] = y * width + ((pattern == Pattern.ROWS || (y & 1) == 0) ? x : width - x - 1);
            }
        }
        return result;
    }

    /**
     * Creates a WS281x driver instance with a "rows" arrangement of pixels (see Pattern).
     * <p>
     * Note that the baud rate of the SPI channel handed in needs to configured to SPI_BAUD.
     */
    public Ws281xDriver(Spi spi, int width, int height) {
        this(spi, width, height, Pattern.ROWS);
    }

    /**
     * Creates a WS281x driver instance with the given pixel arrangement pattern.
     * <p>
     */
    public Ws281xDriver(Spi spi, int width, int height, Pattern pattern) {
        this(spi, width, height, createPixelMap(width, height, pattern));
    }

    /**
     * Creates a WS281x driver instance.
     * <p>
     * Note that the baud rate of the SPI channel handed in needs to configured to SPI_BAUD.
     * <p>
     * The pixelMap parameter maps a given pixel index (y * width + x) to the led index in the strip.
     */
    public Ws281xDriver(Spi spi, int width, int height, int[] pixelMap) {
        this.spi = spi;
        if (pixelMap.length != width * height) {
            throw new IllegalArgumentException("The pixel map size (" + pixelMap.length + ") must match the LED strip length (" + width * height + ")");
        }
        this.pixelMap = new int[pixelMap.length];
        System.arraycopy(pixelMap, 0, this.pixelMap, 0, pixelMap.length);

        // We just check it's not the default to allow device-depending adjustments while still avoiding the
        // most simple footgun here.
        if (spi.config().getBaud() == Spi.DEFAULT_BAUD) {
            throw new IllegalArgumentException("Please set the baud rate to Ws281xDriver.SPI_BAUD");
        }

        spiBuffer = new byte[width * height * COLOR_CHANNELS * BIT_STRETCH];
        displayInfo = new GraphicsDisplayInfo(width, height, PixelFormat.GRB_888);
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
            for (int j = 0; j < width; j++) {
                int dst = getPixelAddress(x + j, y + i);
                for (int k = 0; k < COLOR_CHANNELS; k++) {
                    int value = (bytes[src++] & 0xff) * brightness / 255;
                    for (int bitIdex = 0; bitIdex < 8; bitIdex += 2) {
                        byte newValue = (byte) ((((value << bitIdex) & 0x80) == 0 ? 0b1000_0000 : 0b1100_0000)
                                | (((value << bitIdex) & 0x40) == 0 ? 0b1000 : 0b1100));
                        if (newValue != spiBuffer[dst]) {
                            spiBuffer[dst] = newValue;
                            lastChangedByte = dst;
                        }
                        dst++;
                    }
                }
            }
        }
        // We always need to start at 0, but we only need to send up to the last changed pixel.
        // Dividing and then multiplying makes sure we always send full pixel data and don't stop somewhere in the
        // middle of a pixel.
        int lastChangedPixel = lastChangedByte / COLOR_CHANNELS / BIT_STRETCH;
        if (lastChangedPixel > 0) {
            materializeDelay();
            spi.write(spiBuffer, 0, lastChangedPixel * COLOR_CHANNELS * BIT_STRETCH);
            setDelayNanos(50000);
        }
    }

    /** Sets the brightness of the LED matrix to the given value between 0 and 255. The default value is 64. */
    public void setBrightness(int brightness) {
        this.brightness = Math.max(0, Math.min(brightness, 255));
    }

    @Override
    public void close() {
        spi.close();
    }


    // Private methods


    private int getPixelAddress(int x, int y) {
        int pixelIndex = pixelMap[y * displayInfo.getWidth() + x];
        return pixelIndex * COLOR_CHANNELS * BIT_STRETCH;
    }

    private void materializeDelay() {
        while (true) {
            long remaining = Instant.now().until(busyUntil, ChronoUnit.NANOS);
            if (remaining < 0) {
                break;
            }
            try {
                Thread.sleep(remaining / 1_000_000, (int) (remaining % 1_000_000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    private void setDelayNanos(long nanos) {
        Instant target = Instant.now().plusNanos(nanos);
        if (target.isAfter(busyUntil)) {
            busyUntil = target;
        }
    }
}
