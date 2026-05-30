package com.pi4j.drivers.sensor.environment.scd4x;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.drivers.sensor.environment.scd4x.Scd4xDriver.Measurement;
import com.pi4j.drivers.sensor.environment.scd4x.Scd4xDriver.Mode;
import com.pi4j.exception.Pi4JException;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfigBuilder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Scd4xDriverTest {

    static final int BUS = 1;

    static final Context pi4j = Pi4J.newAutoContext();

    @Test
    public void testBasicMeasurementWorks() {
        try (Scd4xDriver driver = createDriver()) {
            assertEquals(Mode.IDLE, driver.getMode());

            driver.startPeriodicMeasurement();

            assertEquals(Mode.PERIODIC_MEASUREMENT, driver.getMode());

            Measurement measurement = driver.readMeasurement();

            driver.stopPeriodicMeasurement();

            assertEquals(Mode.IDLE, driver.getMode());

            assertTrue(measurement.getTemperature() > 0);
            assertTrue(measurement.getTemperature() < 50);
            assertTrue(measurement.getCo2() > 300);
            assertTrue(measurement.getCo2() < 3000);
            assertTrue(measurement.getHumidity() >= 0);
            assertTrue(measurement.getHumidity() <= 100);
        }
    }


    Scd4xDriver createDriver() {
        try (I2C i2c = pi4j.create(I2CConfigBuilder.newInstance(pi4j).bus(BUS).device(Scd4xDriver.I2C_ADDRESS));
             Scd4xDriver driver = new Scd4xDriver(i2c)) {
            return driver;
        } catch (Pi4JException e) {
            Assumptions.abort("SCD 4x not found on i2c bus " + BUS + " address " + Scd4xDriver.I2C_ADDRESS);
            throw new RuntimeException(e);
        }
    }
}
