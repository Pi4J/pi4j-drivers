package com.pi4j.drivers.sensor.environment.bmx280;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Runs common tests from the base class and additional tests depending on a specific fake setup.
 */
public class Bmx280DriverFakeI2cTest extends AbstractBmx280DriverTest {

    @Test
    public void testChipId() {
        Bmx280Driver driver = createDriver();
        assertEquals(Bmx280Driver.Model.BME280, driver.getModel());
    }

    @Override
    Bmx280Driver createDriver() {
        FakeI2CRegisterDataReaderWriter fakeI2c =  new FakeI2CRegisterDataReaderWriter();

        // Chip id
        fakeI2c.writeRegister(Bmp280Constants.CHIP_ID, Bmp280Constants.ID_VALUE_BME);

        // Calibration data
        fakeI2c.writeRegister(0x88, new byte[] {
                0, 110, -114, 103, 50, 0, 67, -111, 102, -42, -48, 11, -61, 27, 44, 0,
                -7, -1, -76, 45, -24, -47, -120, 19});

        fakeI2c.writeRegister(0xe0, new byte[] {0, 110, 1, 0, 19, 39, 3, 30, 4, 65, -1, -1, -1, -1, -1, -1});

        // Measurement data
        fakeI2c.writeRegister(0xf7, new byte[] {86, -61, 0, 126, -63, 0, 123, 82, -128});

        return new Bmx280Driver(fakeI2c);
    }
}
