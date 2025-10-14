package com.pi4j.drivers.sensor.geospatial.lsm9ds1;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Lsm9ds1DriverTest {
    // Sense HAT configuration
    private static final int BUS = 1;

    static Context pi4j = Pi4J.newAutoContext();

    @Test
    public void testBasicMeasurementWorks() throws InterruptedException{
        Lsm9ds1Driver driver = createDriver();
        driver.setAccelerometerEnabled(true);
        driver.setGyroscopeEnabled(true);

        // Gyroscope seems to be a bit flaky...
        driver.readGyroscope();
        float[] angularRate = driver.readGyroscope();

        assertTrue(Math.abs(angularRate[0]) < 2);
        assertTrue(Math.abs(angularRate[1]) < 2);
        assertTrue(Math.abs(angularRate[2]) < 2);

        float[] acceleration = driver.readAccelerometer();

        assertTrue(Math.abs(acceleration[0]) < 0.1);
        assertTrue(Math.abs(acceleration[1]) < 0.1);
        assertTrue(Math.abs(acceleration[2] - 0.981) < 0.1);
    }

    public Lsm9ds1Driver createDriver() {
        try {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(BUS).device(Lsm9ds1Driver.I2C_ADDRESS));
            return new Lsm9ds1Driver(i2c);
        } catch (Exception e) {
            e.printStackTrace();
            Assumptions.abort("LSM9DS1 not found on i2c bus " + BUS + " address " + Lsm9ds1Driver.I2C_ADDRESS);
            throw new RuntimeException(e);
        }
    }
}
