package com.pi4j.drivers.display.graphics.console;

import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.GraphicsDisplayInfo;
import com.pi4j.drivers.display.graphics.PixelFormat;

/**
 * A graphics driver that renders to the console, mostly for debug / testing purposes.
 * <p>
 * Note that this will produce artifacts unless the line height matches the character height.
 */
public class ConsoleGraphicsDriver implements GraphicsDisplayDriver {
    // Screen buffer; we could get rid of this by adding y-granularity support but then would lose the
    // render on close option.
    private byte[] buffer;

    private final GraphicsDisplayInfo displayInfo;
    private final boolean renderOnClose;

    public ConsoleGraphicsDriver(int width, int height, boolean renderOnClose) {
        this.displayInfo = new GraphicsDisplayInfo(width, height, PixelFormat.RGB_888);
        this.renderOnClose = renderOnClose;
        buffer = new byte[width * height * 3];
    }

    @Override
    public GraphicsDisplayInfo getDisplayInfo() {
        return displayInfo;
    }

    @Override
    public void setPixels(int x, int y, int width, int height, byte[] data) {
        for (int i = 0; i < height; i++) {
            System.arraycopy(data, i * width * 3, buffer, pixelAddress(x, y + i), width * 3);
        }
        if (!renderOnClose) {
            render(x, y, width, height);
        }
    }

    @Override
    public void close() {
        if (renderOnClose) {
            render(0, 0, displayInfo.getWidth(), displayInfo.getHeight());
        }
    }

    private void render(int x, int y, int width, int height) {
        if ((y & 1) != 0) {
            y--;
            height++;
        }
        for (int i = 0; i < height; i += 2) {
            if (!renderOnClose) {
                System.out.print("\u001b[" + (x + 1) + ";" + ((y + i) / 2 + 1) + "H");
            }
            int fgPos = pixelAddress(x,  i);
            int bgPos = pixelAddress(x, i + 1);
            for (int j = 0; j < width; j ++) {
                System.out.print("\u001b[48;2;" + (buffer[fgPos] & 0xff) +";" +(buffer[fgPos + 1] & 0xff) + ";" + (buffer[fgPos + 2] & 0xff) + "m");
                System.out.print("\u001b[38;2;" + (buffer[bgPos] & 0xff) +";" +(buffer[bgPos + 1] & 0xff) + ";" + (buffer[bgPos + 2] & 0xff) + "m▄");
                fgPos += 3;
                bgPos += 3;
            }
            if (renderOnClose) {
                System.out.println("\033[0m");
            }
        }
    }

    private int pixelAddress(int x, int y) {
        return (y * displayInfo.getWidth() + x) * 3;
    }
}
