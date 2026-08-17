package com.pi4j.drivers.multipurpose.piborg;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

//@Disabled
public class UltraBorgDriverTest {

    private static Logger log = LoggerFactory.getLogger(UltraBorgDriverTest.class);

    private static final int BUS = 1;

    private Context pi4j;

    @BeforeEach
    public void setUp() {
        pi4j = Pi4J.newAutoContext();
    }

    @Test
    public void testGetBoardId() throws Exception {

        try (UltraBorgDriver driver = createDriver()) {
            int boardId = driver.getBoardId();
            assertTrue(54 == boardId);
        }

    }

    @Test
    public void testMinMax() throws Exception {

        try (UltraBorgDriver driver = createDriver()) {

            int minB4 = driver.getServoMinimum(UltraBorgDriver.Channel.CHANNEL_1);
            int maxB4 = driver.getServoMaximum(UltraBorgDriver.Channel.CHANNEL_1);
            int startupB4 = driver.getServoStartup(UltraBorgDriver.Channel.CHANNEL_1);

            log.info("B4 Min: " + minB4 + "\tMax: " + maxB4 + "\tStartup: " + startupB4);

            driver.setServoMinimum(UltraBorgDriver.Channel.CHANNEL_1, minB4 + 50);
            driver.setServoMaximum(UltraBorgDriver.Channel.CHANNEL_1, maxB4 - 50);
            driver.setServoStartup(UltraBorgDriver.Channel.CHANNEL_1, startupB4 + 1);

            int minAfter = driver.getServoMinimum(UltraBorgDriver.Channel.CHANNEL_1);
            int maxAfter = driver.getServoMaximum(UltraBorgDriver.Channel.CHANNEL_1);
            int startupAfter = driver.getServoStartup(UltraBorgDriver.Channel.CHANNEL_1);

            log.info("After Min: " + minAfter + "\tMax: " + maxAfter + "\tStartup: " + startupAfter);

            assertTrue(minB4 + 50 == minAfter);
            assertTrue(maxB4 - 50 == maxAfter);
            assertTrue(startupB4 + 1 == startupAfter);
        }
    }

    @Test
    public void testPosition() throws Exception {

        try (UltraBorgDriver driver = createDriver()) {
            int pwm = driver.getRawPwm(UltraBorgDriver.Channel.CHANNEL_1);
            double position = driver.getServoPosition(UltraBorgDriver.Channel.CHANNEL_1);
            log.info("Pwm: " + pwm + "\tPosotion: " + position);

            long delayMs = calculateDelay(position, -1.0);
            driver.setServoPosition(UltraBorgDriver.Channel.CHANNEL_1, -1.0);
            Thread.sleep(delayMs);

            pwm = driver.getRawPwm(UltraBorgDriver.Channel.CHANNEL_1);
            position = driver.getServoPosition(UltraBorgDriver.Channel.CHANNEL_1);
            log.info("Pwm: " + pwm + "\tPosotion: " + position);

            driver.setServoPosition(UltraBorgDriver.Channel.CHANNEL_1, 0.0);
            Thread.sleep(delayMs);
            pwm = driver.getRawPwm(UltraBorgDriver.Channel.CHANNEL_1);
            position = driver.getServoPosition(UltraBorgDriver.Channel.CHANNEL_1);
            log.info("Pwm: " + pwm + "\tPosotion: " + position);

            driver.setServoPosition(UltraBorgDriver.Channel.CHANNEL_1, 1.0);
            Thread.sleep(delayMs);
            pwm = driver.getRawPwm(UltraBorgDriver.Channel.CHANNEL_1);
            position = driver.getServoPosition(UltraBorgDriver.Channel.CHANNEL_1);
            log.info("Pwm: " + pwm + "\tPosotion: " + position);

        }
    }

    @AfterEach
    public void shutdown() {
        pi4j.shutdown();
    }

    public UltraBorgDriver createDriver() {
        try {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(BUS).device(UltraBorgDriver.DEFAULT_ADDRESS));
            return new UltraBorgDriver(i2c);
        } catch (Exception e) {
            e.printStackTrace();
            Assumptions.abort("UltraBorg not found on i2c bus " + BUS + " address " + UltraBorgDriver.DEFAULT_ADDRESS);
            throw new RuntimeException(e);
        }
    }

    private long calculateDelay(double oldPos, double newPos) {
        final double DEGREES_PER_UNIT = 180.0; // adjust to your servo's actual range
        final double MS_PER_DEGREE = 0.17 / 60.0 * 1000; // 2.833 ms/degree
        final long BUFFER_MS = 50; // slack for acceleration, no-load vs loaded, etc.

        double positionDelta = Math.abs(newPos - oldPos); // in -1.0..1.0 units
        double degreesMoved = positionDelta * DEGREES_PER_UNIT;
        long delayMs = (long) (degreesMoved * MS_PER_DEGREE) + BUFFER_MS;
        return delayMs;
    }

}
