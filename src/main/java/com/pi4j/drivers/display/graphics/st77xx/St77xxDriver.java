package com.pi4j.drivers.display.graphics.st77xx;

import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.GraphicsDisplayInfo;
import com.pi4j.drivers.display.graphics.PixelFormat;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;

import java.util.EnumSet;

/**
 * Supported chipsets:
 * <ul>
 * <li>ST7735S https://www.waveshare.com/w/upload/e/e2/ST7735S_V1.1_20111121.pdf
 * <li>ST7789</li>
 * </ul>
 * Tested on
 * <ul>
 * <li>Adafruit 1.54" 240x240 Wide Angle TFT LCD Display with MicroSD - ST7789 with EYESPI Connector:
 *     https://www.adafruit.com/product/3787
 * <li>Waveshare display hats (see display hat drivers)
 * </ul>
 */
public class St77xxDriver implements GraphicsDisplayDriver {

    /** The SPI baud rate supported by this chip. */
    public static final int ST_7735_SPI_BAUDRATE = 15_000_000;
    public static final int ST_7789_SPI_BAUDRATE = 62_500_000;
    public static final EnumSet<PixelFormat> SUPPORTED_PIXEL_FORMATS = EnumSet.of(PixelFormat.RGB_444, PixelFormat.RGB_565);

    private final int xOffset;
    private final int yOffset;

    private static final int COLMOD_RGB_65K = 0x50;
    private static final int COLMOD_CONTROL_12BIT = 0x03;
    private static final int COLMOD_CONTROL_16BIT = 0x05;

    private static final int MADCTL_RGB_ORDER = 0x00;
    private static final int MADCTL_BGR_ORDER = 0x08;

    private static final byte[] addrBuf = new byte[4];

    private final Spi spi;
    private final DigitalOutput dc;
    private final DigitalOutput rst;
    private final GraphicsDisplayInfo displayInfo;
    private final boolean invert;

    public St77xxDriver(
            Spi spi,
            DigitalOutput dc,
            DigitalOutput rst,
            PixelFormat pixelFormat,
            boolean invert,
            int displayWidth,
            int displayHeight,
            int xOffset,
            int yOffset) {
        this.spi = spi;
        this.dc = dc;
        this.rst = rst;
        this.displayInfo = new GraphicsDisplayInfo(displayWidth, displayHeight, pixelFormat);
        this.invert = invert;
        this.xOffset = xOffset;
        this.yOffset = yOffset;

        init();
    }

    private void init() {
        if (rst != null) {
            rst.on();
        }

        command(Command.SWRESET);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        command(Command.SLPOUT);

        command(Command.COLMOD);
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

        command(Command.MADCTL);
        data(MADCTL_BGR_ORDER);

        addressRangeCommand(Command.CASET, xOffset, displayInfo.getWidth() + xOffset); // Column addr set
        addressRangeCommand(Command.RASET, yOffset, displayInfo.getHeight() + yOffset); // Row addr set

        if (invert) {
            command(Command.INVON);
        }
        command(Command.NORON);
        command(Command.DISPON);

        command(Command.MADCTL);
        data(0xC0);
    }

    private void command(Command command) {
        dc.off();
        spi.write(command.code);
    }

    /** Sends a command parameterized with screen address data */
    private void addressRangeCommand(Command command, int min, int max) {
        command(command);
        addrBuf[0] = (byte) (min >> 8);
        addrBuf[1] = (byte) min;
        addrBuf[2] = (byte) (max >> 8);
        addrBuf[3] = (byte) max;
        data(addrBuf);
    }

    private void data(int x) {
        if (x < 0 || x > 0xff) {
            throw new IllegalArgumentException("ST77xx data value out of range (0..255): " + x);
        }
        dc.on();
        spi.write(x);
        dc.off();
    }

    private void data(byte[] buf) {
        data(buf, buf.length);
    }

    private void data(byte[] x, int length) {
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
        addressRangeCommand(Command.CASET, xOffset + x, xOffset + x + width - 1); // Column addr set
        addressRangeCommand(Command.RASET, yOffset + y, yOffset + y + height - 1); // Row addr set
        command(Command.RAMWR); // write to RAM
        data(data, (width * height * displayInfo.getPixelFormat().getBitCount() + 7) / 8);
    }

    @Override
    public void close() {
        spi.close();
        dc.close();
        if (rst != null) {
            rst.off();
            rst.close();
        }
    }
}
