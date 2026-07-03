package com.pi4j.drivers.sensor.environment.bmx280;

import com.pi4j.io.SerialCircuitIO;


/**
 * Fake I2C register access implementation that just stores the register values as sent.
 * Regular register read and write operations can be used in tests to set expected output data and
 */
public class FakeI2CRegisterDataReaderWriter implements SerialCircuitIO {

    // Allow tests direct access. We only use 7 bits (the 8th bit is used as the SPI read flag).
    public final byte[] registerValues = new byte[128];

    @Override
    public void writeThenRead(byte[] writeData, int writeOffset, int writeLength, int delay, byte[] readData, int readOffset, int readLength) {
        if (writeLength < 1) {
            throw new IllegalArgumentException("Register expected");
        }
        int register = writeData[writeOffset] & 0x7f;
        if (writeLength > 1) {
            System.arraycopy(writeData, writeOffset + 1, registerValues, register, writeLength - 1);
        }

        if (readLength > 0 && readData != null) {
            System.arraycopy(registerValues, register, readData, readOffset, readLength);
        }
    }

    @Override
    public void close() throws Exception {

    }
}
