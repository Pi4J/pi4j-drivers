package com.pi4j.drivers.display.graphics;

import com.pi4j.drivers.display.BitmapFont;

/** A simple graphics context tailored towards pixel-exact rendering on small displays. */
public class Graphics {
    private final GraphicsDisplay display;

    private BitmapFont font = BitmapFont.get5x8Font();
    private int color = 0xffffffff;
    private int textScaleX = 1;
    private int textScaleY = 1;
    private int[] glyphBuffer = new int[2];
    private int[] textPalette = new int[2];
    private int[] indexedTransferBuffer = new int[32];
    private boolean processAlpha;
    private int clipXMin = 0;
    private int clipYMin = 0;
    private int clipXMax;
    private int clipYMax;

    Graphics(GraphicsDisplay display) {
        this.display = display;
        this.clipXMax = display.getWidth();
        this.clipYMax = display.getHeight();
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void fillRect(int x, int y, int width, int height) {
        int xMin = Math.max(clipXMin, x);
        int yMin = Math.max(clipYMin, y);
        int xMax = Math.min(x + width, clipXMax);
        int yMax = Math.min(y + height, clipYMax);
        if (xMax <= xMin || yMax <= yMin) {
            return;
        }
        synchronized (display.lock) {
            for (int targetY = yMin; targetY < yMax; targetY++) {
                display.drawHLine(xMin, targetY, xMax - xMin, color, -1);
            }
            display.markModified(xMin, yMin, xMax, yMax);
        }
    }

    public int renderText(int x, int y, String text) {
        int length = text.length();
        int width = 0;
        for (int offset = 0; offset < length; ) {
            int codepoint = text.codePointAt(offset);
            offset += Character.charCount(codepoint);
            width += renderCharacter(x + width, y, codepoint);
        }
        return width;
    }

    /**
     * Renders a single character at the given position.
     * <p>
     * Returns the width of the character in pixel.
     */
    public int renderCharacter(int x, int y, int codepoint) {
        BitmapFont.Glyph glyph = font.getGlyph(codepoint);
        if (glyph == null) {
            return font.getCellWidth();
        }
        int bitOffset = glyph.getData(glyphBuffer);
        int h = font.getCellHeight();
        boolean saveProcessAlpha = processAlpha;
        textPalette[1] = processAlpha ? color : (color | 0xff000000);
        processAlpha = true;
        drawIndexed(x, y - h, font.getCellWidth() * textScaleX, h * textScaleY, glyphBuffer, 1, textPalette, bitOffset, font.getCellWidth(), textScaleX, textScaleY);
        processAlpha = saveProcessAlpha;
        return font.getCellWidth() * textScaleX;
    }

    private void setPixelInternal(int x, int y) {
        if (x >= clipXMin && x < clipXMax && y >= clipYMin && y < clipYMax) {
            display.setPixelInternal(x, y, color);
        }
    }

    private void drawLineLow(int x0, int y0, int x1, int y1) {
        int xMin = Math.max(clipXMin, x0);
        int yMin = Math.max(clipYMin, Math.min(y0, y1));
        int xMax = Math.min(x1 + 1, clipXMax);
        int yMax = Math.min(Math.max(y0, y1) + 1, clipYMax);
        if (xMax <= xMin || yMax <= yMin) {
            return;
        }
        int dx = x1 - x0;
        int dy = y1 - y0;

        int yi = 1;
        if (dy < 0) {
            yi = -1;
            dy = -dy;
        }
        int d = (2 * dy) - dx;
        int y = y0;

        synchronized (display.lock) {
            if (dy == 0) {
                display.drawHLine(xMin, y, xMax - xMin, color, -1);
            } else {
                for (int x = x0; x < xMax; x++) {
                    setPixelInternal(x, y);
                    if (d > 0) {
                        y += yi;
                        d += (2 * (dy - dx));
                    } else {
                        d += 2 * dy;
                    }
                }
            }
            display.markModified(xMin, yMin, xMax, yMax);
        }
    }

    private void drawLineHigh(int x0, int y0, int x1, int y1) {

        int xMin = Math.max(clipXMin, Math.min(x0, x1));
        int yMin = Math.max(clipYMin, y0);
        int xMax = Math.min(Math.max(x0, x1) + 1, clipXMax);
        int yMax = Math.min(y1 + 1, clipYMax);
        if (xMax <= xMin || yMax <= yMin) {
            return;
        }
        int dx = x1 - x0;
        int dy = y1 - y0;
        int xi = 1;
        if (dx < 0) {
            xi = -1;
            dx = -dx;
        }
        int d = (2 * dx) - dy;
        int x = x0;
        synchronized (display.lock) {
            for (int y = y0; y < yMax; y++) {
                setPixelInternal(x, y);
                if (d > 0) {
                    x += xi;
                    d += (2 * (dx - dy));
                } else {
                    d += 2 * dx;
                }
            }
            display.markModified(xMin, yMin, xMax, yMax);
        }
    }

    public void drawLine(int x0, int y0, int x1, int y1) {
        if (Math.abs(y1 - y0) < Math.abs(x1 - x0)) {
            if (x0 > x1) {
                drawLineLow(x1, y1, x0, y0);
            } else {
                drawLineLow(x0, y0, x1, y1);
            }
        } else {
            if (y0 > y1) {
                drawLineHigh(x1, y1, x0, y0);
            } else {
                drawLineHigh(x0, y0, x1, y1);
            }
        }
    }


    public void drawRgb(
            int x, int y, int width, int height, int[] rgbData) {
        drawRgb(x, y, width, height, rgbData, 0, width, 1, 1);
    }

    public void drawRgb(
            int x, int y, int width, int height, int[] rgbData, int offset, int scanLength, int scaleX, int scaleY) {

        int xMin = Math.max(clipXMin, x);
        int yMin = Math.max(clipYMin, y);
        int xMax = Math.min(x + width, clipXMax);
        int yMax = Math.min(y + height, clipYMax);
        if (xMax <= xMin || yMax <= yMin) {
            return;
        }

        synchronized (display.lock) {
            for (int sy = yMin; sy < yMax; sy++) {
                display.drawRgbRow(
                        xMin,
                        sy,
                        xMax - xMin,
                        rgbData,
                        processAlpha,
                        offset + ((sy - y) / scaleY * scanLength) + (xMin - x) / scaleX,
                        scaleX,
                        (xMin - x) % scaleX);
            }
            display.markModified(xMin, yMin, xMax, yMax);
        }
    }

    public void drawIndexed(
            int x, int y, int width, int height, int[] data, int bitCount, int[] palette, int bitOffset, int bitScanLength, int scaleX, int scaleY) {
        if (Integer.highestOneBit(bitCount) != Integer.lowestOneBit(bitCount)) {
            throw new IllegalArgumentException("Bitcount must be a power of 2");
        }

        int xMin = Math.max(clipXMin, x);
        int yMin = Math.max(clipYMin, y);
        int xMax = Math.min(x + width, clipXMax);
        int yMax = Math.min(y + height, clipYMax);
        if (xMax <= xMin || yMax <= yMin) {
            return;
        }

        int bitMask = (1 << bitCount) - 1;
        int len = (xMax - xMin + scaleX - 1) / scaleX;

        if (len > indexedTransferBuffer.length) {
            indexedTransferBuffer = new int[len];
        }

        synchronized (display.lock) {
            for (int sy = yMin; sy < yMax; sy++) {
                if (sy == yMin || (sy - y) % scaleY == 0) {
                    int srcBitPos = bitOffset + ((sy - y) / scaleY * bitScanLength + (xMin - x) / scaleX) * bitCount;
                    int dstPos = 0;
                    for (int i = 0; i < len; i++) {
                        int indices = data[srcBitPos / 32];
                        int bitIndex = srcBitPos % 32;
                        int index = (indices >> (32 - bitCount - bitIndex)) & bitMask;
                        indexedTransferBuffer[dstPos++] = palette[index];
                        srcBitPos += bitCount;
                    }
                }
                display.drawRgbRow(xMin, sy, xMax - xMin, indexedTransferBuffer, processAlpha, 0, scaleX, (xMin - x) % scaleX);
            }
            display.markModified(xMin, yMin, xMax, yMax);
        }
    }

    public void setClip(int x, int y, int width, int height) {
        clipXMin = Math.max(0, x);
        clipYMin = Math.max(0, y);
        clipXMax = Math.min(x + width, display.getWidth());
        clipYMax = Math.min(y + height, display.getHeight());
    }

    public void setFont(BitmapFont font) {
        this.font = font;
    }

    public void setProcessAlpha(boolean processAlpha) {
        this.processAlpha = processAlpha;
    }

    public void setTextScale(int scale) {
        setTextScale(scale, scale);
    }

    public void setTextScale(int scaleX, int scaleY) {
        this.textScaleX = scaleX;
        this.textScaleY = scaleY;
    }
}
