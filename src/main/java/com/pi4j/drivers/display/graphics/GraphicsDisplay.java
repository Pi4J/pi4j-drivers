package com.pi4j.drivers.display.graphics;

import com.pi4j.drivers.display.BitmapFont;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class GraphicsDisplay implements Closeable {
    // TODO(https://github.com/Pi4J/pi4j/issues/475): Remove or update this limitation.
    private static final int MAX_TRANSFER_SIZE = 4000;

    enum Rotation {
        ROTATE_0, ROTATE_90, ROTATE_180, ROTATE_270
    }

    protected final GraphicsDisplayDriver driver;
    private final Object lock = new Object();
    private final int[] displayBuffer;
    private final byte[] transferBuffer;
    private final Timer timer = new Timer();

    private int modifiedXMax = Integer.MIN_VALUE;
    private int modifiedXMin = Integer.MAX_VALUE;
    private int modifiedYMax = Integer.MIN_VALUE;
    private int modifiedYMin = Integer.MAX_VALUE;
    private TimerTask pendingUpdate = null;
    private int transferDelayMillis = 15;
    private final int displayWidth;
    private final int displayHeight;
    private final Rotation rotation;

    public GraphicsDisplay(GraphicsDisplayDriver driver) {
        this(driver, Rotation.ROTATE_0);
    }

    public GraphicsDisplay(GraphicsDisplayDriver driver, Rotation rotation) {
        this.driver = driver;
        this.rotation = rotation;
        if (rotation == Rotation.ROTATE_0 || rotation == Rotation.ROTATE_180) {
            displayWidth = driver.getDisplayInfo().getWidth();
            displayHeight = driver.getDisplayInfo().getHeight();
        } else {
            displayWidth = driver.getDisplayInfo().getHeight();
            displayHeight = driver.getDisplayInfo().getWidth();
        }
        displayBuffer = new int[displayWidth * displayHeight];
        transferBuffer = new byte[Math.min(
                MAX_TRANSFER_SIZE,
                (displayWidth * displayHeight * driver.getDisplayInfo().getPixelFormat().getBitCount() + 7) / 8)];
    }

    @Override
    public void close() {
        flush();
        driver.close();
    }

    /** Draws an image at the given coordinates */
    public void drawImage(int x, int y, int width, int height, int[] rgb888pixels) {
        synchronized (lock) {
            int xMin = Math.max(0, x);
            int yMin = Math.max(0, y);
            int xMax = Math.min(x + width, displayWidth);
            int yMax = Math.min(y + height, displayHeight);
            if (xMax <= xMin || yMax <= yMin) {
                return;
            }
            for (int targetY = yMin; targetY < yMax; targetY++) {
                System.arraycopy(
                        rgb888pixels,
                        (targetY - y) * width + xMin - x,
                        displayBuffer,
                        pixelAddress(xMin, targetY),
                        xMax - xMin);
            }
            markModified(xMin, yMin, xMax, yMax);
        }
    }

    public void fillRect(int x, int y, int width, int height, int rgb888) {
        synchronized (lock) {
            int xMin = Math.max(0, x);
            int yMin = Math.max(0, y);
            int xMax = Math.min(x + width, displayWidth);
            int yMax = Math.min(y + height, displayHeight);
            if (xMax <= xMin || yMax <= yMin) {
                return;
            }
            for (int targetY = yMin; targetY < yMax; targetY++) {
                int start = pixelAddress(xMin, targetY);
                Arrays.fill(displayBuffer, start, start + xMax - xMin, rgb888);
            }
            markModified(xMin, yMin, xMax, yMax);
        }
    }

    /** Forces an immediate transfer of the modified screen area */
    public void flush() {
        synchronized (lock) {
            if (modifiedXMin < Integer.MAX_VALUE) {
                transferBuffer(modifiedXMin, modifiedYMin, modifiedXMax, modifiedYMax);
                modifiedXMin = Integer.MAX_VALUE;
                modifiedYMin = Integer.MAX_VALUE;
                modifiedXMax = Integer.MIN_VALUE;
                modifiedYMax = Integer.MIN_VALUE;
            }
        }
    }

    /**
     * Returns the display height in pixels. This might differ from the driver display information, as the screen
     * might be rotated.
     */
    public int getHeight() {
        return displayHeight;
    }

    /**
     * Returns the display width in pixels. This might differ from the driver display information, as the screen
     * might be rotated.
     */
    public int getWidth() {
        return displayWidth;
    }

    /**
     * Renders a text string at the given position with the given font and color.
     * <p>
     * Returns the width of the rendered text in pixel.
     */
    public int renderText(int x, int baselineY, String text, BitmapFont font, int color) {
        return renderText(x, baselineY, text, font, color, 1, 1);
    }

    /**
     * Renders a text string at the given position with the given font, color and scale.
     * <p>
     * Returns the width of the rendered text in pixel.
     */
    public int renderText(
            int x, int baselineY, String text, BitmapFont font, int color, int scaleX, int scaleY
    ) {
        int length = text.length();
        int width = 0;
        for (int offset = 0; offset < length; ) {
            int codepoint = text.codePointAt(offset);
            offset += Character.charCount(codepoint);
            width += renderCharacter(x + width, baselineY, codepoint, font, color, scaleX, scaleY);
        }
        return width;
    }

    /**
     * Renders a single character at the given position.
     * <p>
     * Returns the width of the character in pixel.
     */
    public int renderCharacter(
            int x0, int baselineY, int codepoint, BitmapFont font, int color, int scaleX, int scaleY
    ) {
        BitmapFont.Glyph glyph = font.getGlyph(codepoint);
        if (glyph == null) {
            return font.getCellWidth();
        }
        int w = glyph.getWidth();
        int h = font.getCellHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (glyph.getPixel(x, y)) {
                    int x2 = x;
                    // Combine neighbouring pixels to reduce the number of calls.
                    while (x2 < glyph.getWidth() && glyph.getPixel(x2 + 1, y)) {
                        x2++;
                    }
                    if (scaleX == 1 && scaleY == 1 && x2 == x) {
                        setPixel(x0 + x, baselineY + y - h, color);
                    } else {
                        fillRect(
                                x0 + scaleX * x,
                                baselineY + (y - h) * scaleY,
                                scaleX * (x2 - x + 1),
                                scaleY,
                                color);
                    }
                    x = x2;
                }
            }
        }
        return w * scaleX;
    }

    /** Sets the pixel at the given coordinates to the given color */
    public void setPixel(int x, int y, int color) {
        synchronized (lock) {
            if (x < 0 || y < 0 || x >= displayWidth || y >= displayHeight) {
                return;
            }
            displayBuffer[pixelAddress(x, y)] = color;
            markModified(x, y, x + 1, y + 1);
        }
    }

    /**
     * Sets the maximum delay between graphics updates and the screen buffer transfer to the display driver.
     * Setting the value to 0 will send all data immediately. A negative value will require an explicit
     * call to flush for the transfer. The default value is 15;
     */
    public void setTransferDelayMillis(int millis) {
        this.transferDelayMillis = millis;
    }

    // Private methods. Note that internally
    // - we assume coordinates are in range while we account for out-of-bounds coordinates in user methods.
    // - we use min/max coordinate bounds instead of width/height as in user methods.

    /** Marks the given screen area as modified */
    private void markModified(int xMin, int yMin, int xMax, int yMax) {
        synchronized (lock) {
            modifiedXMin = Math.min(modifiedXMin, xMin);
            modifiedYMin = Math.min(modifiedYMin, yMin);
            modifiedXMax = Math.max(modifiedXMax, xMax);
            modifiedYMax = Math.max(modifiedYMax, yMax);
            if (transferDelayMillis == 0) {
                flush();
            } else if (pendingUpdate == null && transferDelayMillis > 0) {
                pendingUpdate = new TimerTask() {
                    @Override
                    public void run() {
                        pendingUpdate = null;
                        flush();
                    }};
                timer.schedule(pendingUpdate, transferDelayMillis);
            }
        }
    }

    /** Returns the address of the given pixel in the display buffer */
    private int pixelAddress(int x, int y) {
        return y * displayWidth + x;
    }

    /** Transfers the given display buffer area to the display driver, mapping the rotation */
    private void transferBuffer(int xMin, int yMin, int xMax, int yMax) {
        switch (rotation) {
            case ROTATE_0 ->
                transferBuffer(pixelAddress(xMin, yMin), 1, displayWidth, xMin, yMin, xMax, yMax);
            case ROTATE_90 ->
                transferBuffer(pixelAddress(xMin, yMax - 1), -displayWidth, 1, displayHeight - yMax, displayWidth - xMax, displayHeight - yMin, displayWidth - xMin);
            case ROTATE_180 ->
                transferBuffer(pixelAddress(xMax - 1, yMax - 1), -1, -displayWidth, displayWidth - xMax, displayHeight - yMax, displayWidth - xMin, displayHeight - yMin);
            case ROTATE_270 ->
                transferBuffer(pixelAddress(xMax - 1, yMin), displayWidth, -1, yMin, xMin, yMax, xMax);
        }
    }

    /** Transfers the given display buffer area to the display driver */
    private void transferBuffer(int sourceAddress, int sourceStrideX, int sourceStrideY, int xMin, int yMin, int xMax, int yMax) {
        synchronized (lock) {
            int xGranularity = driver.getDisplayInfo().getXGranularity();
            xMin = (xMin / xGranularity) * xGranularity;
            xMax = ((xMax + xGranularity - 1) / xGranularity) * xGranularity;

            int width = xMax - xMin;
            int height = yMax - yMin;

            PixelFormat pixelFormat = driver.getDisplayInfo().getPixelFormat();
            int bitsPerRow = width * pixelFormat.getBitCount();
            int bitOffset = 0;
            for (int i = 0; i < height; i++) {
                bitOffset += pixelFormat.writeRgb(
                        displayBuffer,
                        sourceAddress,
                        sourceStrideX,
                        transferBuffer,
                        bitOffset,
                        width);
                sourceAddress += sourceStrideY;
                // Transfer if the last row is reached or the next row would overflow the buffer.
                if (i == height - 1 || bitOffset + bitsPerRow > transferBuffer.length * 8) {
                    int rows = bitOffset / bitsPerRow;
                    driver.setPixels(xMin, yMin + i + 1 - rows, width, rows, transferBuffer);
                    bitOffset = 0;
                }
            }
        }
    }
}
