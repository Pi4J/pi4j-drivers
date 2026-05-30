package com.pi4j.drivers.sensor.environment.hts221;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.drivers.sensor.geospatial.lsm9ds1.Lsm9ds1Driver;
import com.pi4j.io.i2c.I2C;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Hts221DriverTest {
    // Sense HAT configuration
    private static final int BUS = 1;

    static Context pi4j = Pi4J.newAutoContext();

    @Test
    public void testBasicMeasurementWorks() throws InterruptedException{
        Hts221Driver driver = createDriver();

        float temperature = driver.readTemperature();
        assertTrue(temperature > 10);
        assertTrue(temperature < 60); // The chip is on the hat above the CPU...

        float humidity = driver.readHumidity();
        assertTrue(humidity > 10);
        assertTrue(humidity < 90);
    }

    public Hts221Driver createDriver() {
        try {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(BUS).device(Hts221Driver.I2C_ADDRESS));
            return new Hts221Driver(i2c);
        } catch (Exception e) {
            e.printStackTrace();
            Assumptions.abort("HTS 221 not found on i2c bus " + BUS + " address " + Hts221Driver.I2C_ADDRESS);
            throw new RuntimeException(e);
        }
    }
}
