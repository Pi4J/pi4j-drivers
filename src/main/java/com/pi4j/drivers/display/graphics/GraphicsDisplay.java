package com.pi4j.drivers.display.graphics;

import com.pi4j.drivers.display.BitmapFont;

import java.util.*;

public class GraphicsDisplay {
    // TODO(https://github.com/Pi4J/pi4j/issues/475): Remove or update this limitation.
    private static final int MAX_TRANSFER_SIZE = 4000;

    public enum Rotation {
        ROTATE_0, ROTATE_90, ROTATE_180, ROTATE_270;

        public Rotation plus(Rotation other) {
            Rotation[] values = Rotation.values();
            return values[(ordinal() + other.ordinal()) % values.length];
        }

        public Rotation minus(Rotation other) {
            Rotation[] values = Rotation.values();
            return values[(values.length + ordinal() - other.ordinal()) % values.length];
        }
    }

    // Directly accessed by Graphics
    final Object lock = new Object();
    final int[] displayBuffer;

    private final Timer timer = new Timer();

    private int modifiedXMax = Integer.MIN_VALUE;
    private int modifiedXMin = Integer.MAX_VALUE;
    private int modifiedYMax = Integer.MIN_VALUE;
    private int modifiedYMin = Integer.MAX_VALUE;
    private TimerTask pendingUpdate = null;
    private int transferDelayMillis = 15;
    private final int displayWidth;
    private final int displayHeight;
    private final List<DriverEntry> drivers = new ArrayList<>();

    public GraphicsDisplay(GraphicsDisplayDriver driver) {
        this(driver, Rotation.ROTATE_0);
    }

    public GraphicsDisplay(GraphicsDisplayDriver driver, Rotation rotation) {
        rotation = rotation.minus(driver.getDisplayInfo().getImplicitRotation());
        if (rotation == Rotation.ROTATE_0 || rotation == Rotation.ROTATE_180) {
            displayWidth = driver.getDisplayInfo().getWidth();
            displayHeight = driver.getDisplayInfo().getHeight();
        } else {
            displayWidth = driver.getDisplayInfo().getHeight();
            displayHeight = driver.getDisplayInfo().getWidth();
        }
        displayBuffer = new int[displayWidth * displayHeight];
        drivers.add(new DriverEntry(0, 0, driver, rotation));
    }

    /**
     * Creates a virtual display with no drivers attached. Useful for mapping a single "logical" display to multiple
     * physical displays, e.g. when managing multiple light strip based LED displays.
     */
    public GraphicsDisplay(int displayWidth, int displayHeight) {
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        displayBuffer = new int[displayWidth * displayHeight];
    }

    /**
     * Map the given display driver into the given position of this GraphicsDisplay. Useful for rendering
     * the same content to multiple physical displays or to have a "virtual" display span multiple physical
     * displays.
     */
    public void attachDriver(int x0, int y0, GraphicsDisplayDriver driver, Rotation rotation) {
        synchronized (lock) {
            drivers.add(new DriverEntry(x0, y0, driver, rotation.minus(driver.getDisplayInfo().getImplicitRotation())));
        }
    }

    public void close() {
        flush();
        timer.cancel();
        synchronized (lock) {
            for (DriverEntry entry : drivers) {
                entry.driver.close();
            }
            drivers.clear();
        }
    }

    /** Draws an image at the given coordinates */
    @Deprecated
    public void drawImage(int x, int y, int width, int height, int[] rgb888pixels) {
        getGraphics().drawRgb(x, y, width, height, rgb888pixels);
    }

    @Deprecated
    public void fillRect(int x, int y, int width, int height, int rgb888) {
        Graphics graphics = getGraphics();
        graphics.setColor(rgb888);
        graphics.fillRect(x, y, width, height);
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

    /** Obtains a new graphics context for this display. */
    public Graphics getGraphics() {
        return new Graphics(this);
    }

    /** Returns the width of this dispaly in pixel. */
    public int getWidth() {
        return displayWidth;
    }

    /** Returns the height of this dispaly in pixel. */
    public int getHeight() {
        return displayHeight;
    }

    /**
     * Renders a text string at the given position with the given font and color.
     * <p>
     * Returns the width of the rendered text in pixel.
     */
    @Deprecated
    public int renderText(int x, int baselineY, String text, BitmapFont font, int color) {
        return renderText(x, baselineY, text, font, color, 1, 1);
    }

    /**
     * Renders a text string at the given position with the given font, color and scale.
     * <p>
     * Returns the width of the rendered text in pixel.
     */
    @Deprecated
    public int renderText(
            int x, int baselineY, String text, BitmapFont font, int color, int scaleX, int scaleY
    ) {
        Graphics graphics = getGraphics();
        graphics.setColor(color);
        graphics.setFont(font);
        graphics.setTextScale(scaleX, scaleY);
        return graphics.renderText(x, baselineY, text);
    }

    /**
     * Renders a single character at the given position.
     * <p>
     * Returns the width of the character in pixel.
     */
    @Deprecated
    public int renderCharacter(
            int x0, int baselineY, int codepoint, BitmapFont font, int color, int scaleX, int scaleY
    ) {
       Graphics graphics = getGraphics();
       graphics.setColor(color);
       graphics.setFont(font);
       graphics.setTextScale(scaleX, scaleY);
       return graphics.renderCharacter(x0, baselineY, codepoint);
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

    // Package visible methods used by the graphics context.

    /** Marks the given screen area as modified */
    void markModified(int xMin, int yMin, int xMax, int yMax) {
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

    /** Does not call markModified and does not take any clipping into account */
    void drawRgbRow(int x, int y, int scaledWidth, int[] rgbData, boolean processAlpha, int offset, int scaleX, int remainder) {
        int dst = pixelAddress(x, y);
        if (!processAlpha) {
            if (scaleX == 1) {
                System.arraycopy(rgbData, offset, displayBuffer, dst, scaledWidth);
            } {
                for (int i = 0; i < scaledWidth; i++) {
                    displayBuffer[dst + i] = rgbData[offset + (i + remainder) / scaleX];
                }
            }
        } else {
            for (int i = 0; i < scaledWidth; i++) {
                int srcArgb = rgbData[offset + i / scaleX];
                int srcAlpha = (srcArgb >> 24) & 0xff;
                switch (srcAlpha) {
                    case 0 -> {}
                    case 255 -> displayBuffer[dst + i] = srcArgb | 0xff000000;
                    default -> {
                        int dstRgb = displayBuffer[dst + i];

                        int srcRed = (srcArgb >> 16) & 0xff;
                        int srcGreen = (srcArgb >> 8) & 0xff;
                        int srcBlue = srcArgb & 0xff;

                        int dstRed = Math.min(255, (((dstRgb >> 16) & 0xff) * (255 - srcAlpha) + srcRed * srcAlpha) / 255);
                        int dstGreen = Math.min(255, (((dstRgb >> 8) & 0xff) * (255 - srcAlpha) + srcGreen * srcAlpha) / 255);
                        int dstBlue = Math.min(255, ((dstRgb & 0xff) * (255 - srcAlpha) + srcBlue * srcAlpha) / 255);

                        displayBuffer[dst + i] = 0xff000000 | (dstRed << 16) | (dstGreen << 8) | dstBlue;
                    }
                }
            }
        }
    }

    /** Does not call markModified and does not take any clipping into account */
    void drawHLine(int x, int y, int len, int color, long pattern) {
        int dst = pixelAddress(x, y);
        if (pattern == -1) {
            Arrays.fill(displayBuffer, dst, dst + len, color);
        } else {
            for (int i = 0; i < len; i++) {
                if ((pattern & (1L << (i % 64))) != 0) {
                    displayBuffer[dst + i] = color;
                }
            }
        }
    }

    // Private methods. Note that internally
    // - we assume coordinates are in range while we account for out-of-bounds coordinates in user methods.
    // - we use min/max coordinate bounds instead of width/height as in user methods.

    /** Returns the address of the given pixel in the display buffer */
    private int pixelAddress(int x, int y) {
        return y * displayWidth + x;
    }

    /** Transfers the given display buffer area to the display driver, mapping the rotation */
    private void transferBuffer(int xMin, int yMin, int xMax, int yMax) {
        synchronized (lock) { // drivers access
            for (DriverEntry driverEntry : drivers) {
                driverEntry.transferBuffer(xMin, yMin, xMax, yMax);
            }
        }
    }

    /** Keeps track of the screen area and rotation managed by a driver */
    class DriverEntry {
        private final int x0;
        private final int y0;
        private final GraphicsDisplayDriver driver;
        private final Rotation rotation;
        private final byte[] transferBuffer ;

        private DriverEntry(int x0, int y0, GraphicsDisplayDriver driver, Rotation rotation) {
            this.x0 = x0;
            this.y0 = y0;
            this.driver = driver;
            this.rotation = rotation;
            this.transferBuffer = new byte[Math.min(
                    MAX_TRANSFER_SIZE,
                    (driver.getDisplayInfo().getWidth() * driver.getDisplayInfo().getHeight() * driver.getDisplayInfo().getPixelFormat().getBitCount() + 7) / 8)];
        }

        private void transferBuffer(int xMin, int yMin, int xMax, int yMax) {
            switch (rotation) {
                case ROTATE_0 ->
                        transferBuffer(pixelAddress(xMin, yMin), 1, displayWidth,
                                xMin - x0, yMin - y0, xMax - x0, yMax - y0);
                case ROTATE_90 ->
                        transferBuffer(pixelAddress(xMin, yMax - 1), -displayWidth, 1,
                                displayHeight - yMax - y0, xMin - x0, displayHeight - yMin - y0, xMax - x0);
                case ROTATE_180 ->
                        transferBuffer(pixelAddress(xMax - 1, yMax - 1), -1, -displayWidth,
                                displayWidth - xMax - x0, displayHeight - yMax - y0, displayWidth - xMin - x0, displayHeight - yMin - y0);
                case ROTATE_270 ->
                        transferBuffer(pixelAddress(xMax - 1, yMin), displayWidth, -1,
                                yMin - y0, displayWidth - xMax - x0, yMax - y0, displayWidth - xMin - x0);
            }
        }

        /** Transfers the given display buffer area to the display driver */
        private void transferBuffer(int sourceAddress, int sourceStrideX, int sourceStrideY, int xMin, int yMin, int xMax, int yMax) {
            GraphicsDisplayInfo displayInfo = driver.getDisplayInfo();

            // Bail out if the changed area is outside the area governed by this device.
            if (xMax <= 0 || yMax <= 0 || xMin >= displayInfo.getWidth() || yMin >= displayInfo.getHeight()) {
                return;
            }

            // Restrict coordinates to the display size.
            if (xMin < 0) {
                sourceAddress -= xMax * sourceStrideX;
                xMin = 0;
            }
            if (yMin < 0) {
                sourceAddress -= yMin * sourceStrideY;
                yMin = 0;
            }
            xMax = Math.min(displayInfo.getWidth(), xMax);
            yMax = Math.min(displayInfo.getHeight(), yMax);

            // Make sure to match device x-alignment constraints.
            int xGranularity = driver.getDisplayInfo().getXGranularity();
            int remainder = xMin % xGranularity;
            if (remainder != 0) {
                sourceAddress -= remainder * sourceStrideX;
                xMin -= remainder;
            }
            xMax = ((xMax + xGranularity - 1) / xGranularity) * xGranularity;

            int width = xMax - xMin;
            int height = yMax - yMin;

            PixelFormat pixelFormat = driver.getDisplayInfo().getPixelFormat();
            int bitsPerRow = width * pixelFormat.getBitCount();
            int bitOffset = 0;

            synchronized (lock) { // display / transfer buffer access
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
}
