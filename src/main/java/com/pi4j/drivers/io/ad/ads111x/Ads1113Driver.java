package com.pi4j.drivers.io.ad.ads111x;

import com.pi4j.drivers.sensor.Sensor;
import com.pi4j.drivers.sensor.SensorDescriptor;
import com.pi4j.io.i2c.I2C;
import com.pi4j.util.Delay;

/** A basic ADS 1113 Driver with support for single-shot mode */
public class Ads1113Driver implements Sensor {
    final Delay delay = new Delay();
    final byte[] buffer = new byte[2];
    final I2C i2c;

    OperatingMode operatingMode;
    InputVoltageRange inputVoltageRange = InputVoltageRange.V_2_048;

    public Ads1113Driver(I2C i2c) {
        this.i2c = i2c;
        int operatingModeOrdinal = readConfigBits(Constants.MODE_FLAG_POS, 1);
        operatingMode = OperatingMode.values()[operatingModeOrdinal];
    }

    public DataRate getDataRate() {
        int ordinal = readConfigBits(Constants.DR_OFFSET, Constants.DR_COUNT);
        return DataRate.values()[ordinal];
    }

    public InputVoltageRange getInputVoltageRange() {
        return inputVoltageRange;
    }

    public OperatingMode getOperatingMode() {
        return operatingMode;
    }

    /**
     * Requests a voltage reading, waits for availability and returns the raw 16 bit integer value,
     * sign-extended to 32 bit.
     */
    public int readRawValue() {
        if (operatingMode == OperatingMode.SINGLE_SHOT) {
            writeConfigBits(Constants.OS_FLAG_POS, 1, Constants.OS_START_SINGLE);
            while (readConfigBits(Constants.OS_FLAG_POS, 1) == Constants.OS_ONGOING_CONV) {
                delay.setMicros(100).materialize();
            }
        }
        return readRegister(Constants.CONVERSION_REG_ADDR);
    }

    /** Reads the analog input voltage, potentially blocking until it's available */
    public double readValue() {
        return readRawValue() * inputVoltageRange.value / 0x7fff;
    }

    public void setDataRate(DataRate dataRate) {
        writeConfigBits(Constants.DR_OFFSET, Constants.DR_COUNT, dataRate.ordinal());
    }

    public void setOperatingMode(OperatingMode mode) {
        this.operatingMode = mode;
        writeConfigBits(Constants.MODE_FLAG_POS, 1, mode.ordinal());
    }

    // Internal helpers

    int readConfigBits(int offset, int count) {
        int rawValue = readRegister(Constants.CONFIG_REG_ADDR);
        int mask = (1 << count) -1;
        return (rawValue >>> offset) & mask;
    }

    void writeConfigBits(int offset, int count, int value) {
        int oldValue = readRegister(Constants.CONFIG_REG_ADDR);
        int mask = ((1 << count) - 1) << offset;
        writeRegister(Constants.CONFIG_REG_ADDR, (oldValue & ~mask) | ((value << offset) & mask));
    }

    void writeRegister(int address, int value) {
        buffer[0] = (byte) (value >> 8);
        buffer[1] = (byte) value;
        i2c.writeRegister(address, buffer, 0, 2);
    }

    int readRegister(int address) {
        i2c.readRegister(address, buffer, 0, 2);
        // The implicit sign extension for buffer[0] below is intentional
        return (buffer[0] << 8) | (buffer[1] & 0xFF);
    }

    @Override
    public SensorDescriptor getDescriptor() {
        return Constants.DESCRIPTOR;
    }

    @Override
    public void readMeasurement(double[] values) {
        values[0] = readValue();
    }

    @Override
    public void close() {
        i2c.close();
    }
}
