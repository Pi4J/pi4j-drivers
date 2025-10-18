package com.pi4j.drivers.sensor.environment.bmx280;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Common tests that work for spi, i2c and fake drivers.
 */
abstract class AbstractBmx280DriverTest {

    @Test
    public void testBasicMeasurementWorks() {
        Bmx280Driver driver = createDriver();

        Bmx280Driver.Measurement measurement = driver.readMeasurement();

        assertTrue(measurement.getTemperature() > 0);
        assertTrue(measurement.getTemperature() < 50);
        assertTrue(measurement.getPressure() > 90_000);
        assertTrue(measurement.getPressure() < 110_000);
    }

    abstract Bmx280Driver createDriver();
}
