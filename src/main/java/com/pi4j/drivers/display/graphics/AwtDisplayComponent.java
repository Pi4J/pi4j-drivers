package com.pi4j.drivers.display.graphics;

import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AwtDisplayComponent extends BaseDisplayComponent {

    private static Logger log = LoggerFactory.getLogger(AwtDisplayComponent.class);

    public AwtDisplayComponent(DisplayDriver driver) {
        super(driver);
    }

    public void display(BufferedImage rawImg) {

        log.debug("display: {} {} x {}", rawImg.getType(), rawImg.getWidth(), rawImg.getHeight());
        DisplayInfo displayInfo = driver.getDisplayInfo();

        // clip the image to what will fit on the display
        BufferedImage img = rawImg.getSubimage(0, 0, Math.min(rawImg.getWidth(), displayInfo.getWidth()),
                Math.min(rawImg.getHeight(), displayInfo.getHeight()));

        int[] rgb888pixels = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        drawImage(0, 0, img.getWidth(), img.getHeight(), rgb888pixels);
    }
}
