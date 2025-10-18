package com.pi4j.drivers.sensor.environment.hts221;

import com.pi4j.drivers.sensor.Sensor;
import com.pi4j.io.i2c.I2CRegisterDataReaderWriter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Basic HTS 221 driver supporting single-shot measurements.
 *
 * Datasheet: https://www.st.com/resource/en/datasheet/hts221.pdf
 */
public class Hts221Driver implements Sensor {
    public static final int I2C_ADDRESS = 0x5f;
    public static final Descriptor DESCRIPTOR = new Descriptor(
            new ValueDescriptor(0, ValueKind.HUMIDITY),
            new ValueDescriptor(1, ValueKind.TEMPERATURE));

    private static final int WHO_AM_I_VALUE = 0xbc;
    private static final int STATUS_TEMPERATURE_AVAILABLE_MASK = 1;
    private static final int STATUS_HUMIDITY_AVAILABLE_MASK = 2;

    private final ByteBuffer buffer = ByteBuffer.allocate(Register.ADDRESS_SPACE_END).order(ByteOrder.LITTLE_ENDIAN);
    private final I2CRegisterDataReaderWriter registerAccess;

    private final float calibH0Rh;
    private final float calibH1Rh;
    private final float calibT0DegC;
    private final float calibT1DegC;

    private final int calibH0T0Out;
    private final int calibH1T0Out;

    private final int calibT0Out;
    private final int calibT1Out;


    public Hts221Driver(I2CRegisterDataReaderWriter registerAccess) {
        this.registerAccess = registerAccess;

        int whoAmIValue = registerAccess.readRegister(Register.WHO_AM_I);
        if (whoAmIValue != WHO_AM_I_VALUE) {
            throw new IllegalStateException("WHO_AM_I register value " + whoAmIValue + " does not match expected value " + WHO_AM_I_VALUE);
        }

        // Enable the chip
        int ctrl1 = registerAccess.readRegister(Register.CTRL_REG1);
        registerAccess.writeRegister(Register.CTRL_REG1, ctrl1 | 0x80);

        // Read calibration data.
        // We map the registers into the same addresses in the buffer to simplify addressing.
        readRegisters(Register.CALIB_0, Register.CALIB_0, Register.CALIBRATION_DATA_SIZE);

        calibH0Rh = (buffer.get(Register.CALIB_H0_RH_X2) & 0xff) / 2f;
        calibH1Rh = (buffer.get(Register.CALIB_H1_RH_X2) & 0xff) / 2f;

        int calibT1T0Msb = buffer.get(Register.CALIB_T1_T0_MSB);

        calibT0DegC = ((buffer.get(Register.CALIB_T0_DEGC_X8) & 0xff) | ((calibT1T0Msb & 3) << 8)) / 8f;
        calibT1DegC = ((buffer.get(Register.CALIB_T1_DEGC_X8) & 0xff) | ((calibT1T0Msb & 12) << 6)) / 8f;

        calibH0T0Out = buffer.getShort(Register.CALIB_H0_T0_OUT);
        calibH1T0Out = buffer.getShort(Register.CALIB_H1_T0_OUT);

        calibT0Out = buffer.getShort(Register.CALIB_T0_OUT);
        calibT1Out = buffer.getShort(Register.CALIB_T1_OUT);
    }


    @Override
    public void close() {
        // Disable the chip
        int ctrl1 = registerAccess.readRegister(Register.CTRL_REG1);
        registerAccess.writeRegister(Register.CTRL_REG1, ctrl1 & 0x7f);
        if (registerAccess instanceof Closeable) {
            try {
                ((Closeable) registerAccess).close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Descriptor getDescriptor() {
        return DESCRIPTOR;
    }
    
    /** Reads a humidity value. Requests a single shot measurement if no data is available. */
    public float readHumidity() {
        requestData(STATUS_HUMIDITY_AVAILABLE_MASK);

        readRegisters(Register.HUMIDITY_OUT_L,0, 2);
        int rawHumidityOut = buffer.getShort(0);
        return (calibH1Rh - calibH0Rh)
                * (rawHumidityOut - calibH0T0Out) / (calibH1T0Out - calibH0T0Out)
                + calibH0Rh;
    }

    @Override
    public void readMeasurement(float[] values) {
        values[0] = readHumidity();
        values[1] = readTemperature();
    }

    /** Reads a temperature value. Requests a single shot measurement if no data is available. */
    public float readTemperature() {
        requestData(STATUS_TEMPERATURE_AVAILABLE_MASK); // Temperature bit in the status register

        readRegisters(Register.TEMP_OUT_L,0, 2);
        int rawTempOut = buffer.getShort(0);
        return (calibT1DegC - calibT0DegC)
                * (rawTempOut - calibT0Out) / (calibT1Out - calibT0Out)
                + calibT0DegC;
    }

    // Private helpers

    private void readRegisters(int register, int bufferOffset, int length) {
        registerAccess.readRegister(register | Register.AUTO_INCREMENT_FLAG, buffer.array(), bufferOffset, length);
    }

    private void requestData(int readyFlag) {
        int status = registerAccess.readRegister(Register.STATUS_REG);
        if ((status & readyFlag) == 0) {
            int ctrl2 = registerAccess.readRegister(Register.CTRL_REG2);
            registerAccess.writeRegister(Register.CTRL_REG2, ctrl2 | 1); // ONE_SHOT

            while ((registerAccess.readRegister(Register.STATUS_REG) & readyFlag) == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                   Thread.currentThread().interrupt();
                   throw new RuntimeException(e);
                }
            }
        }
    }
}
