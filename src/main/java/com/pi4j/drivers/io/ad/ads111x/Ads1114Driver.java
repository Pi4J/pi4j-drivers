package com.pi4j.drivers.io.ad.ads111x;

import com.pi4j.io.i2c.I2C;

/** Adds comparator capabilities to the base chip model. */
public class Ads1114Driver extends Ads1113Driver {
    public Ads1114Driver(I2C i2c) {
        super(i2c);
        int inputVoltageRangeOrdinal = readConfigBits(Constants.PGA_OFFSET, Constants.PGA_COUNT);
        InputVoltageRange[] values = InputVoltageRange.values();
        inputVoltageRange = inputVoltageRangeOrdinal >= values.length ? InputVoltageRange.V_0_256 : values[inputVoltageRangeOrdinal];
    }

    public ComparatorPolarity getComparatorPolarity() {
        int ordinal = readConfigBits(Constants.COMP_POL_FLAG_POS, 1);
        return ComparatorPolarity.values()[ordinal];
    }

    public ComparatorQueue getComparatorQueue() {
        int ordinal = readConfigBits(Constants.COMP_QUEUE_POS, Constants.COMP_QUEUE_COUNT);
        return ComparatorQueue.values()[ordinal];
    }

    public int getRawHighThreshold() {
        return readRegister(Constants.HIGH_TRESH_REG_ADDR);
    }

    public int getRawLowThreshold() {
        return readRegister(Constants.LOW_TRESH_REG_ADDR);
    }

    public boolean isLatchingComparator() {
        return readConfigBits(Constants.COMP_LAT_FLAG_POS, 1) != 0;
    }

    public boolean isWindowComparator() {
        return readConfigBits(Constants.COMP_MODE_FLAG_POS, 1) != 0;
    }

    public void setComparatorPolarity(ComparatorPolarity polarity) {
        writeConfigBits(Constants.COMP_POL_FLAG_POS, 1, polarity.ordinal());
    }

    public void setComparatorQueue(ComparatorQueue queue) {
        writeConfigBits(Constants.COMP_QUEUE_POS, Constants.COMP_QUEUE_COUNT, queue.ordinal());
    }

    public void setLatchingComparator(boolean value) {
        writeConfigBits(Constants.COMP_LAT_FLAG_POS,1, value ? 1 : 0);
    }

    public void setInputVoltageRange(InputVoltageRange range) {
        writeConfigBits(Constants.PGA_OFFSET, Constants.PGA_COUNT, range.ordinal());
        this.inputVoltageRange = range;
    }

    public void setWindowComparator(boolean value) {
        writeConfigBits(Constants.COMP_MODE_FLAG_POS, 1, value ? 1 : 0);
    }

    public void setRawLowThreshold(int value) {
        writeRegister(Constants.LOW_TRESH_REG_ADDR, value);
    }

    public void setRawHighThreshold(int value) {
        writeRegister(Constants.HIGH_TRESH_REG_ADDR, value);
    }

}
