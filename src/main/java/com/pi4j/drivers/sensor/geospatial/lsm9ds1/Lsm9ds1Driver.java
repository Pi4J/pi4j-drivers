package com.pi4j.drivers.sensor.geospatial.lsm9ds1;

import com.pi4j.drivers.sensor.Sensor;
import com.pi4j.drivers.sensor.SensorDescriptor;
import com.pi4j.io.i2c.I2CRegisterDataReaderWriter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Basic driver for the LSM9DS1 inertial module (IMU) accelerometer and gyroscope.
 * <p>
 * Note that the magnetometer of this module has its own connection, handled by a separate driver.
 * <p>
 * Interrupts, FIFO and some settings are currently not supported.
 * <p>
 * Datasheet: https://www.st.com/resource/en/datasheet/lsm9ds1.pdf
 */
public class Lsm9ds1Driver implements Sensor {
    public static final int I2C_ADDRESS_0 = 0x6a;
    public static final int I2C_ADDRESS_1 = 0x6b;
    private final static int WHO_AM_I_VALUE = 0b01101000;

    public static final SensorDescriptor DESCRIPTOR = new SensorDescriptor.Builder()
            .addValue(SensorDescriptor.Kind.ACCELERATION_X)
            .addValue(SensorDescriptor.Kind.ACCELERATION_Y)
            .addValue(SensorDescriptor.Kind.ACCELERATION_Z)
            .addValue(SensorDescriptor.Kind.ANGULAR_VELOCITY_X)
            .addValue(SensorDescriptor.Kind.ANGULAR_VELOCITY_Y)
            .addValue(SensorDescriptor.Kind.ANGULAR_VELOCITY_Z)
            .addI2cAddress(I2C_ADDRESS_0)
            .addI2cAddress(I2C_ADDRESS_1)
            .setI2cSensorDetector(i2c -> i2c.readRegister(Register.WHO_AM_I) == WHO_AM_I_VALUE ? new Lsm9ds1Driver(i2c) : null)
            .build();


    private final I2CRegisterDataReaderWriter registerAccess;
    private final ByteBuffer buffer = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);

    private GyroscopeRange gyroscopeRange = GyroscopeRange.DPS_245;
    private AccelerometerRange accelerometerRange = AccelerometerRange.G_2;
    private boolean accelerometerEnabled = true;
    private boolean gyroscopeEnabled = true;
    private int outputDataRateCode = 1;
    private float outputDataRate = 14.9f;

    public Lsm9ds1Driver(I2CRegisterDataReaderWriter registerAccess) {
        this.registerAccess = registerAccess;

        int whoAmI = registerAccess.readRegister(Register.WHO_AM_I);
        if (whoAmI != WHO_AM_I_VALUE) {
            throw new IllegalStateException("WHO_AM_I register value " + Integer.toHexString(whoAmI) + " does not match expected value: " + Integer.toHexString(WHO_AM_I_VALUE));
        }

        // Soft reset, so we are sure we match the assumed default state.
        registerAccess.writeRegister(Register.CTRL_REG8, 0b00000101);

        try {
            // Not using DeferredDelay here as this seems to be the only delay required
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        updateOperationMode();
    }

    @Override
    public void close() {
        gyroscopeEnabled = false;
        accelerometerEnabled = false;
        updateOperationMode();

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
        registerAccess.readRegister(Register.OUT_X_L_XL,  buffer.array(), 0, 6);

        values[0] = buffer.getShort(0) * accelerometerRange.ms2/ Short.MAX_VALUE;
        values[1] = buffer.getShort(2) * accelerometerRange.ms2 / Short.MAX_VALUE;
        values[2] = buffer.getShort(4) * accelerometerRange.ms2 / Short.MAX_VALUE;

        registerAccess.readRegister(Register.OUT_X_L_G, buffer.array(), 0, 6);

        values[3] = (gyroscopeRange.getDps() * buffer.getShort(0)) / Short.MAX_VALUE;
        values[4] = (gyroscopeRange.getDps() * buffer.getShort(2)) / Short.MAX_VALUE;
        values[5] = (gyroscopeRange.getDps() * buffer.getShort(4)) / Short.MAX_VALUE;
    }

    /** Returns a float array containing the gyroscope x, y and z-values in degree per second. */
    public float[] readGyroscope() {
        registerAccess.readRegister(Register.OUT_X_L_G, buffer.array(), 0, 6);
        return new float[] {
                (gyroscopeRange.getDps() * buffer.getShort(0)) / Short.MAX_VALUE,
                (gyroscopeRange.getDps() * buffer.getShort(2)) / Short.MAX_VALUE,
                (gyroscopeRange.getDps() * buffer.getShort(4)) / Short.MAX_VALUE
        };
    }

    /**
     * Returns the acceleration x, y and z-values in meter per second^2.
     * Note that the z-value will be ~0.981, measuring 1g caused by earth's gravity. */
    public float[] readAccelerometer() {
        registerAccess.readRegister(Register.OUT_X_L_XL,  buffer.array(), 0, 6);
        return new float[] {
                buffer.getShort(0) * accelerometerRange.ms2/ Short.MAX_VALUE,
                buffer.getShort(2) * accelerometerRange.ms2 / Short.MAX_VALUE,
                buffer.getShort(4) * accelerometerRange.ms2 / Short.MAX_VALUE
        };
    }

    public void setAccelerometerEnabled(boolean accelerometerEnabled) {
        if (this.accelerometerEnabled != accelerometerEnabled) {
            this.accelerometerEnabled = accelerometerEnabled;
            updateOperationMode();
        }
    }

    /** Enabling the gyroscope will implicitly enable the accelerometer */
    public void setGyroscopeEnabled(boolean gyroscopeEnabled) {
        if (this.gyroscopeEnabled != gyroscopeEnabled) {
            this.gyroscopeEnabled = gyroscopeEnabled;
            updateOperationMode();
        }
    }

    /**
     * Sets the output data rate. If the given data rate is not available, the next higher rate
     * will be selected.
     */
    public float setOutputDataRate(float hz) {
        int code;
        float rate;
        if (hz < 59.5) {
            rate = 14.9f;
            code = 0b001;
        } else if (hz < 119) {
            rate = 59.5f;
            code = 0b010;
        }else if (hz < 238) {
            rate = 119;
            code = 0b011;
        }else if (hz < 479) {
            rate = 238;
            code = 0b100;
        }else if (hz < 952) {
            rate = 497;
            code = 0b101;
        } else {
            rate = 952;
            code = 0b110;
        }
        if (code != outputDataRateCode) {
            outputDataRateCode = code;
            outputDataRate = rate;
            updateOperationMode();
        }
        return outputDataRate;
    }

    /** Sets the range for accelerometer values. */
    public void setAccelerometerRange(AccelerometerRange range) {
        setRegisterBits(Register.CTRL_REG6_XL, 4, 3, range.ordinal());
        accelerometerRange = range;
    }

    /** Sets the range for gyroscope values. */
    public void setGyroscopeRange(GyroscopeRange range) {
        setRegisterBits(Register.CTRL_REG1_G, 4, 3, range.ordinal());
        gyroscopeRange = range;
    }

    // Private helpers

    private void updateOperationMode() {
        // This is a bit weird: The documentation seems to say that writing to CTRL_REG6
        // will disable the gyrometer: "writing to CTRL_REG6_XL (20h), the accelerometer operates in
        // normal mode and the gyroscope is powered down".
        // Here, we order the calls to be safe wrt. to this.
        if (gyroscopeEnabled) {
            setRegisterBits(Register.CTRL_REG6_XL, 7, 5, accelerometerEnabled ? outputDataRateCode : 0);
            setRegisterBits(Register.CTRL_REG1_G, 7, 5, outputDataRateCode);
        } else if (accelerometerEnabled) {
            setRegisterBits(Register.CTRL_REG1_G, 7, 5, 0);
            setRegisterBits(Register.CTRL_REG6_XL, 7, 5, outputDataRateCode);
        } else {
            setRegisterBits(Register.CTRL_REG1_G, 7, 5, 0);
            setRegisterBits(Register.CTRL_REG6_XL, 7, 5, 0);
        }
    }

    private void setRegisterBits(int register, int high, int low, int value) {
        int count = high - low + 1;
        int mask = (((1 << count) - 1) << low);

        int registerValue = registerAccess.readRegister(register);
        int updatedValue = (registerValue & ~mask) | ((value << low) & mask);

        registerAccess.writeRegister(register, updatedValue);
    }


    // Public Enums

    /** Available accelerometer range G-values */
    public enum AccelerometerRange {
        // The order is important as the ordinal value is used to get the encoded value.
        G_2(2), G_16(16), G_4(4), G_8(8);

        private final float ms2;

        AccelerometerRange(int g) {
            this.ms2 = g * 0.981f;
        }

        public float getMs2() {
            return ms2;
        }
    }

    /** Available gyroscope range values (dps = degrees per second). */
    public enum GyroscopeRange {
        // The order is important as the ordinal value is used to get the encoded value.
        DPS_245(245), DPS_500(500), INVALID_VALUE(0), DPS_2000(2000);

        private final float dps;
        GyroscopeRange(float dps) {
            this.dps = dps;
        }
        public float getDps() {
            return dps;
        }
    }
}
