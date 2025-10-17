package com.pi4j.drivers.sensor.geospatial.lsm9ds1;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.drivers.sensor.geospatial.lsm9ds1.Lsm9ds1MagnetometerDriver.Range;
import com.pi4j.io.i2c.I2C;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Lsm9ds1MagnetometerDriverTest {
    // Sense HAT configuration
    private static final int BUS = 1;

    Context pi4j;

    @BeforeEach
    public void setup() {
        pi4j = Pi4J.newAutoContext();
    }

    @Test
    public void testBasicMeasurementWorks() throws InterruptedException{
        try (Lsm9ds1MagnetometerDriver driver = createDriver()) {

            float[] magneticField = driver.readMagneticField();

            assertTrue(Math.abs(magneticField[0]) < 4);
            assertTrue(Math.abs(magneticField[1]) < 4);
            assertTrue(Math.abs(magneticField[2]) < 4);

            // Check that the values are roughly the same when we change the measurement range.
            driver.setRange(Range.GAUSS_16);
            float[] magneticField2 = driver.readMagneticField();
            for (int i = 0; i < magneticField2.length; i++) {
                assertTrue(Math.abs(magneticField[i]-magneticField2[i]) < 0.1);
            }
        }
    }

    @AfterEach
    public void shutdown() {
        pi4j.shutdown();
    }

    public Lsm9ds1MagnetometerDriver createDriver() {
        try {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j).bus(BUS).device(Lsm9ds1MagnetometerDriver.I2C_ADDRESS_0));
            return new Lsm9ds1MagnetometerDriver(i2c);
        } catch (Exception e) {
            e.printStackTrace();
            Assumptions.abort("LSM9DS1 not found on i2c bus " + BUS + " address " + Lsm9ds1MagnetometerDriver.I2C_ADDRESS_0);
            throw new RuntimeException(e);
        }
    }
}
