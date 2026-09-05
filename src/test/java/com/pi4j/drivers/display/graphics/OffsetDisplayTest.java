package com.pi4j.drivers.display.graphics;

import java.io.IOException;

import com.pi4j.drivers.display.graphics.GraphicsDisplay.Rotation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class OffsetDisplayTest {

    @Test
    public void testBaseline() {
        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_444);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.setTransferDelayMillis(0);
    	display.attachDriver(0, 0, driver, Rotation.ROTATE_0);
    }

    @Test
    public void testNegative444() {
        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_444);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.setTransferDelayMillis(0);
        try {
            display.attachDriver(-10, -10, driver, Rotation.ROTATE_0);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testPositive444() {
        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_444);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.setTransferDelayMillis(0);
        try {
            display.attachDriver(10, 10, driver, Rotation.ROTATE_0);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testX444() {
        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_444);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.setTransferDelayMillis(0);
        try {
            display.attachDriver( 1, 0, driver, Rotation.ROTATE_0 );
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testY444() {
        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_444);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.setTransferDelayMillis(0);
        try {
            display.attachDriver(0, 1, driver, Rotation.ROTATE_0);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testNegative888() {
        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_888);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.setTransferDelayMillis(0);
        display.attachDriver( -10, -10, driver, Rotation.ROTATE_0 );
    }

    @Test
    public void testPositive888() {
        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_888);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.setTransferDelayMillis(0);
        display.attachDriver( 10, 10, driver, Rotation.ROTATE_0 );
    }

    @Test
    public void testX888() {
        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_888);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.setTransferDelayMillis(0);
        display.attachDriver( 1, 0, driver, Rotation.ROTATE_0 );
    }

    @Test
    public void testY888() {
        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_888);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.setTransferDelayMillis(0);
        display.attachDriver( 0, 1, driver, Rotation.ROTATE_0 );
    }
}
