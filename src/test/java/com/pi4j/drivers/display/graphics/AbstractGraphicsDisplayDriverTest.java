package com.pi4j.drivers.display.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.drivers.display.BitmapFont;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;

public abstract class AbstractGraphicsDisplayDriverTest {
    private Context pi4j;

    @BeforeEach
    public void setUp() {
        pi4j = Pi4J.newAutoContext();
    }

    @AfterEach
    public void tearDown() {
        pi4j.shutdown();
    }

    public abstract GraphicsDisplayDriver createDriver(Context pi4j);

    @Test
    public void testFillRect() throws InterruptedException {
        GraphicsDisplay display = new GraphicsDisplay(createDriver(pi4j));
        display.setTransferDelayMillis(0);
        int width = display.getWidth();
        int height = display.getHeight();
        display.fillRect(0, 0, width, height, 0x0);
        Random random = new Random(0);
        for (int i = 0; i < 10; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int w = random.nextInt(width - x);
            int h = random.nextInt(height - y);
            int color = random.nextInt(0xffffff);
            display.fillRect(x, y, w, h, color);
        }
        display.close();
    }

    // Text makes rotation (bugs) quite obvious, so we use this to test both.
    @Test
    public void testBitmapFont0_100() throws InterruptedException {
        renderBitmapFont(GraphicsDisplay.Rotation.ROTATE_0, 100);
    }

    @Test
    public void testBitmapFont0() throws InterruptedException {
        renderBitmapFont(GraphicsDisplay.Rotation.ROTATE_0, 0);
    }

    @Test
    public void testBitmapFont90_100() throws InterruptedException {
        renderBitmapFont(GraphicsDisplay.Rotation.ROTATE_90, 100);
    }

    @Test
    public void testBitmapFont90_0() throws InterruptedException {
        renderBitmapFont(GraphicsDisplay.Rotation.ROTATE_90, 0);
    }

    @Test
    public void testBitmapFont180_100() throws InterruptedException {
        renderBitmapFont(GraphicsDisplay.Rotation.ROTATE_180, 100);
    }

    @Test
    public void testBitmapFont180_0() throws InterruptedException {
        renderBitmapFont(GraphicsDisplay.Rotation.ROTATE_180, 0);
    }

    @Test
    public void testBitmapFont270() throws InterruptedException {
        renderBitmapFont(GraphicsDisplay.Rotation.ROTATE_270, 100);
    }

    @Test
    public void testBitmapFont270_0() throws InterruptedException {
        renderBitmapFont(GraphicsDisplay.Rotation.ROTATE_270, 0);
    }

    private void renderBitmapFont(GraphicsDisplay.Rotation rotation, int transferDelay) throws InterruptedException {
        GraphicsDisplay display = new GraphicsDisplay(createDriver(pi4j), rotation);
        display.setTransferDelayMillis(transferDelay);
        int width = display.getWidth();
        int height = display.getHeight();
        display.fillRect(0, 0, width, height, 0);

        BitmapFont font = BitmapFont.get5x8Font();
        BitmapFont proportionalFont = BitmapFont.get5x10Font(BitmapFont.Option.PROPORTIONAL);

        int textWidth = display.renderText(1, 8, "Hello Pi4J Monospaced", font, 0xff8888);
        assertEquals("Hello Pi4J Monospaced".length() * 6, textWidth);
        display.renderText(1, 50, "Hello Pi4j-gpqy", proportionalFont, 0x88ff88, 2, 3);
        display.renderText(1, 100, "Hello Pi4J 3/4x", proportionalFont, 0x8888ff, 3, 4);
        display.renderText(1, 180, rotation.name(), proportionalFont, 0xffff88, 4, 7);

        Thread.sleep(100);

        display.close();
    }

    /**
     * Renders rainbow colors from red on the left to violet on the right
     * with the brightness starting at 0.1 at the top, increasing to 1 at the bottom
     * <p>
     * This should allow for checking color, orientation and brightness correctness.
     */
    @Test
    @Disabled
    public void testSetPixel() throws InterruptedException {
        GraphicsDisplay display = new GraphicsDisplay(createDriver(pi4j));
        display.setTransferDelayMillis(0);
        int width = display.getWidth();
        int height = display.getHeight();
        display.fillRect(0, 0, width, height, java.awt.Color.WHITE.getRGB() );

        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                float brightness = (0.8f * y) / height + 0.2f;
                display.setPixel(x, y,
                        Argb32.fromHsl((360f * x) / width, 1, brightness)
                );
            }
            Thread.sleep(5);
        }

        display.close();
    }
}
