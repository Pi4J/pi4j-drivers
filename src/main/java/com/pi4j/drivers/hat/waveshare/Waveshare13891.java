package com.pi4j.drivers.hat.waveshare;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.PixelFormat;
import com.pi4j.drivers.display.graphics.st77xx.St77xxDriver;
import com.pi4j.drivers.input.GameController;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfigBuilder;

import java.io.Closeable;

/**
 * Configured drivers for the display and game controls on a
 * Waveshare 1.44 inch 128x128 LCD hat.
 * <p>
 * All components are created on demand and then cached.
 * <p>
 * https://www.waveshare.com/1.44inch-lcd-hat.htm
 * https://www.waveshare.com/wiki/1.44inch_LCD_HAT
 */
public class Waveshare13891 implements Closeable {
    private final Context pi4j;
    private GameController controller;
    private GraphicsDisplayDriver displayDriver;
    private GraphicsDisplay display;

    public Waveshare13891(Context pi4j) {
        this.pi4j = pi4j;
    }

    public GameController getController() {
        if (controller == null) {
            controller = JoystickHatGameControllerFactory.create(pi4j);
        }
        return controller;
    }

    public GraphicsDisplayDriver getDisplayDriver() {
        if (displayDriver == null) {
            DigitalOutput rstPin = pi4j.create(DigitalOutputConfigBuilder.newInstance(pi4j).bcm(27));
            Spi spi = pi4j.create(SpiConfigBuilder.newInstance(pi4j).bus(0).channel(0).baud(St77xxDriver.ST_7735_SPI_BAUDRATE));
            DigitalOutput dc = pi4j.create(DigitalOutputConfigBuilder.newInstance(pi4j).bcm(25));
            displayDriver = new St77xxDriver(spi, dc, rstPin, PixelFormat.RGB_565, false, 128, 128, 2, 3);
        }
        return displayDriver;
    }

    public GraphicsDisplay getDisplay() {
        if (display == null) {
            display = new GraphicsDisplay(getDisplayDriver(), GraphicsDisplay.Rotation.ROTATE_270);
        }
        return display;
    }

    @Override
    public void close() {
        if (controller != null) {
            controller.close();
        }
        if (display != null) {
            display.close();
        } else if (displayDriver != null) {
            displayDriver.close();
        }
    }
}
