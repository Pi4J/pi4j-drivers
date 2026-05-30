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

        double temperature = measurement.getTemperature();
        assertTrue(temperature > 0, "Temperature should be greater than 0°C; was: " + temperature);
        assertTrue(temperature < 50, "Temperature should be smaller than 50°C; was: " + temperature);

        double pressure = measurement.getPressure();
        assertTrue(pressure > 90_000, "Pressure should be greater than 90 kPa; was: " + pressure + " Pa");
        assertTrue(pressure < 110_000, "Pressure should be less than 110 kPa; was: " + pressure + " Pa");
    }

    abstract Bmx280Driver createDriver();
}
