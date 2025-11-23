package com.pi4j.drivers.display.graphics;

import java.awt.Color;
import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GraphicsDisplayTest {

    // 12 bit test
    @Test
    public void testRgb888toRgb444() throws IOException {
        FakeGraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_444);
        GraphicsDisplay display = new GraphicsDisplay(driver);
        display.fillRect(0, 0, 48, 1, Color.RED.getRGB());
        display.flush();

        byte[] data = driver.getData();

        assertEquals((byte) 0xF0, data[0]);
        assertEquals(100 * 100 * 3 / 2, data.length);
    }

    // 16 bit test
    @Test
    public void testRgb888toRgb565() throws IOException {
        FakeGraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_565);
        GraphicsDisplay display = new GraphicsDisplay(driver);
        display.setTransferDelayMillis(0);
        display.fillRect(0, 0, 48, 1, Color.RED.getRGB());

        byte[] data = driver.getData();

        assertEquals(100 * 100 * 2, data.length);
        assertEquals((byte) 0xF8, data[0]);
    }

    @Test
    public void testSetPixel() {
        FakeGraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_888);
        GraphicsDisplay display = new GraphicsDisplay(driver);
        display.setTransferDelayMillis(0);
        display.setPixel(10, 10, 0x112233);

        byte[] data = driver.getData();
        assertEquals(100 * 100 * 3, data.length);

        int pos = (10 * 100 + 10) * 3;
        assertEquals(0x11, data[pos]);
        assertEquals(0x22, data[pos+1]);
        assertEquals(0x33, data[pos+2]);
    }

    @Test
    public void fullscreen() {
        FakeGraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(128, 128, PixelFormat.RGB_888);
        GraphicsDisplay display = new GraphicsDisplay(driver);

        fillGradient(display, 0, 0, 128, 128, Mode.PIXEL, 1, 0);
        display.flush();
        driver.assertGradient(0, 0, 1,0, 0, 1);

        fillGradient(display, 0, 0, 128, 128, Mode.PIXEL, 1000, 0);
        display.flush();
        driver.assertGradient(0, 0, 1,0, 0, 1);

        fillGradient(display, 0, 0, 128, 128, Mode.PIXEL, 100, 100);
        display.flush();
        driver.assertGradient(0, 0, 1,0, 0, 1);

        fillGradient(display, 0, 0, 128, 128, Mode.RECT, 1, 0);
        display.flush();
        driver.assertGradient(0, 0, 1,0, 0, 1);

        fillGradient(display, 0, 0, 128, 128, Mode.IMAGE, 1, 0);
        display.flush();
        driver.assertGradient(0, 0, 1,0, 0, 1);
    }

    @Test
    public void fullscreenRotated() {
        FakeGraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(128, 128, PixelFormat.RGB_888);
        GraphicsDisplay display = new GraphicsDisplay(driver, GraphicsDisplay.Rotation.ROTATE_90);

        fillGradient(display, 0, 0, 128, 128, Mode.PIXEL, 1, 0);
        display.flush();
        driver.assertGradient(127, 127, 0, -1, -1, 0);

        fillGradient(display, 0, 0, 128, 128, Mode.PIXEL, 1000, 0);
        display.flush();
        driver.assertGradient(127, 127, 0, -1, -1, 0);

        fillGradient(display, 0, 0, 128, 128, Mode.PIXEL, 100, 100);
        display.flush();
        driver.assertGradient(127, 127, 0, -1, -1, 0);

        fillGradient(display, 0, 0, 128, 128, Mode.RECT, 1, 0);
        display.flush();
        driver.assertGradient(127, 127, 0, -1, -1, 0);

        fillGradient(display, 0, 0, 128, 128, Mode.IMAGE, 1, 0);
        display.flush();
        driver.assertGradient(127, 127, 0, -1, -1, 0);
    }


    @Test
    public void multiScreen() {
        FakeGraphicsDisplayDriver driver0 = new FakeGraphicsDisplayDriver(64, 64, PixelFormat.RGB_888);
        FakeGraphicsDisplayDriver driver1 = new FakeGraphicsDisplayDriver(64, 64, PixelFormat.RGB_888);
        FakeGraphicsDisplayDriver driver2 = new FakeGraphicsDisplayDriver(64, 128, PixelFormat.RGB_888);
        GraphicsDisplay display = new GraphicsDisplay(128, 128);
        display.attachDriver(0, 0, driver0, GraphicsDisplay.Rotation.ROTATE_0);
        display.attachDriver(64, 0, driver0, GraphicsDisplay.Rotation.ROTATE_0);
        display.attachDriver(0, 64, driver0, GraphicsDisplay.Rotation.ROTATE_0);

        fillGradient(display, 0, 0, 128, 128, Mode.PIXEL, 1, 0);
        display.flush();
        driver0.assertGradient(0, 0, 1, 0, 0, 1);
        driver1.assertGradient(64, 0, 1, 0, 0, 1);
        driver2.assertGradient(0, 64, 1, 0, 0, 1);
    }



    // Helpers

    private void fillGradient(GraphicsDisplay display, int x0, int y0, int width, int height, Mode mode, int flush, int random) {
        int count = 0;
        int[] image = new int[1];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = FakeGraphicsDisplayDriver.testColor(x, y);
                switch (mode) {
                    case Mode.RECT ->
                        display.fillRect(x0 + x, y0 + y, 1, 1, color);
                    case Mode.PIXEL ->
                        display.setPixel(x0 + x, y0 + y, color);
                    case Mode.IMAGE -> {
                        image[0] = color;
                        display.drawImage(x0 + x, y0 + y, 1, 1, image);
                    }
                }
                if (++count >= flush) {
                    display.flush();
                    count = 0;
                }
            }
        }

        Random generator = new Random();
        for (int i = 0; i < random; i++) {
            int x = generator.nextInt(width);
            int y = generator.nextInt(height);
            int color = FakeGraphicsDisplayDriver.testColor(x, y);
            display.setPixel(x, y, color);
            if (++count >= flush) {
                display.flush();
                count = 0;
            }
        }

    }

    enum Mode {
        PIXEL, RECT, IMAGE
    }

}
