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
 * Waveshare "GamePi13" 1.3 inch LCD hat.
 * <p>
 * All components are created on demand and then cached.
 * <p>
 * https://www.waveshare.com/gamepi13.htm
 * https://www.waveshare.com/wiki/GamePi13
 */
public class GamePi13 implements Closeable {
    private final Context pi4j;
    private GameController controller;
    private GraphicsDisplayDriver displayDriver;
    private GraphicsDisplay display;
    private DigitalOutput rstPin;

    public GamePi13(Context pi4j) {
        this.pi4j = pi4j;
    }

    public GameController getController() {
        if (controller == null) {
            GameController.Builder builder = new GameController.Builder(pi4j);
            builder.addGndSwitch(GameController.Key.RT, 14)
                    .addGndSwitch(GameController.Key.X, 15)
                    .addGndSwitch(GameController.Key.LT, 23)
                    .addGndSwitch(GameController.Key.Y, 12)
                    .addGndSwitch(GameController.Key.LEFT, 16)
                    .addGndSwitch(GameController.Key.B, 20)
                    .addGndSwitch(GameController.Key.A, 21)
                    .addGndSwitch(GameController.Key.UP, 5)
                    .addGndSwitch(GameController.Key.DOWN, 6)
                    .addGndSwitch(GameController.Key.RIGHT, 13)
                    .addGndSwitch(GameController.Key.SELECT, 19)
                    .addGndSwitch(GameController.Key.START, 26);
            controller = builder.build();
        }
        return controller;
    }

    public GraphicsDisplayDriver getDisplayDriver() {
        if (displayDriver == null) {
            rstPin = pi4j.digitalOutput().create(27);
            rstPin.high();
            Spi spi = pi4j.create(SpiConfigBuilder.newInstance(pi4j).bus(0).address(0).baud(St7789Driver.SPI_BAUDRATE));
            DigitalOutput dc = pi4j.create(DigitalOutputConfigBuilder.newInstance(pi4j).address(25));
            displayDriver = new St7789Driver(spi, dc, 240, PixelFormat.RGB_565);
        }
        return displayDriver;
    }

    public GraphicsDisplay getDisplay() {
        if (display == null) {
            display = new GraphicsDisplay(getDisplayDriver());
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
