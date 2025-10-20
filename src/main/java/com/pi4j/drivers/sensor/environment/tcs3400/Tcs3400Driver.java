package com.pi4j.drivers.sensor.environment.tcs3400;

import com.pi4j.drivers.sensor.Sensor;
import com.pi4j.drivers.sensor.SensorDescriptor;
import com.pi4j.io.i2c.I2CRegisterDataReaderWriter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A minimal TCS3400 driver for measuring light color and brightness.
 *
 * Datasheet: https://look.ams-osram.com/m/595d46c644740603/original/TCS3400-Color-Light-to-Digital-Converter.pdf
 */
public class Tcs3400Driver implements Sensor {
    public static final int I2C_ADDRESS = 0x39;
    public static final int I2C_ADDRESS_TCS34007 = 0x29;
    public static final SensorDescriptor DESCRIPTOR = new SensorDescriptor(
            new SensorDescriptor.Value(0, SensorDescriptor.Kind.LIGHT),
            new SensorDescriptor.Value(1, SensorDescriptor.Kind.LIGHT_RED),
            new SensorDescriptor.Value(2, SensorDescriptor.Kind.LIGHT_GREEN),
            new SensorDescriptor.Value(3, SensorDescriptor.Kind.LIGHT_BLUE));

    private static final int ID_TCS34001_34005 = 0b100100_00;
    private static final int ID_TCS34003_34007 = 0b100100_11;

    private final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    private final I2CRegisterDataReaderWriter registerAccess;

    public Tcs3400Driver(I2CRegisterDataReaderWriter registerAccess) {
        this.registerAccess = registerAccess;

        int id = registerAccess.readRegister(Register.ID);
        if (id != ID_TCS34001_34005 && id != ID_TCS34003_34007) {
            throw new IllegalStateException("Expected id value " + ID_TCS34001_34005 + " or " + ID_TCS34003_34007 + " but got " + id);
        }

        registerAccess.writeRegister(Register.ENABLE, 0b00000011);  // AES, PON bits
    }

    @Override
    public void close() {
        registerAccess.writeRegister(Register.ENABLE, 0);

        if (registerAccess instanceof Closeable) {
            try {
                ((Closeable) registerAccess).close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public SensorDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Reads the "clear", red, green and blue values from the sensor and returns them as an array of four
     * integers.
     */
    public float[] readCrgb() {
        float[] result = new float[4];
        readMeasurement(result);
        return result;
    }

    @Override
    public void readMeasurement(float[] values) {
        while((registerAccess.readRegister(Register.STATUS) & 1) == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        registerAccess.readRegister(Register.CDATAL, buffer.array(), 0, 8);

        values[0] =  buffer.getShort(0) & 0xffff;
        values[1] =  buffer.getShort(2) & 0xffff;
        values[2] =  buffer.getShort(4) & 0xffff;
        values[3] =  buffer.getShort(6) & 0xffff;
    }
}
