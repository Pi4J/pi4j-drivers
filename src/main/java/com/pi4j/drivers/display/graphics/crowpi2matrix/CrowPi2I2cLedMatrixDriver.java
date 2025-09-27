package com.pi4j.drivers.display.graphics.crowpi2matrix;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.GraphicsDisplayInfo;
import com.pi4j.drivers.display.graphics.PixelFormat;
import com.pi4j.io.i2c.I2C;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * Driver for the new I2C based CrowPi2 LED matrix that is used in the Pi5-compatible model.
 */
public class CrowPi2I2cLedMatrixDriver implements GraphicsDisplayDriver {
    public static final int I2C_ADDRESS = 0x66;

    private final GraphicsDisplayInfo displayInfo = new GraphicsDisplayInfo(8, 8, PixelFormat.RGB_888);
    private final I2C i2c;
    byte[] writeBuf = new byte[Offset.values().length];

    private Instant busyUntil = Instant.now();


    /** Creates a driver instance using the given i2c connection with the brightness set to 64 (1/4). */
    public CrowPi2I2cLedMatrixDriver(I2C i2c) {
        this.i2c = i2c;
        setBrightness(64);
    }

    /** Convenience constructor, as there seems to be only one configuration anyway */
    public CrowPi2I2cLedMatrixDriver(Context context) {
        this(context.create(I2C.newConfigBuilder(context).bus(1).device(I2C_ADDRESS).build()));
    }

    @Override
    public GraphicsDisplayInfo getDisplayInfo() {
        return displayInfo;
    }

    private void clear() {
        writeBuf[Offset.FUNC.ordinal()] = (byte) Func.CLEAR.ordinal();
        sendBuf();
    }

    private void setPixel(int x, int y, int r, int g, int b) {
        int targetAddress = y * displayInfo.getWidth() + x;

        writeBuf[Offset.FUNC.ordinal()] = (byte) Func.SETPIXELCOLOR.ordinal();
        writeBuf[Offset.POS.ordinal()] = (byte) targetAddress;

        writeBuf[Offset.R.ordinal()] = (byte) r;
        writeBuf[Offset.G.ordinal()] = (byte) g;
        writeBuf[Offset.B.ordinal()] = (byte) b;

        sendBuf();
    }

    private void fill(int pos, int count, int r, int g, int b) {
        writeBuf[Offset.FUNC.ordinal()] = (byte) Func.FILL.ordinal();
        writeBuf[Offset.FIRST.ordinal()] = (byte) pos;
        writeBuf[Offset.COUNT.ordinal()] = (byte) count;

        writeBuf[Offset.R.ordinal()] = (byte) r;
        writeBuf[Offset.G.ordinal()] = (byte) g;
        writeBuf[Offset.B.ordinal()] = (byte) b;

        sendBuf();
    }

    private void show() {
        writeBuf[Offset.FUNC.ordinal()] = (byte) Func.SHOW.ordinal();
        sendBuf();
    }

    private void sendBuf() {
        materializeDelay();
        writeBuf[Offset.LEN.ordinal()] = (byte) (writeBuf.length - Offset.LEN.ordinal());
        i2c.write(writeBuf);
        setDelayMicros(4000);
        Arrays.fill(writeBuf, (byte) 0);
    }

    public void setBrightness(int brightness) {
        writeBuf[Offset.FUNC.ordinal()] = (byte) Func.SETBRIGHTNESS.ordinal();
        writeBuf[Offset.BRIGHT.ordinal()] = (byte) brightness;
        sendBuf();
    }


    @Override
    public void setPixels(int x, int y, int w, int h, byte[] bytes) {
        int src = 0;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int r = bytes[src++];
                int g = bytes[src++];
                int b = bytes[src++];
                setPixel(x + j, y + i, r, g, b);
            }
        }
        show();
    }

    // Placing this here allows coordination of chip and connection based delays without needing a driver
    // reference here -- or complex interactions.
    void setDelayMicros(int micros) {
        Instant target = Instant.now().plusNanos(micros * 1000L);
        if (target.isAfter(busyUntil)) {
            busyUntil = target;
        }
    }

    void materializeDelay() {
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

    @Override
    public void close() {
        i2c.close();
    }


    /** Offsets of all parts of the 15 byte struct sent to the display */
    enum Offset {
        CMD, LEN, FUNC, POS,
        R, G, B, W, C,
        BRIGHT,
        FIRST, COUNT, DATA, DATA1
    }

    /** Display command function codes */
    enum Func {
        SHOW,
        SETPIXELCOLOR,
        FILL,
        SETBRIGHTNESS,
        WHITEOVERRAINBOW,
        GAMMA8,
        GAMMA32,
        NUMPIXEL,
        COLORHSV,
        CLEAR,
        SENDDATA2SHOW,
        SENDALLPIXRGB0,
        SENDALLPIXRGB1,
        SENDALLPIXRGB2,
        SENDALLPIXRGB3,
        SENDALLPIXRGB4,
        SENDALLPIXRGB5,
    }
}
