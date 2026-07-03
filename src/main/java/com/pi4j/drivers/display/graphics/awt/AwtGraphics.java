package com.pi4j.drivers.display.graphics.awt;

import com.pi4j.drivers.display.graphics.Graphics;

import java.awt.image.BufferedImage;

public class AwtGraphics  {

    public static void drawImage(Graphics graphics, int x, int y, BufferedImage image) {
        int[] rgb888pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        graphics.drawRgb(x, y, image.getWidth(), image.getHeight(), rgb888pixels);
    }
}
