package com.pi4j.drivers.display.graphics.sh1106;

import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.GraphicsDisplayInfo;
import com.pi4j.drivers.display.graphics.PixelFormat;
import com.pi4j.io.OnOffWrite;
import com.pi4j.io.spi.Spi;

import java.io.Closeable;
import java.io.IOException;

/**
 * A driver for 128x64 displays using the SH1106 chip. The display is rotated to simplify communication with
 * the GraphicsDisplay implementation. The rotation is automatically reversed there.
 */
public class Sh1106Driver implements GraphicsDisplayDriver {
    private static final GraphicsDisplayInfo DISPLAY_INFO = new GraphicsDisplayInfo(
            64, 128, PixelFormat.MONOCHROME, 8, GraphicsDisplay.Rotation.ROTATE_90);
    private final Spi spi;
    private final OnOffWrite<?> dc;
    private final OnOffWrite<?> rst;
    private final byte[] cmdBuf = new byte[256];
    private final byte[] displayBuf = new byte[8 * 128];

    public Sh1106Driver(Spi spi, OnOffWrite<?> dc, OnOffWrite rst) {
        this.spi = spi;
        this.dc = dc;
        this.rst = rst;
        rst.on();
        command(
                0xAE, 0x00,  // display off
                0xC8, 0x00,         // Scan from 127 to 0 (Reverse scan)
                0xAF, 0x00         // Display on
        );
    }

    private void command(int... code) {
        dc.off();
        for (int i = 0; i < code.length; i++) {
            cmdBuf[i] = (byte) code[i];
        }
        spi.write(cmdBuf, 0, code.length);
    }

    private void sendData(int x, int y, int count) {
        command(
                0xB0 | (x >> 3),
                0x10 | ((y + 2) >> 4), (y + 2) & 0x0f);
        dc.on();
        spi.write(displayBuf, pixelAddress(x, y), count);
    }

    @Override
    public GraphicsDisplayInfo getDisplayInfo() {
        return DISPLAY_INFO;
    }

    private int pixelAddress(int x, int y) {
        int address = 128 * (x >> 3) + y;
        return address;
    }

    @Override
    public void setPixels(int x, int y, int width, int height, byte[] data) {
        int index = 0;
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px += 8) {
                displayBuf[pixelAddress(x + px, y + py)] = (byte) Integer.reverse(data[index++]<<24);
            }
        }

        for (int px = 0; px < width; px += 8) {
            sendData(x + px, y, height);
        }
    }

    @Override
    public void close() {
        if (dc instanceof Closeable) {
            try {
                ((Closeable) dc).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (rst instanceof Closeable) {
            try {
                ((Closeable) rst).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        spi.close();
    }
}
