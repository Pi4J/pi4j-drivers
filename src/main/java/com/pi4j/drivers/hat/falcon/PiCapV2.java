package com.pi4j.drivers.hat.falcon;

import java.io.Closeable;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.ssd1306.Ssd1306Driver;
import com.pi4j.drivers.sensor.geospatial.mcp7941x.Mcp7941xDriver;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfigBuilder;

import com.pi4j.drivers.input.GameController;

/**
 * Configured drivers for the display and game controls on a Falcon PiCap V2
 * https://pixelcontroller.com/store/featured/99-falcon-picap-v2.html other i2c device at: 50 == 24c256 - eeprom -
 * contains licenses for DPIPixels
 */

public class PiCapV2 implements Closeable {

    private final Context pi4j;
    private GameController controller;
    private GraphicsDisplayDriver displayDriver;
    private GraphicsDisplay display;
    private Mcp7941xDriver realTimeClock;

    public PiCapV2(Context pi4j) {
        this.pi4j = pi4j;
    }

    public GameController getController() {
        if (controller == null) {
            GameController.Builder builder = new GameController.Builder(pi4j);
            builder.addGndSwitch(GameController.Key.DOWN, 22).addGndSwitch(GameController.Key.UP, 23)
                    .addGndSwitch(GameController.Key.ENTER, 24).addGndSwitch(GameController.Key.TEST, 25)
                    .addGndSwitch(GameController.Key.BACK, 27);
            controller = builder.build();
        }
        return controller;
    }

    public GraphicsDisplayDriver getDisplayDriver() {
        if (displayDriver == null) {
            I2C i2c = pi4j.create(I2CConfigBuilder.newInstance(pi4j).bus(1).device(0x3c).build());
            displayDriver = new Ssd1306Driver(i2c);
        }
        return displayDriver;
    }

    public GraphicsDisplay getDisplay() {
        if (display == null) {
            display = new GraphicsDisplay(getDisplayDriver());
        }
        return display;
    }

    public Mcp7941xDriver getRealTimeClock() {
        if (realTimeClock == null) {
            I2C i2c = pi4j.create(I2CConfigBuilder.newInstance(pi4j).bus(1).device(0x6f).build());
            realTimeClock = new Mcp7941xDriver(i2c);
        }
        return realTimeClock;
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
