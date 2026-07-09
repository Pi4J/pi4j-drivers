package com.pi4j.drivers.hat.raspberry;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.GraphicsDisplay.Rotation;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.GraphicsTextAnimator;
import com.pi4j.drivers.display.graphics.framebuffer.FramebufferDriver;
import com.pi4j.drivers.display.BitmapFont;
import com.pi4j.drivers.display.graphics.Argb32;
import com.pi4j.drivers.display.graphics.Graphics;
import com.pi4j.drivers.input.GameController;
import com.pi4j.drivers.input.GameController.Key;
import com.pi4j.drivers.input.linux.LinuxInputDriver;
import com.pi4j.drivers.sensor.Sensor;
import com.pi4j.drivers.sensor.environment.hts221.Hts221Driver;
import com.pi4j.drivers.sensor.environment.lps25h.Lps25hDriver;
import com.pi4j.drivers.sensor.environment.tcs3400.Tcs3400Driver;
import com.pi4j.drivers.sensor.geospatial.lsm9ds1.Lsm9ds1Driver;
import com.pi4j.drivers.sensor.geospatial.lsm9ds1.Lsm9ds1MagnetometerDriver;
import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.i2c.I2C;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class SenseHat implements AutoCloseable {

    // -------------------------------------------------------------------------
    // Joystick event
    // -------------------------------------------------------------------------

    public enum Action { PRESSED, RELEASED, HELD }

    public record Event(java.time.Instant timestamp, GameController.Key key, Action action) {}

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private static final int WIDTH  = 8;
    private static final int HEIGHT = 8;

    private final Context pi4j;
    private Rotation rotation;

    // Display
    private GraphicsDisplayDriver displayDriver;
    private GraphicsDisplay       display;

    // Joystick
    private LinuxInputDriver       inputDriver;
    private GameController         controller;
    private final ListenableOnOffRead.Impl up     = new ListenableOnOffRead.Impl();
    private final ListenableOnOffRead.Impl down   = new ListenableOnOffRead.Impl();
    private final ListenableOnOffRead.Impl left   = new ListenableOnOffRead.Impl();
    private final ListenableOnOffRead.Impl right  = new ListenableOnOffRead.Impl();
    private final ListenableOnOffRead.Impl center = new ListenableOnOffRead.Impl();
    private final ConcurrentLinkedQueue<Event>      eventQueue        = new ConcurrentLinkedQueue<>();
    private final CopyOnWriteArrayList<Consumer<Event>> joystickListeners = new CopyOnWriteArrayList<>();

    // Environmental sensors
    private Hts221Driver             hts221Driver;
    private Lps25hDriver             lps25hDriver;
    private Tcs3400Driver            tcs3400Driver;

    // IMU
    private Lsm9ds1Driver            lsm9ds1Driver;
    private Lsm9ds1MagnetometerDriver lsm9ds1MagnetometerDriver;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public SenseHat(Context pi4j) {
        this.pi4j     = pi4j;
        this.rotation = Rotation.ROTATE_0;
    }

    public SenseHat(Context pi4j, Rotation rotation) {
        this.pi4j     = pi4j;
        this.rotation = rotation;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void close() {
        display                   = closeAndNull(display);
        displayDriver             = closeAndNull(displayDriver);
        controller                = closeAndNull(controller);
        inputDriver               = closeAndNull(inputDriver);
        hts221Driver              = closeAndNull(hts221Driver);
        lps25hDriver              = closeAndNull(lps25hDriver);
        tcs3400Driver             = closeAndNull(tcs3400Driver);
        lsm9ds1Driver             = closeAndNull(lsm9ds1Driver);
        lsm9ds1MagnetometerDriver = closeAndNull(lsm9ds1MagnetometerDriver);
    }

    private static <T extends AutoCloseable> T closeAndNull(T resource) {
        if (resource != null) {
            try { resource.close(); } catch (Exception ignored) {}
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // LED Matrix — drivers
    // -------------------------------------------------------------------------

    public GraphicsDisplayDriver getDisplayDriver() {
        if (displayDriver == null) {
            displayDriver = FramebufferDriver.forSenseHat();
        }
        return displayDriver;
    }

    public GraphicsDisplay getDisplay() {
        if (display == null) {
            display = new GraphicsDisplay(getDisplayDriver(), rotation);
        }
        return display;
    }

    // -------------------------------------------------------------------------
    // LED Matrix — orientation
    // -------------------------------------------------------------------------

    public void setRotation(Rotation r) {
        Objects.requireNonNull(r, "rotation must not be null");
        display       = closeAndNull(display);
        displayDriver = closeAndNull(displayDriver);
        rotation      = r;
        displayDriver = FramebufferDriver.forSenseHat();
        display       = new GraphicsDisplay(displayDriver, r);
    }

    public void flipHorizontal() {
        getDisplay().flipH();
    }

    public void flipVertical() {
        getDisplay().flipV();
    }

    // -------------------------------------------------------------------------
    // LED Matrix — pixels
    // -------------------------------------------------------------------------

    public void clear() {
        fill(Argb32.BLACK);
    }

    public void fill(int r, int g, int b) {
        fill(Argb32.fromRgb(r, g, b));
    }

    public void fill(int color) {
        Graphics graphics = getDisplay().getGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
    }

    public void setPixel(int x, int y, int r, int g, int b) {
        setPixel(x, y, Argb32.fromRgb(r, g, b));
    }

    public void setPixel(int x, int y, int color) {
        checkCoordinates(x, y);
        getDisplay().setPixel(x, y, color);
    }

    public int getPixel(int x, int y) {
        checkCoordinates(x, y);
        return getDisplay().getPixel(x, y);
    }

    public int[] getPixels() {
        return getDisplay().getPixels();
    }

    // pixel array must contain exactly 64 ARGB values in row-major order (index 0 = x=0,y=0)
    public void setPixels(int[] pixels) {
        Objects.requireNonNull(pixels, "pixels must not be null");
        if (pixels.length != WIDTH * HEIGHT) {
            throw new IllegalArgumentException("pixels must contain exactly 64 values");
        }
        var display = getDisplay();
        for (int i = 0; i < pixels.length; i++) {
            display.setPixel(i % WIDTH, i / WIDTH, pixels[i]);
        }
    }

    // pixel array must contain exactly 64 [r,g,b] entries in row-major order (index 0 = x=0,y=0)
    public void setPixels(int[][] pixels) {
        Objects.requireNonNull(pixels, "pixels must not be null");
        if (pixels.length != WIDTH * HEIGHT) {
            throw new IllegalArgumentException("pixels must contain exactly 64 [r,g,b] entries");
        }
        var display = getDisplay();
        for (int i = 0; i < pixels.length; i++) {
            int[] pixel = pixels[i];
            if (pixel == null || pixel.length < 3) {
                throw new IllegalArgumentException("pixel " + i + " must contain [r,g,b]");
            }
            display.setPixel(i % WIDTH, i / WIDTH, Argb32.fromRgb(pixel[0], pixel[1], pixel[2]));
        }
    }

    // -------------------------------------------------------------------------
    // LED Matrix — text
    // -------------------------------------------------------------------------

    public void showMessage(String text) {
        showMessage(text, 100, Argb32.WHITE, Argb32.BLACK);
    }

    public void showMessage(String text, long scrollSpeedMillis) {
        showMessage(text, scrollSpeedMillis, Argb32.WHITE, Argb32.BLACK);
    }

    public void showMessage(String text, long scrollSpeedMillis, int textColor) {
        showMessage(text, scrollSpeedMillis, textColor, Argb32.BLACK);
    }

    public void showMessage(String text, long scrollSpeedMillis, int textColor, int backColor) {
        GraphicsTextAnimator animator = new GraphicsTextAnimator(getDisplay(), text);
        animator.setDelayMillis(scrollSpeedMillis);
        animator.setForeground(textColor);
        animator.setBackground(backColor);
        animator.setClearOnStop(true);
        animator.scrollText();
    }

    public void showLetter(char letter) {
        showLetter(letter, Argb32.WHITE, Argb32.BLACK);
    }

    public void showLetter(char letter, int textColor) {
        showLetter(letter, textColor, Argb32.BLACK);
    }

    public void showLetter(char letter, int textColor, int backColor) {
        Graphics graphics = getDisplay().getGraphics();
        graphics.setColor(backColor);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
        graphics.setFont(BitmapFont.get5x8Font());
        graphics.setColor(textColor);
        graphics.renderCharacter(1, HEIGHT, letter);
    }

    // -------------------------------------------------------------------------
    // Humidity sensor (HTS221)
    // -------------------------------------------------------------------------

    public Hts221Driver getHumiditySensor() {
        if (hts221Driver == null) {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(1).device(Hts221Driver.I2C_ADDRESS));
            hts221Driver = new Hts221Driver(i2c);
        }
        return hts221Driver;
    }

    public double getHumidity() {
        return getHumiditySensor().readHumidity();
    }

    public double getTemperatureFromHumidity() {
        return getHumiditySensor().readTemperature();
    }

    // -------------------------------------------------------------------------
    // Pressure sensor (LPS25H)
    // -------------------------------------------------------------------------

    public Lps25hDriver getPressureSensor() {
        if (lps25hDriver == null) {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(1).device(Lps25hDriver.I2C_ADDRESS));
            lps25hDriver = new Lps25hDriver(i2c);
        }
        return lps25hDriver;
    }

    public double getPressure() {
        return getPressureSensor().readPressure();
    }

    public double getTemperatureFromPressure() {
        return getPressureSensor().readTemperature();
    }

    // -------------------------------------------------------------------------
    // Colour / light sensor (TCS3400)
    // -------------------------------------------------------------------------

    public Tcs3400Driver getLightSensor() {
        if (tcs3400Driver == null) {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(1).device(Tcs3400Driver.I2C_ADDRESS));
            tcs3400Driver = new Tcs3400Driver(i2c);
        }
        return tcs3400Driver;
    }

    // returns [clear, red, green, blue] as raw 16-bit counts
    public double[] getColour() {
        return getLightSensor().readCrgb();
    }

    // -------------------------------------------------------------------------
    // IMU — accelerometer & gyroscope (LSM9DS1)
    // -------------------------------------------------------------------------

    public Lsm9ds1Driver getAccelerometer() {
        if (lsm9ds1Driver == null) {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(1).device(Lsm9ds1Driver.I2C_ADDRESS_0));
            lsm9ds1Driver = new Lsm9ds1Driver(i2c);
        }
        return lsm9ds1Driver;
    }

    // returns [x, y, z] in m/s²
    public double[] getAccelerometerRaw() {
        return getAccelerometer().readAccelerometer();
    }

    // returns raw [x, y, z] in degrees/s
    public double[] getGyroscopeRaw() {
        return getAccelerometer().readGyroscope();
    }

    public double[] getOrientationDegrees() {
        return getAccelerometer().readOrientationDegrees();
    }

    public double[] getOrientationRadians() {
        return getAccelerometer().readOrientationRadians();
    }

    // -------------------------------------------------------------------------
    // IMU — magnetometer (LSM9DS1)
    // -------------------------------------------------------------------------

    public Lsm9ds1MagnetometerDriver getMagnetometer() {
        if (lsm9ds1MagnetometerDriver == null) {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(1).device(Lsm9ds1MagnetometerDriver.I2C_ADDRESS_0));
            lsm9ds1MagnetometerDriver = new Lsm9ds1MagnetometerDriver(i2c);
        }
        return lsm9ds1MagnetometerDriver;
    }

    public double getCompass() {
        return getMagnetometer().readHeading();
    }

    // returns [x, y, z] in gauss
    public double[] getCompassRaw() {
        return getMagnetometer().readMagneticField();
    }

    // -------------------------------------------------------------------------
    // IMU — combined config
    // -------------------------------------------------------------------------

    public void setImuConfig(boolean compassEnabled, boolean gyroEnabled, boolean accelEnabled) {
        getMagnetometer().setEnabled(compassEnabled);
        getAccelerometer().setGyroscopeEnabled(gyroEnabled);
        getAccelerometer().setAccelerometerEnabled(accelEnabled);
    }

    // -------------------------------------------------------------------------
    // All sensors
    // -------------------------------------------------------------------------

    public List<Sensor> getAllSensors() {
        List<Sensor> result = new ArrayList<>();
        result.add(getHumiditySensor());
        result.add(getPressureSensor());
        result.add(getLightSensor());
        result.add(getAccelerometer());
        result.add(getMagnetometer());
        return result;
    }

    // -------------------------------------------------------------------------
    // Joystick — driver & controller
    // -------------------------------------------------------------------------

    public LinuxInputDriver getInputDriver() {
        if (inputDriver == null) {
            inputDriver = LinuxInputDriver.forSenseHat();
            inputDriver.addListener(this::handleEvent);
        }
        return inputDriver;
    }

    public GameController getController() {
        if (controller == null) {
            getInputDriver();
            controller = new GameController.Builder(pi4j)
                    .addDigitalInput(GameController.Key.DOWN,   down)
                    .addDigitalInput(GameController.Key.LEFT,   left)
                    .addDigitalInput(GameController.Key.RIGHT,  right)
                    .addDigitalInput(GameController.Key.UP,     up)
                    .addDigitalInput(GameController.Key.CENTER, center)
                    .build();
        }
        return controller;
    }

    // -------------------------------------------------------------------------
    // Joystick — events
    // -------------------------------------------------------------------------

    // non-blocking; returns all events since last call
    public List<Event> getEvents() {
        getInputDriver();
        List<Event> result = new ArrayList<>();
        Event e;
        while ((e = eventQueue.poll()) != null) {
            result.add(e);
        }
        return result;
    }

    // blocks until the next joystick event; returns null if the thread is interrupted
    public Event waitForEvent() {
        LinkedBlockingQueue<Event> slot = new LinkedBlockingQueue<>(1);
        Consumer<Event> listener = e -> slot.offer(e);
        addJoystickListener(listener);
        try {
            return slot.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            removeJoystickListener(listener);
        }
    }

    // -------------------------------------------------------------------------
    // Joystick — listeners
    // -------------------------------------------------------------------------

    public void addJoystickListener(Consumer<Event> listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        joystickListeners.add(listener);
        getInputDriver();
    }

    public void removeJoystickListener(Consumer<Event> listener) {
        joystickListeners.remove(listener);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void handleEvent(LinuxInputDriver.Event event) {
        if (event.getType() != LinuxInputDriver.EV_KEY) {
            return;
        }

        int code  = event.getCode();
        int value = event.getValue();

        record KeyEntry(Key key, ListenableOnOffRead.Impl impl) {}
        KeyEntry entry = switch (code) {
            case LinuxInputDriver.KEY_UP    -> new KeyEntry(Key.UP,     up);
            case LinuxInputDriver.KEY_DOWN  -> new KeyEntry(Key.DOWN,   down);
            case LinuxInputDriver.KEY_LEFT  -> new KeyEntry(Key.LEFT,   left);
            case LinuxInputDriver.KEY_RIGHT -> new KeyEntry(Key.RIGHT,  right);
            case LinuxInputDriver.KEY_ENTER -> new KeyEntry(Key.CENTER, center);
            default -> null;
        };
        if (entry == null) return;

        Key key  = entry.key();
        if (value == LinuxInputDriver.STATE_PRESS || value == LinuxInputDriver.STATE_RELEASE) {
            entry.impl().setState(value == LinuxInputDriver.STATE_PRESS);
        }

        Action action = switch (value) {
            case LinuxInputDriver.STATE_PRESS   -> Action.PRESSED;
            case LinuxInputDriver.STATE_RELEASE -> Action.RELEASED;
            case LinuxInputDriver.STATE_HOLD    -> Action.HELD;
            default -> null;
        };
        if (action == null) return;

        Event senseEvent = new Event(event.getTime(), key, action);
        eventQueue.add(senseEvent);

        for (Consumer<Event> listener : joystickListeners) {
            listener.accept(senseEvent);
        }
    }

    private static void checkCoordinates(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            throw new IllegalArgumentException("x and y must be between 0 and 7");
        }
    }
}