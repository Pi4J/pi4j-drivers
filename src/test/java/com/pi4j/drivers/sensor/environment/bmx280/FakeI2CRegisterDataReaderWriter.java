package com.pi4j.drivers.sensor.environment.bmx280;

import com.pi4j.io.i2c.I2CRegisterDataReaderWriter;


/**
 * Fake I2C register access implementation that just stores the register values as sent.
 * Regular register read and write operations can be used in tests to set expected output data and
 */
public class FakeI2CRegisterDataReaderWriter implements I2CRegisterDataReaderWriter {

    // Allow tests direct access
    public final byte[] registerValues = new byte[256];

    @Override
    public int readRegister(int index) {
        return registerValues[index] & 255;
    }

    @Override
    public int readRegister(byte[] register, byte[] data, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readRegister(int register, byte[] data, int offset, int length) {
        System.arraycopy(registerValues, register, data, offset, length);
        return length;
    }

    @Override
    public int writeRegister(int register, byte value) {
        registerValues[register] = value;
        return 1;
    }

    @Override
    public int writeRegister(int register, byte[] data, int offset, int length) {
        System.arraycopy(data, offset, registerValues, register, length);
        return length;
    }

    @Override
    public int writeRegister(byte[] register, byte[] data, int offset, int length) {
       throw new UnsupportedOperationException();
    }
}
