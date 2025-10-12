package com.pi4j.drivers.display.graphics;

import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AwtGraphicsDisplay extends GraphicsDisplay {

    private static Logger log = LoggerFactory.getLogger(AwtGraphicsDisplay.class);

    public AwtGraphicsDisplay(GraphicsDisplayDriver driver) {
        super(driver);
    }

    public void display(BufferedImage rawImg) {

        log.debug("display: {} {} x {}", rawImg.getType(), rawImg.getWidth(), rawImg.getHeight());

        // clip the image to what will fit on the display
        BufferedImage img = rawImg.getSubimage(0, 0, Math.min(rawImg.getWidth(), getWidth()),
                Math.min(rawImg.getHeight(), getHeight()));

        int[] rgb888pixels = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        drawImage(0, 0, img.getWidth(), img.getHeight(), rgb888pixels);
    }
}
