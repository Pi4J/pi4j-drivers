package com.pi4j.drivers.sensor.geospatial.lsm9ds1;

import com.pi4j.drivers.sensor.Sensor;
import com.pi4j.drivers.sensor.SensorDescriptor;
import com.pi4j.io.i2c.I2CRegisterDataReaderWriter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Basic driver for the LSM9DS1 mangentometer.
 * <p>
 * Note that the accelerometer and gyroscope of this module has its own connection, handled by a separate driver.
 * <p>
 * Interrupts, FIFO and some settings are currently not supported.
 * <p>
 * Datasheet: https://www.st.com/resource/en/datasheet/lsm9ds1.pdf
 */
public class Lsm9ds1MagnetometerDriver implements Sensor {
    public static final int I2C_ADDRESS_0 = 0x1c;
    public static final int I2C_ADDRESS_1 = 0x1e;
    public static final SensorDescriptor DESCRIPTOR = new SensorDescriptor(
            new SensorDescriptor.Value(0, SensorDescriptor.Kind.MAGNETIC_FIELD_X),
            new SensorDescriptor.Value(0, SensorDescriptor.Kind.MAGNETIC_FIELD_Y),
            new SensorDescriptor.Value(0, SensorDescriptor.Kind.MAGNETIC_FIELD_Z));

    private static final int WHO_AM_I_VALUE = 0b111101;

    private final I2CRegisterDataReaderWriter registerAccess;

    private final ByteBuffer buffer = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);

    private Range range = Range.GAUSS_4;

    public Lsm9ds1MagnetometerDriver(I2CRegisterDataReaderWriter registerAccess) {
        this.registerAccess = registerAccess;

        int whoAmIValue = registerAccess.readRegister(Register.WHO_AM_I_M);
        if (whoAmIValue != WHO_AM_I_VALUE) {
            throw new IllegalStateException("WhoAmI value " + whoAmIValue + " does not match expected value " + WHO_AM_I_VALUE);
        }

        // Request soft reset.
        setRegisterBits(Register.CTRL_REG2_M, 2, 2, 1);
    }

    @Override
    public void close() {
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

    @Override
    public void readMeasurement(float[] values) {
        // Request single measurement
        setRegisterBits(Register.CTRL_REG3_M, 1, 0, 1);

        float scale = switch (range) {
            case GAUSS_4 -> 4f;
            case GAUSS_8 -> 8f;
            case GAUSS_12 -> 12f;
            case GAUSS_16 -> 16f;
        } / Short.MAX_VALUE;

        while ((registerAccess.readRegister(Register.STATUS_REG_M) & 8) == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        registerAccess.readRegister(Register.OUT_X_L_M, buffer.array(), 0, 6);

        values[0] = buffer.getShort(0) * scale;
        values[1] = buffer.getShort(2) * scale;
        values[2] = buffer.getShort(4) * scale;
    }

    public float[] readMagneticField() {
        float[] result = new float[3];
        readMeasurement(result);
        return result;
    }

    public void setRange(Range range) {
        setRegisterBits(Register.CTRL_REG2_M, 6, 5, range.ordinal());
        this.range = range;
    }

    // Private

    private void setRegisterBits(int register, int high, int low, int value) {
        int count = high - low + 1;
        int mask = (((1 << count) - 1) << low);

        int registerValue = registerAccess.readRegister(register);
        int updatedValue = (registerValue & ~mask) | ((value << low) & mask);

        registerAccess.writeRegister(register, updatedValue);
    }

    enum Range {
        // The order is important, as the ordinal value will be used to set the corresponding register;
        GAUSS_4, GAUSS_8, GAUSS_12, GAUSS_16
    }
}
