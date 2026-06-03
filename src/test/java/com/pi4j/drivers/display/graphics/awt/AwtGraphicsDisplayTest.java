package com.pi4j.drivers.display.graphics.awt;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

import java.io.IOException;

import com.pi4j.drivers.display.graphics.FakeGraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.PixelFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AwtGraphicsDisplayTest {

    private static Logger log = LoggerFactory.getLogger(AwtGraphicsDisplayTest.class);

    // 12 bit test
    @Test
    public void testRgb888toRgb444() throws IOException {

        FakeGraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(10, 10, PixelFormat.RGB_444);
        GraphicsDisplay display = new GraphicsDisplay(driver);
        display.setTransferDelayMillis(0);

        BufferedImage img = new BufferedImage(12, 12, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = img.createGraphics();

        g2d.setPaint(java.awt.Color.RED);
        g2d.fillRect(0, 0, img.getWidth(), img.getHeight());
        g2d.dispose();
        AwtGraphics.drawImage(display.getGraphics(), 0, 0, img);

        byte[] data = driver.getData();

        log.trace("Size of Data: {}", data.length);
        assertEquals((byte) 0xF0, data[0], "First pixel is unexpected");
        assertEquals(10 * 10 * 12 / 8, data.length, "Length of data is unexpected");
    }

    // 16 bit test
    @Test
    public void testRgb888toRgb565() throws IOException {

        FakeGraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(10, 10, PixelFormat.RGB_565);
        GraphicsDisplay mockDisplay = new GraphicsDisplay(driver);
        mockDisplay.setTransferDelayMillis(0);

        BufferedImage img = new BufferedImage(12, 12, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = img.createGraphics();

        g2d.setPaint(java.awt.Color.RED);
        g2d.fillRect(0, 0, img.getWidth(), img.getHeight());
        g2d.dispose();
        AwtGraphics.drawImage(mockDisplay.getGraphics(), 0, 0, img);

        byte[] data = driver.getData();

        log.trace("Size of Data: {}", data.length);
        assertEquals((byte) 0xF8, data[0], "First pixel is unexpected");
        assertEquals(10 * 10 * 16 / 8, data.length, "Length of data is unexpected");
    }

    @Test
    public void testDisplayDataBufferInt444() {

        FakeGraphicsDisplayDriver display = new FakeGraphicsDisplayDriver(10, 10, PixelFormat.RGB_444);
        GraphicsDisplay mockDisplay = new GraphicsDisplay(display);
        mockDisplay.setTransferDelayMillis(0);

        BufferedImage img = makeDataBufferInt();
        AwtGraphics.drawImage(mockDisplay.getGraphics(), 0, 0, img);

        byte[] data = display.getData();

        log.trace("Size of Data: {}", data.length);
        assertEquals(10 * 10 * 12 / 8, data.length, "Length of data is unexpected");
        assertEquals((byte) 0xF0, data[0], "First pixel is unexpected");
    }

    @Test
    public void testDisplayDataBufferInt565() {

        FakeGraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(10, 10, PixelFormat.RGB_565);
        GraphicsDisplay mockDisplay = new GraphicsDisplay(driver);
        mockDisplay.setTransferDelayMillis(0);

        BufferedImage img = makeDataBufferInt();
        AwtGraphics.drawImage(mockDisplay.getGraphics(), 0, 0, img);

        byte[] data = driver.getData();

        assertEquals(10 * 10 * 16 / 8, data.length, "Length of data is unexpected");
        assertEquals((byte) 0xF8, data[0], "First pixel is unexpected");
    }

    private BufferedImage makeDataBufferInt() {
        int width = 10, height = 10;

        // Allocate pixel array
        int[] pixels = new int[width * height];

        // Wrap it in DataBufferInt
        DataBufferInt dataBuffer = new DataBufferInt(pixels, pixels.length);

        // Define how pixels are laid out
        int[] bandMasks = { 0x00FF0000, 0x0000FF00, 0x000000FF }; // R, G, B
        SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, width, height, bandMasks);

        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, null);

        ColorModel colorModel = new DirectColorModel(24, 0x00FF0000, 0x0000FF00, 0x000000FF);

        BufferedImage image = new BufferedImage(colorModel, raster, false, null);

        // Example: set one pixel red
        pixels[0] = 0x00FF0000;
        pixels[1] = 0x00FF0000;
        pixels[2] = 0x00FF0000;
        pixels[3] = 0x00FF0000;

        return image;
    }
}
