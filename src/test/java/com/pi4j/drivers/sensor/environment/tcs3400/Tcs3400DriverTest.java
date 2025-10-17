package com.pi4j.drivers.sensor.environment.tcs3400;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Tcs3400DriverTest {
    // Sense HAT configuration
    private static final int BUS = 1;

    private Context pi4j;

    @BeforeEach
    public void setup() {
        pi4j = Pi4J.newAutoContext();
    }

    @Test
    public void testBasicMeasurementWorks() throws InterruptedException{
        try (Tcs3400Driver driver = createDriver()) {

            float[] crgb = driver.readCrgb();

            assertTrue(Math.abs(crgb[0]) < 0x10000);
            assertTrue(Math.abs(crgb[1]) < 0x10000);
            assertTrue(Math.abs(crgb[2]) < 0x10000);
            assertTrue(Math.abs(crgb[3]) < 0x10000);
        }
    }

    @AfterEach
    public void shutdown() {
        pi4j.shutdown();
    }

    public Tcs3400Driver createDriver() {
        try {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(BUS).device(Tcs3400Driver.I2C_ADDRESS));
            return new Tcs3400Driver(i2c);
        } catch (Exception e) {
            e.printStackTrace();
            Assumptions.abort("TCS3400 not found on i2c bus " + BUS + " address " + Tcs3400Driver.I2C_ADDRESS);
            throw new RuntimeException(e);
        }
    }
}
