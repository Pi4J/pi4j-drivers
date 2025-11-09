package com.pi4j.drivers.hat.waveshare;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.PixelFormat;
import com.pi4j.drivers.display.graphics.st7789.St7789Driver;
import com.pi4j.drivers.input.GameController;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfigBuilder;

import java.io.Closeable;
import java.io.IOException;

/**
 * Configured drivers for the display and game controls on a
 * Waveshare 1.3 inch LCD hat.
 * <p>
 * All components are created on demand and then cached.
 * <p>
 * https://www.waveshare.com/1.3inch-lcd-hat.htm
 * https://www.waveshare.com/wiki/1.3inch_LCD_HAT
 */
public class Waveshare14972 implements Closeable {
    private final Context pi4j;
    private GameController controller;
    private GraphicsDisplayDriver displayDriver;
    private GraphicsDisplay display;
    private DigitalOutput rstPin;

    public Waveshare14972(Context pi4j) {
        this.pi4j = pi4j;
    }

    public GameController getController() {
        if (controller == null) {
            GameController.Builder builder = new GameController.Builder(pi4j);
            builder.addGndSwitch(GameController.Key.KEY_1, 21)
                    .addGndSwitch(GameController.Key.KEY_2, 20)
                    .addGndSwitch(GameController.Key.KEY_3, 16)
                    .addGndSwitch(GameController.Key.UP, 6)
                    .addGndSwitch(GameController.Key.DOWN, 19)
                    .addGndSwitch(GameController.Key.LEFT, 5)
                    .addGndSwitch(GameController.Key.RIGHT, 26)
                    .addGndSwitch(GameController.Key.CENTER, 13);
            controller = builder.build();
        }
        return controller;
    }

    public GraphicsDisplayDriver getDisplayDriver() {
        if (displayDriver == null) {
            rstPin = pi4j.digitalOutput().create(27);
            rstPin.high();
            Spi spi = pi4j.create(SpiConfigBuilder.newInstance(pi4j).bus(0).bcm(0).baud(St7789Driver.SPI_BAUDRATE));
            DigitalOutput dc = pi4j.create(DigitalOutputConfigBuilder.newInstance(pi4j).bcm(25));
            displayDriver = new St7789Driver(spi, dc, 240, PixelFormat.RGB_565);
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
    public void close() throws IOException {
        if (controller != null) {
            controller.close();
        }
        if (display != null) {
            display.close();
        } else if (displayDriver != null) {
            displayDriver.close();
        }
        if (rstPin != null) {
            rstPin.close();
        }
    }
}
