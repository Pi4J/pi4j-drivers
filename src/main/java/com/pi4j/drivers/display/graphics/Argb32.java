package com.pi4j.drivers.display.graphics;

/** Support functions for ARGB integers. */
public class Argb32 {

    /**
     * Returns an ARGB integer for the given red, green and blue channels ranging from 0 to 255 each.
     * The alpha channel is set to 255.
     */
    public static int fromRgb(int r, int g, int b) {
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    /** Returns a rgb integer for the given red, green and blue channels ranging from 0f to 1f each. */
    public static int fromRgb(float r, float g, float b) {
        return fromRgb((int) (255 * r), (int) (255 * g), (int) (255 * b));
    }

    /** Returns a rgb integer for the given hue (0..360), saturation (0..1) and lightness (0..1) values. */
    public static int fromHsl(float hue, float saturation, float lightness) {
        float hue6 = (hue % 360) / 60;
        float c = (1f - Math.abs(2 * lightness - 1)) * saturation;
        float x = c * (1 - Math.abs(hue6 % 2 - 1));
        float m = lightness - c / 2;
        c += m;
        x += m;
        return switch ((int) hue6) {
            case 0 -> fromRgb(c, x, m);
            case 1 -> fromRgb(x, c, m);
            case 2 -> fromRgb(m, c, x);
            case 3 -> fromRgb(m, x, c);
            case 4 -> fromRgb(x, m, c);
            case 5 -> fromRgb(c, m, x);
            default -> 0;
        };
    }
}
