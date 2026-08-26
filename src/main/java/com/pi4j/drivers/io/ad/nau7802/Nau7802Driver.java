package com.pi4j.drivers.io.ad.nau7802;

import com.pi4j.io.i2c.I2C;
import com.pi4j.util.Delay;

/** A basic driver for the NAU 7802 -- a special-purpose ADC for weigh scales. */
public class Nau7802Driver {
    public static final int I2C_ADDRESS = 0x2a;

    private final Delay delay = new Delay();
    private final I2C i2c;
    private final byte[] buffer = new byte[3];

    public Nau7802Driver(I2C i2c) {
        this.i2c = i2c;
    }

    /** Reads a signed 24 bit analog value. */
    public int read() {
        i2c.readRegister(Constants.Register.ADCO_B2, buffer, 0, 3);
        // Note that the implicit sign extension for buffer[0] below is intentional.
        return (buffer[0] << 16) | ((buffer[1] & 0xFF) << 8) | (buffer[2] & 0xFF);
    }

    /** Resets the chip */
    public void reset() {
        setBit(Constants.Bit.PU_CTRL_RR, Constants.Register.PU_CTRL);
        delay.setMillis(1).materialize();
        clearBit(Constants.Bit.PU_CTRL_RR, Constants.Register.PU_CTRL);
    }

    /** Powers the chip up and starts measuring */
    public void powerUp() {
        setBit(Constants.Bit.PU_CTRL_PUD, Constants.Register.PU_CTRL);
        setBit(Constants.Bit.PU_CTRL_PUA, Constants.Register.PU_CTRL);

        for (int i = 0; i < 128; i++) {
           if (getBit(Constants.Bit.PU_CTRL_PUR, Constants.Register.PU_CTRL)) {
               setBit(Constants.Bit.PU_CTRL_CS, Constants.Register.PU_CTRL);
               return;
           }
           delay.setMillis(1).materialize();
        }
        throw new IllegalStateException("Power up failed");
    }

    public void powerDown() {
        clearBit(Constants.Bit.PU_CTRL_PUD, Constants.Register.PU_CTRL);
        clearBit(Constants.Bit.PU_CTRL_PUA, Constants.Register.PU_CTRL);
    }

    public void setSampleRate(SampleRate sampleRate) {
        int value = i2c.readRegister(Constants.Register.CTRL2);
        i2c.writeRegister(Constants.Register.CTRL2, (value & 0b10001111) | (sampleRate.code << 4));
    }

    public void setChannel(Channel channel) {
        if (channel == Channel.CHANNEL_1) {
            clearBit(Constants.Bit.CTRL2_CHS, Constants.Register.CTRL2);
        } else {
            setBit(Constants.Bit.CTRL2_CHS, Constants.Register.CTRL2);
        }
    }

    // Private Helpers

    private void clearBit(int bit, int register) {
        int value = i2c.readRegister(register);
        i2c.writeRegister(register, value & ~(1 << bit));
    }

    private void setBit(int bit, int register) {
        int value = i2c.readRegister(register);
        i2c.writeRegister(register, value | (1 << bit));
    }

    private boolean getBit(int bit, int register) {
        return (i2c.readRegister(register) & (1 << bit)) != 0;
    }

    // Public enums

    public enum LowDropOut {
        LDO_4V5,
        LDO_4V2,
        LDO_3V9,
        LDO_3V6,
        LDO_3V3,
        LDO_3V0,
        LDO_2V7,
        LDO_2V4
    }

    public enum Gain {
        GAIN_1,
        GAIN_2,
        GAIN_4,
        GAIN_8,
        GAIN_16,
        GAIN_32,
        GAIN_64,
        GAIN_128
    }

    public enum Channel {
        CHANNEL_1,
        CHANNEL_2
    }


    public enum SampleRate {
        SPS_320(0b111),
        SPS_80(0b011),
        SPS_40(0b010),
        SPS_20(0b001),
        SPS_10(0b000);

        final int code;

        SampleRate(int code) {
            this.code = code;
        }
    }



}
