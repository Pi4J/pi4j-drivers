package com.pi4j.drivers.display.graphics;

import java.io.IOException;

import com.pi4j.drivers.display.graphics.GraphicsDisplay.Rotation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OffsetDisplayTest {

    @Test
    public void testBaseline() {

        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_444);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
	display.attachDriver( 0, 0, driver, Rotation.ROTATE_0 );
    }

    @Test
    public void testNegative() {

        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_444);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.attachDriver( -10, -10, driver, Rotation.ROTATE_0 );
    }

    @Test
    public void testPositive() {

        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_444);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.attachDriver( 10, 10, driver, Rotation.ROTATE_0 );
    }

    @Test
    public void testX() {

        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_444);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.attachDriver( 1, 0, driver, Rotation.ROTATE_0 );
    }

    @Test
    public void testY() {

        GraphicsDisplayDriver driver = new FakeGraphicsDisplayDriver(100, 100, PixelFormat.RGB_444);
        GraphicsDisplay display = new GraphicsDisplay(100,100);
        display.attachDriver( 0, 1, driver, Rotation.ROTATE_0 );
    }
}
