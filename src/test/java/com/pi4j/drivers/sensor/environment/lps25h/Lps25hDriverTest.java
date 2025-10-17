package com.pi4j.drivers.sensor.environment.lps25h;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Lps25hDriverTest {
    // Sense HAT configuration
    private static final int BUS = 1;

    static Context pi4j = Pi4J.newAutoContext();

    @Test
    public void testBasicMeasurementWorks() throws InterruptedException{
        Lps25hDriver driver = createDriver();

        float temperature = driver.readTemperature();
        assertTrue(temperature > 10);
        assertTrue(temperature < 60); // The chip is on the hat above the CPU...

        float pressure = driver.readPressure();
        assertTrue(pressure > 900);
        assertTrue(pressure < 1100);
    }

    public Lps25hDriver createDriver() {
        try {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(BUS).device(Lps25hDriver.I2C_ADDRESS));
            return new Lps25hDriver(i2c);
        } catch (Exception e) {
            e.printStackTrace();
            Assumptions.abort("LPS25H not found on i2c bus " + BUS + " address " + Lps25hDriver.I2C_ADDRESS);
            throw new RuntimeException(e);
        }
    }
}
