package com.pi4j.driver.sensor.bmx280;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.exception.Pi4JException;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfigBuilder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * If a BME 280 configured to the BMP 280 address or a BMP 280 is connected to i2c bus 1,
 * this test will perform a measurement and check that the values are in a reasonable range.
 */
public class Bmx280DriverI2cTest {

    static final int BUS = 1;
    static final int ADDRESS = Bmx280Driver.ADDRESS_BMP_280;

    static final Context pi4j = Pi4J.newAutoContext();

    @Test
    public void testBasicMeasurementWorks() {
        Bmx280Driver driver = createDriverOrAbort();

        Bmx280Driver.Measurement measurement = driver.readMeasurement();

        assertTrue(measurement.getTemperature() > 0);
        assertTrue(measurement.getTemperature() < 50);
        assertTrue(measurement.getPressure() > 90_000);
        assertTrue(measurement.getPressure() < 110_000);
    }

    Bmx280Driver createDriverOrAbort() {
        try {
            I2C i2c = pi4j.create(I2CConfigBuilder.newInstance(pi4j).bus(BUS).device(ADDRESS));
            return new Bmx280Driver(i2c);
        } catch (Pi4JException e) {
            Assumptions.abort("BMx280 not found on i2c bus " + BUS + " address " + ADDRESS);
            throw new RuntimeException(e);
        }
    }


}
