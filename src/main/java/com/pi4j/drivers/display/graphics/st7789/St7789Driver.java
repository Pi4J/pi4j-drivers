package com.pi4j.drivers.display.graphics.st7789;

import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.PixelFormat;
import com.pi4j.drivers.display.graphics.GraphicsDisplayInfo;

import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/*
 * Tested on Adafruit 1.54" 240x240 Wide Angle TFT LCD Display with MicroSD - ST7789 with EYESPI Connector
 * https://www.adafruit.com/product/3787
 */

public class St7789Driver implements GraphicsDisplayDriver {

    public static final EnumSet<PixelFormat> SUPPORTED_PIXEL_FORMATS = EnumSet.of(PixelFormat.RGB_444, PixelFormat.RGB_565);

    private static Logger log = LoggerFactory.getLogger(St7789Driver.class);
    private final static int WIDTH = 240;

    // This chip controls 240x320
    // An offset of 80 allows it to control 240x240
    private final int yOffset;

    private static final int SWRESET = 0x01;
    private static final int SLPOUT = 0x11;
    private static final int NORON = 0x13;
    private static final int INVON = 0x21;
    private static final int DISPON = 0x29;
    private static final int CASET = 0x2A;
    private static final int RASET = 0x2B;
    private static final int RAMWR = 0x2C;
    private static final int MADCTL = 0x36;
    private static final int COLMOD = 0x3A;

    private static final int COLMOD_RGB_65K = 0x50;
    private static final int COLMOD_CONTROL_12BIT = 0x03;
    private static final int COLMOD_CONTROL_16BIT = 0x05;

    private static final int MADCTL_RGB_ORDER = 0x00;
    private static final int MADCTL_BGR_ORDER = 0x08;

    private static final byte[] addrBuf = new byte[4];

    private final Spi spi;
    private final DigitalOutput dc;
    private final GraphicsDisplayInfo displayInfo;

    public St7789Driver(Spi spi, DigitalOutput dc, int displayHeight, PixelFormat pixelFormat) {
        this.spi = spi;
        this.dc = dc;
        this.displayInfo = new GraphicsDisplayInfo(WIDTH, displayHeight, pixelFormat);
        this.yOffset = 320 - displayHeight;

        init();
    }

    private void init() {

        command(SWRESET);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        command(SLPOUT);

        command(COLMOD);
        switch (displayInfo.getPixelFormat()) {
            case RGB_444:
                data(COLMOD_RGB_65K | COLMOD_CONTROL_12BIT);
                break;
            case RGB_565:
                data(COLMOD_RGB_65K | COLMOD_CONTROL_16BIT);
                break;
            default:
                throw new IllegalArgumentException("Unsupported pixel format: " + displayInfo.getPixelFormat());
        }

        command(MADCTL);
        data(MADCTL_BGR_ORDER);

        command(CASET, 0, WIDTH); // Column addr set
        command(RASET, yOffset, displayInfo.getHeight() + yOffset); // Row addr set

        command(INVON);
        command(NORON);
        command(DISPON);

        command(MADCTL);
        data(0xC0);

    }

    private void command(int x) {
        if (x < 0 || x > 0xff) {
            throw new IllegalArgumentException("ST7789 bad command value " + x);
        }

        log.trace("Command: {}", x);

        dc.off();
        spi.write(x);
    }

    /** Sends a command parameterized with screen address data */
    private void command(int commandCode, int min, int max) {
        command(commandCode);
        addrBuf[0] = (byte) (min >> 8);
        addrBuf[1] = (byte) min;
        addrBuf[2] = (byte) (max >> 8);
        addrBuf[3] = (byte) max;
        data(addrBuf);
    }

    private void data(int x) {
        if (x < 0 || x > 0xff) {
            throw new IllegalArgumentException("ST7789 bad data value " + x);
        }
        dc.on();
        spi.write(x);
        dc.off();
    }

    private void data(byte[] buf) {
        data(buf, buf.length);
    }

    private void data(byte[] x, int length) {
        if (log.isTraceEnabled()) { // Avoid large string allocation if logging is off.
            String raw = java.util.HexFormat.of().formatHex(x);
            if (raw.length() > 100) {
                log.trace("Data: {} {}", length, raw.substring(0, 80));
            } else {
                log.trace("Data: {} {}", length, raw);
            }
        }
        dc.on();
        spi.write(x, length);
        dc.off();
    }

    @Override
    public GraphicsDisplayInfo getDisplayInfo() {
        return displayInfo;
    }

    @Override
    public void setPixels(int x, int y, int width, int height, byte[] data) {
        log.trace("setPixels {}", data.length);
        command(CASET, x, x + width - 1); // Column addr set
        command(RASET, yOffset + y, yOffset + y + height - 1); // Row addr set
        command(RAMWR); // write to RAM
        data(data, (width * height * displayInfo.getPixelFormat().getBitCount() + 7) / 8);
    }

    @Override
    public void close() {
        spi.close();
        dc.close();
    }
}
