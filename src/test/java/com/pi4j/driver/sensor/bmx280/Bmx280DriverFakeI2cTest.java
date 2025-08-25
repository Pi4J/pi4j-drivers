package com.pi4j.driver.sensor.bmx280;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class Bmx280DriverFakeI2cTest {

    @Test
    public void testBmx280DriverUnrecognizedChipId() {
        FakeI2CRegisterDataReaderWriter access = new FakeI2CRegisterDataReaderWriter(
                FakeI2CRegisterDataReaderWriter.READ, Bmp280Constants.CHIP_ID, 123
        );

        assertThrows(
                IllegalStateException.class,
                () -> new Bmx280Driver(access),
                "Unrecognized chip ID: 123");
    }

}
