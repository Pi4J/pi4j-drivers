package com.pi4j.drivers.hat.raspberry;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.framebuffer.FramebufferDriver;
import com.pi4j.drivers.display.BitmapFont;
import com.pi4j.drivers.display.graphics.Graphics;
import com.pi4j.drivers.input.GameController;
import com.pi4j.drivers.input.linux.LinuxInputDriver;
import com.pi4j.drivers.sensor.Sensor;
import com.pi4j.drivers.sensor.environment.hts221.Hts221Driver;
import com.pi4j.drivers.sensor.environment.lps25h.Lps25hDriver;
import com.pi4j.drivers.sensor.environment.tcs3400.Tcs3400Driver;
import com.pi4j.drivers.sensor.geospatial.lsm9ds1.Lsm9ds1Driver;
import com.pi4j.drivers.sensor.geospatial.lsm9ds1.Lsm9ds1MagnetometerDriver;
import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.i2c.I2C;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

public class SenseHat {
    private final Context pi4j;
    private GameController controller;
    private GraphicsDisplayDriver displayDriver;
    private GraphicsDisplay display;
    private LinuxInputDriver inputDriver;
    private Hts221Driver hts221Driver;
    private Lps25hDriver lps25hDriver;
    private Tcs3400Driver tcs3400Driver;
    private Lsm9ds1Driver lsm9ds1Driver;
    private Lsm9ds1MagnetometerDriver lsm9ds1MagnetometerDriver;

    private final ListenableOnOffRead.Impl up = new ListenableOnOffRead.Impl();
    private final ListenableOnOffRead.Impl down = new ListenableOnOffRead.Impl();
    private final ListenableOnOffRead.Impl left = new ListenableOnOffRead.Impl();
    private final ListenableOnOffRead.Impl right = new ListenableOnOffRead.Impl();
    private final ListenableOnOffRead.Impl center = new ListenableOnOffRead.Impl();

    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;
    private static final int BLACK = 0x000000;
    private static final int WHITE = 0xffffff;

    public SenseHat(Context pi4j) {
        this.pi4j = pi4j;
    }

    public LinuxInputDriver getInputDriver() {
        if (inputDriver == null) {
            inputDriver = LinuxInputDriver.forSenseHat();
        }
        return inputDriver;
    }

    public GameController getController() {
        if (controller == null) {
            LinuxInputDriver inputDriver = getInputDriver();
            inputDriver.addListener(this::handleEvent);
            controller = new GameController.Builder(pi4j)
                    .addDigitalInput(GameController.Key.DOWN, down)
                    .addDigitalInput(GameController.Key.LEFT, left)
                    .addDigitalInput(GameController.Key.RIGHT, right)
                    .addDigitalInput(GameController.Key.UP, up)
                    .addDigitalInput(GameController.Key.CENTER, center)
                    .build();
        }
        return controller;
    }

    public Hts221Driver getHumiditySensor()  {
        if (hts221Driver == null) {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(1).device(Hts221Driver.I2C_ADDRESS));
            hts221Driver = new Hts221Driver(i2c);
        }
        return hts221Driver;
    }

    public Lps25hDriver getPressureSensor()  {
        if (lps25hDriver == null) {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(1).device(Lps25hDriver.I2C_ADDRESS));
            lps25hDriver = new Lps25hDriver(i2c);
        }
        return lps25hDriver;
    }

    public Tcs3400Driver getLightSensor()  {
        if (tcs3400Driver == null) {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(1).device(Tcs3400Driver.I2C_ADDRESS));
            tcs3400Driver = new Tcs3400Driver(i2c);
        }
        return tcs3400Driver;
    }

    public Lsm9ds1Driver getAccelerometer() {
        if (lsm9ds1Driver == null) {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(1).device(Lsm9ds1Driver.I2C_ADDRESS_0));
            lsm9ds1Driver = new Lsm9ds1Driver(i2c);
        }
        return lsm9ds1Driver;
    }

    public Lsm9ds1MagnetometerDriver getMagnetometer() {
        if (lsm9ds1MagnetometerDriver == null) {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(1).device(Lsm9ds1MagnetometerDriver.I2C_ADDRESS_0));
            lsm9ds1MagnetometerDriver = new Lsm9ds1MagnetometerDriver(i2c);
        }
        return lsm9ds1MagnetometerDriver;
    }

    public float getHumidity() {
        return getHumiditySensor().readHumidity();
    }

    public double getPressure() {
        return getPressureSensor().readPressure();
    }

    public double getTemperature() {
        return getHumiditySensor().readTemperature() + 
        getPressureSensor().readTemperature() / 2.0;
    }

    public double[] readAccelerometer() {
        return getAccelerometer().readAccelerometer();
    }

    public double[] readGyroscope() {
        return getAccelerometer().readGyroscope();
    }

    public double[] readMagneticField() {
        return getMagnetometer().readMagneticField();
    }

    public List<Sensor> getAllSensors() {
        List<Sensor> result = new ArrayList<>();
        result.add(getHumiditySensor());
        result.add(getPressureSensor());
        result.add(getLightSensor());
        result.add(getAccelerometer());
        result.add(getMagnetometer());
        return result;
    }

    public GraphicsDisplayDriver getDisplayDriver() {
        if (displayDriver == null) {
            displayDriver = FramebufferDriver.forSenseHat();
        }
        return displayDriver;
    }

    public GraphicsDisplay getDisplay() {
        if (display == null) {
            display = new GraphicsDisplay(getDisplayDriver());
        }
        return display;
    }

    private void handleEvent(LinuxInputDriver.Event event) {
        if (event.getType() != LinuxInputDriver.EV_KEY) {
            return;
        }
        Boolean state = switch (event.getValue()) {
            case LinuxInputDriver.STATE_PRESS -> true;
            case LinuxInputDriver.STATE_RELEASE -> false;
            default -> null;
        };
        if (state == null) {
            return;
        }
        switch (event.getCode()) {
            case LinuxInputDriver.KEY_DOWN -> down.setState(state);
            case LinuxInputDriver.KEY_UP -> up.setState(state);
            case LinuxInputDriver.KEY_LEFT -> left.setState(state);
            case LinuxInputDriver.KEY_RIGHT -> right.setState(state);
            case LinuxInputDriver.KEY_ENTER -> center.setState(state);
        }
    }

    public void clear() {
        clear(BLACK);
    }
    
    public void clear(int r, int g, int b) {
        clear(rgb(r, g, b));
    }
    
    public void clear(int color) {
        Graphics graphics = getDisplay().getGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
        getDisplay().flush();
    }
    
    public void setPixel(int x, int y, int r, int g, int b) {
        setPixel(x, y, rgb(r, g, b));
    }
    
    public void setPixel(int x, int y, int color) {
        checkCoordinates(x, y);
        getDisplay().setPixel(x, y, color);
        getDisplay().flush();
    }
    
    public void setPixels(int[] pixels) {
        Objects.requireNonNull(pixels, "pixels must not be null");
    
        if (pixels.length != WIDTH * HEIGHT) {
            throw new IllegalArgumentException("pixels must contain exactly 64 RGB values");
        }
    
        var display = getDisplay();
    
        for (int i = 0; i < pixels.length; i++) {
            display.setPixel(i % WIDTH, i / WIDTH, pixels[i]);
        }
    
        display.flush();
    }
    
    public void setPixels(int[][] pixels) {
        Objects.requireNonNull(pixels, "pixels must not be null");
    
        if (pixels.length != WIDTH * HEIGHT) {
            throw new IllegalArgumentException("pixels must contain exactly 64 [r, g, b] entries");
        }
    
        var display = getDisplay();
    
        for (int i = 0; i < pixels.length; i++) {
            int[] pixel = pixels[i];
    
            if (pixel == null || pixel.length < 3) {
                throw new IllegalArgumentException("pixel " + i + " must contain [r, g, b]");
            }
    
            display.setPixel(i % WIDTH, i / WIDTH, rgb(pixel[0], pixel[1], pixel[2]));
        }
    
        display.flush();
    }
    
    public void showLetter(char letter) {
        showLetter(letter, WHITE);
    }
    
    public void showLetter(char letter, int r, int g, int b) {
        showLetter(letter, rgb(r, g, b));
    }
    
    public void showLetter(char letter, int color) {
        var display = getDisplay();
        Graphics graphics = display.getGraphics();
    
        graphics.setColor(BLACK);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
    
        graphics.setFont(BitmapFont.get5x8Font());
        graphics.setColor(color);
        graphics.renderCharacter(1, HEIGHT, letter);
    
        display.flush();
    }
    
    public void showMessage(String message) {
        showMessage(message, WHITE, 100);
    }
    
    public void showMessage(String message, int r, int g, int b, long delayMillis) {
        showMessage(message, rgb(r, g, b), delayMillis);
    }
    
    public void showMessage(String message, int color, long delayMillis) {
        Objects.requireNonNull(message, "message must not be null");
    
        var display = getDisplay();
        Graphics graphics = display.getGraphics();
    
        graphics.setFont(BitmapFont.get5x8Font(BitmapFont.Option.PROPORTIONAL));
    
        int textWidth = message.length() * 6;
    
        for (int x = WIDTH; x >= -textWidth; x--) {
            graphics.setColor(BLACK);
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
    
            graphics.setColor(color);
            graphics.renderText(x, HEIGHT, message);
    
            display.flush();
            sleep(delayMillis);
        }
    }
    
    
    private static int rgb(int r, int g, int b) {
        return 0xff000000
            | (clamp(r) << 16)
            | (clamp(g) << 8)
            | clamp(b);
    }
    
    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
    
    private static void checkCoordinates(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            throw new IllegalArgumentException("x and y must be between 0 and 7");
        }
    }
    
    private static void sleep(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while scrolling message", e);
        }
    }

}
