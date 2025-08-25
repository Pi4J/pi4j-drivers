package com.pi4j.driver.sensor.bmx280;

import com.pi4j.io.i2c.I2CRegisterDataReaderWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fake I2C register access implementation that can play back / expect a given communication pattern.
 * <p>
 * For the given test, a mock would have been more straightforward, but I didn't want to pull in additional
 * dependencies.
 */
public class FakeI2CRegisterDataReaderWriter implements I2CRegisterDataReaderWriter {

    public final static int READ = -1;
    public final static int WRITE = -2;

    final int[] expectedCommunication;
    private int position = 0;
    private byte[] buf = new byte[1];

    public FakeI2CRegisterDataReaderWriter(int... expectedCommunication) {
        this.expectedCommunication = expectedCommunication;
    }

    @Override
    public int readRegister(int index) {
        readRegister(index, buf, 0, 1);
        return buf[0] & 255;
    }

    @Override
    public int readRegister(byte[] register, byte[] data, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readRegister(int register, byte[] data, int offset, int length) {
        assertEquals(expectedCommunication[position++], READ, "READ marker expected");
        assertEquals(expectedCommunication[position++], register, "register expected");

        for (int i = 0; i < length; i++) {
            int input = expectedCommunication[position++];
            assertTrue(input >= 0, "Read data expected");
            data[offset + i] = (byte) input;
        }

        return length;
    }

    @Override
    public int writeRegister(int register, byte value) {
        buf[0] = value;
        writeRegister(register, buf, 0, 1);
        return 1;
    }

    @Override
    public int writeRegister(int register, byte[] data, int offset, int length) {
        assertEquals(expectedCommunication[position++], WRITE, "WRITE marker expected");
        assertEquals(expectedCommunication[position++], register, "register expected");

        for (int i = 0; i < length; i++) {
            int input = expectedCommunication[position++];
            assertEquals(input, data[offset + i] & 0xff, input);
        }

        return length;
    }

    @Override
    public int writeRegister(byte[] register, byte[] data, int offset, int length) {
        throw new UnsupportedOperationException();
    }

}
