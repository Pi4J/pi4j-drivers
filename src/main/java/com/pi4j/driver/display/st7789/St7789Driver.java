package com.pi4j.driver.display.st7789;

import java.io.IOException;

import com.pi4j.driver.display.GraphicsDisplayDriver;
import com.pi4j.driver.display.PixelFormat;
import com.pi4j.driver.display.DisplayInfo;

import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Tested on Adafruit 1.54" 240x240 Wide Angle TFT LCD Display with MicroSD - ST7789 with EYESPI Connector
 * https://www.adafruit.com/product/3787
 */

public class St7789Driver implements GraphicsDisplayDriver {

    private static Logger log = LoggerFactory.getLogger(St7789Driver.class);

    private final int OFFSET = 80;
    private final int WIDTH = 240;
    private final int HEIGHT = 240;

    // 16 bit
    private final byte[] image = new byte[WIDTH * HEIGHT * 16 / 8];

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
    private static final int COLMOD_CONTROL_16BIT = 0x05;

    private static final int MADCTL_RGB_ORDER = 0x00;
    private static final int MADCTL_BGR_ORDER = 0x08;

    private Spi spi;
    private DigitalOutput dc;

    public St7789Driver(Spi spi, DigitalOutput dc) {

        this.spi = spi;
        this.dc = dc;

        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void init() throws java.io.IOException {

        command(SWRESET);

        command(SLPOUT);

        command(COLMOD);
        data( COLMOD_RGB_65K | COLMOD_CONTROL_16BIT );

        command(MADCTL);
        data( MADCTL_BGR_ORDER );

        command(CASET); // Column addr set
        byte[] cols = new byte[4];
        cols[0] = 0x00;
        cols[1] = 0x00;
        cols[2] = (byte) (WIDTH >> 8);
        cols[3] = (byte) (WIDTH & 0xff);
        data(cols);

        command(RASET); // Row addr set
        byte[] rows = new byte[4];
        rows[0] = 0x00;
        rows[1] = 0x50;
        rows[2] = (byte) ((OFFSET + HEIGHT) >> 8);
        rows[3] = (byte) ((OFFSET + HEIGHT) & 0xff);
        data(rows);

        command(INVON);

        command(NORON);

        command(DISPON);

        command(MADCTL);
        data(0xC0);

    }

    private void command(int x) throws com.pi4j.io.exception.IOException, IOException {

        if (x < 0 || x > 0xff) {
            throw new IllegalArgumentException("ST7789 bad command value " + x);
        }

        log.trace("Command: {}", x);

        dc.off();
        byte[] buffer = new byte[1];
        buffer[0] = (byte) x;
        spi.write(buffer);
    }

    private void data(int x) throws IOException, com.pi4j.io.exception.IOException {

        if (x < 0 || x > 0xff) {
            throw new IllegalArgumentException("ST7789 bad data value " + x);
        }

        byte[] buffer = new byte[1];
        buffer[0] = (byte) x;

        data(buffer);
    }

    private void data(byte[] x) throws IOException, com.pi4j.io.exception.IOException {

        String raw = java.util.HexFormat.of().formatHex(x);
        if (raw.length() > 100) {
            log.trace("Data: {} {}", x.length, raw.substring(0, 80));
        } else {
            log.trace("Data: {} {}", x.length, raw);
        }

        dc.on();
        spi.write(x);
        dc.off();
    }

    @Override
    public DisplayInfo getDisplayInfo() {

        return new DisplayInfo( WIDTH, HEIGHT, PixelFormat.RGB_565);
    }

    @Override
    public void setPixels(int x, int y, int w, byte[] data, int offset, int length) throws Exception {

         int j = 0;
         for (int i = 0; i < w; i++) {
            int rgb565 = ((data[offset + 2*j] & 0xff) << 8 ) | (data[offset + 2*j + 1] & 0xff);
            updateImage(x + i, y, rgb565);
            j++;
            if (j >= length) {
                j = 0;
            }
        }
        showImage();
    }

    public void setPixel(int x, int y, int rgb) throws Exception {

        command(CASET); // Column addr set
        byte[] cols = new byte[4];
        cols[0] = 0x00;
        cols[1] = (byte) x;
        cols[2] = 0x00;
        cols[3] = (byte) x;
        data(cols);

        command(RASET); // Row addr set
        byte[] rows = new byte[4];
        rows[0] = (byte) ((OFFSET + y) >> 8);
        rows[1] = (byte) ((OFFSET + y) & 0xff);
        rows[2] = (byte) ((OFFSET + y) >> 8);
        rows[3] = (byte) ((OFFSET + y) & 0xff);
        data(rows);

        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = (rgb) & 0xFF;

        final int value = rgb888toRgb565(red, green, blue);

        command(RAMWR); // write to RAM
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (value >> 8);
        bytes[1] = (byte) value;
        data(bytes);
    }

    public void updateImage(int x, int y, int rgb) {

        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            throw new IllegalArgumentException("Invalid Pixel [" + x + "," + y + "]");
        }

        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = (rgb) & 0xFF;

        final int index = ((y * WIDTH) + x) * 2;

        final int value = rgb888toRgb565(red, green, blue);

        image[index] = (byte) (value >> 8);
        image[index + 1] = (byte) value;
    }

    private int rgb888toRgb565(int r, int g, int b) {

        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            throw new IllegalArgumentException("Invalid Colour (" + r + "," + g + "," + b + ")");
        }

        // rounding correction when reducing from 8 bits per channel to fewer bits, so that colors don’t just get truncated but instead rounded
        // for red and blue: if the 3rd least significant bit is set (0x04), it adds 4.
        // For green: if the 2nd least significant bit is set (0x02), it adds 2 
        if ((r & 0x04) != 0) {
            r += 0x04;

            if (r > 255) {
                r = 255;
            }
        }

        if ((g & 0x02) != 0) {
            g += 0x02;

            if (g > 255) {
                g = 255;
            }
        }

        if ((b & 0x04) != 0) {
            b += 0x04;

            if (b > 255) {
                b = 255;
            }
        }

        final int value = ((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3);
        return value;

    }

    public void showImage() throws IOException {

        log.trace("window");
        command(CASET); // Column addr set
        byte[] cols = new byte[4];
        cols[0] = 0x00;
        cols[1] = 0x00;
        cols[2] = (byte) (WIDTH - 1 >> 8);
        cols[3] = (byte) (WIDTH - 1 & 0xff);
        data(cols);

        command(RASET); // Row addr set
        byte[] rows = new byte[4];
        rows[0] = 0x00;
        rows[1] = 0x50;
        rows[2] = (byte) ((OFFSET + HEIGHT - 1) >> 8);
        rows[3] = (byte) ((OFFSET + HEIGHT - 1) & 0xff);
        data(rows);

        command(RAMWR); // write to RAM
        data(image);
    }
}
