package com.pi4j.drivers.display;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseGraphicsDisplayComponent {

    private static Logger log = LoggerFactory.getLogger(BaseGraphicsDisplayComponent.class);

    protected final GraphicsDisplayDriver driver;

    public BaseGraphicsDisplayComponent(GraphicsDisplayDriver driver) {
        this.driver = driver;
    }

    // it is possible that rgb888pixels contains more than will fit
    // we will just ignore them
    public void drawImage(int x, int y, int width, int height, int[] rgb888pixels) {
        log.debug("drawImage: {},{} \t {} x {} \t {}", x, y, width, height, rgb888pixels.length);
        PixelFormat pixelFormat = driver.getDisplayInfo().getPixelFormat();

        byte[] data = new byte[(width * pixelFormat.getBitCount() + 7) / 8];
        for (int i = 0; i < height; i++) {
            pixelFormat.writeRgb(rgb888pixels, width * i, data, 0, width);
            driver.setPixels(x, y + i, width, 1, data);
        }
    }

    public void fillRect(int x, int y, int width, int height, int rgb888) throws java.io.IOException {
        PixelFormat pixelFormat = driver.getDisplayInfo().getPixelFormat();

        byte[] data = new byte[(width * pixelFormat.getBitCount() + 7) / 8];
        pixelFormat.fillRgb(data, 0, width, rgb888);

        for (int i = 0; i < height; i++) {
            driver.setPixels(x, y + i, width, 1, data);
        }
    }
}
