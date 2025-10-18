package com.pi4j.drivers.sensor.environment.bmx280;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.exception.Pi4JException;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfigBuilder;
import org.junit.jupiter.api.Assumptions;

/**
 * Runs tests if a BME 280 configured to the BMP 280 address or a BMP 280 is connected to i2c bus 1;
 * aborts otherwise.
 */
public class Bmx280DriverI2cTest extends AbstractBmx280DriverTest {

    static final int BUS = 1;
    static final int ADDRESS = Bmx280Driver.ADDRESS_BMP_280;

    static final Context pi4j = Pi4J.newAutoContext();

    @Override
    Bmx280Driver createDriver() {
        try {
            I2C i2c = pi4j.create(I2CConfigBuilder.newInstance(pi4j).bus(BUS).device(ADDRESS));
            return new Bmx280Driver(i2c);
        } catch (Pi4JException e) {
            Assumptions.abort("BMx280 not found on i2c bus " + BUS + " address " + ADDRESS);
            throw new RuntimeException(e);
        }
    }
}
