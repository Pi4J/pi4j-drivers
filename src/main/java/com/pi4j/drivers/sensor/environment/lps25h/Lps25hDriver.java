package com.pi4j.drivers.sensor.environment.lps25h;

import com.pi4j.io.i2c.I2CRegisterDataReaderWriter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * https://www.st.com/resource/en/datasheet/lps25h.pdf
 */
public class Lps25hDriver {
    public static final int I2C_ADDRESS = 0x5c;

    private static final int WHO_AM_I_VALUE = 0xbd;
    private static final int STATUS_TEMPERATURE_AVAILABLE_MASK = 1;
    private static final int STATUS_PRESSURE_AVAILABLE_MASK = 2;

    private final I2CRegisterDataReaderWriter registerAccess;

    private final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);


    public Lps25hDriver(I2CRegisterDataReaderWriter registerAccess) {
        this.registerAccess = registerAccess;

        int whoAmIValue = registerAccess.readRegister(Register.WHO_AM_I);
        if (whoAmIValue != WHO_AM_I_VALUE) {
            throw new IllegalStateException("WHO_AM_I register value " + whoAmIValue + " does not match expected value " + WHO_AM_I_VALUE);
        }

        // Enable the chip
        int ctrl1 = registerAccess.readRegister(Register.CTRL_REG1);
        registerAccess.writeRegister(Register.CTRL_REG1, ctrl1 | 0x80);
    }

    public float readTemperature() {
        requestData(STATUS_TEMPERATURE_AVAILABLE_MASK);
        registerAccess.readRegister(Register.TEMP_OUT_L | Register.AUTO_INCREMENT_FLAG, buffer.array(), 0, 2);
        return buffer.getShort(0) / 480f + 42.5f;
    }


    public float readPressure() {
        requestData(STATUS_PRESSURE_AVAILABLE_MASK);
        registerAccess.readRegister(Register.PRESS_POUT_XL | Register.AUTO_INCREMENT_FLAG, buffer.array(), 0, 3);
        return buffer.getInt(0) / 4096f;
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
