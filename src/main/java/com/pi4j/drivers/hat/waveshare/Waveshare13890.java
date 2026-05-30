package com.pi4j.drivers.hat.waveshare;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.sh1106.Sh1106Driver;
import com.pi4j.drivers.input.GameController;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfigBuilder;

import java.io.Closeable;

/**
 * Configured drivers for the display and game controls on a
 * Waveshare 1.3 inch 128x64 OLED hat.
 * <p>
 * All components are created on demand and then cached.
 * <p>
 * https://www.waveshare.com/product/ai/displays/oled/1.3inch-oled-hat.htm
 * https://www.waveshare.com/wiki/1.3inch_OLED_HAT
 */
public class Waveshare13890 implements Closeable {
    private final Context pi4j;
    private GameController controller;
    private GraphicsDisplayDriver displayDriver;
    private GraphicsDisplay display;

    public Waveshare13890(Context pi4j) {
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
            Spi spi = pi4j.create(SpiConfigBuilder.newInstance(pi4j).bus(0).channel(0));
            DigitalOutput rst = pi4j.create(DigitalOutputConfigBuilder.newInstance(pi4j).bcm(25));
            DigitalOutput dc = pi4j.create(DigitalOutputConfigBuilder.newInstance(pi4j).bcm(24));

            displayDriver = new Sh1106Driver(spi, dc, rst);
        }
        return displayDriver;
    }

    public GraphicsDisplay getDisplay() {
        if (display == null) {
            display = new GraphicsDisplay(getDisplayDriver(), GraphicsDisplay.Rotation.ROTATE_180);
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