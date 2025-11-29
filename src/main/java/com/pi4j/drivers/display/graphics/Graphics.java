package com.pi4j.drivers.display.graphics;

import com.pi4j.drivers.display.BitmapFont;

/** A simple graphics context similar to MIDP's LCDUI graphics. */
public class Graphics {
    private final GraphicsDisplay display;
    private int color = 0xffffffff;
    private BitmapFont font = BitmapFont.get5x8Font();
    private int textScaleX = 1;
    private int textScaleY = 1;

    Graphics(GraphicsDisplay display) {
        this.display = display;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void fillRect(int x, int y, int width, int height) {
        display.fillRect(x, y, width, height, color);
    }

    public void drawString(String text, int x, int y) {
        display.renderText(x, y, text, font, color, textScaleX, textScaleY);
    }

    public void drawRgb(int[] rgbData, int offset, int scanLength, int x, int y, int width, int height) {
        synchronized (display.lock) {
            int xMin = Math.max(0, x);
            int yMin = Math.max(0, y);
            int xMax = Math.min(x + width, display.getWidth());
            int yMax = Math.min(y + height, display.getHeight());
            if (xMax <= xMin || yMax <= yMin) {
                return;
            }
            for (int targetY = yMin; targetY < yMax; targetY++) {
                System.arraycopy(
                        rgbData,
                        offset + (targetY - y) * scanLength + xMin - x,
                        display.displayBuffer,
                        display.pixelAddress(xMin, targetY),
                        xMax - xMin);
            }
            display.markModified(xMin, yMin, xMax, yMax);
        }
    }

    public void setFont(BitmapFont font) {
        this.font = font;
    }

    public void setTextScale(int scale) {
        setTextScale(scale, scale);
    }

    public void setTextScale(int scaleX, int scaleY) {
        this.textScaleX = scaleX;
        this.textScaleY = scaleY;
    }
}
