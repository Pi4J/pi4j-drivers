package com.pi4j.drivers.io.ad.ads111x;

import com.pi4j.drivers.sensor.SensorDescriptor;

import java.util.List;

public class Constants {
    /** Address if the address pin is connected to GND. */
    public static final int I2C_ADDRESS_GND		 	= 0b1001000;
    /** Address if the address pin is connected to VDD. */
    public static final int I2C_ADDRESS_VDD			= 0b1001001;
    /** Address if the address pin is connected to SDA. */
    public static final int I2C_ADDRESS_SDA			= 0b1001010;
    /** Address if the address pin is connected to SCL. */
    public static final int I2C_ADDRESS_SCL			= 0b1001011;

    public static final SensorDescriptor DESCRIPTOR = new SensorDescriptor(
            "ADS111x",
            List.of(new SensorDescriptor.Value(0, SensorDescriptor.Kind.VOLTAGE)),
            List.of(I2C_ADDRESS_GND, I2C_ADDRESS_SDA, I2C_ADDRESS_SCL, I2C_ADDRESS_VDD),
            null);

    static final int CONVERSION_REG_ADDR = 0;
    static final int CONFIG_REG_ADDR = 1;
    static final int LOW_TRESH_REG_ADDR = 2;
    static final int HIGH_TRESH_REG_ADDR = 3;

    static final int CONVERSION_REG_DEFAULT = 0x0000;
    static final int CONFIG_REG_DEFAULT = 0x8583;
    static final int LOW_TRESH_REG_DEFAULT = 0x8000;
    static final int HIGH_TRESH_REG_DEFAULT = 0x7FFF;

    static final int OS_FLAG_POS = 15;
    static final int MUX_OFFSET = 12;
    static final int MUX_COUNT = 3;
    static final int PGA_OFFSET = 9;
    static final int PGA_COUNT = 3;
    static final int MODE_FLAG_POS = 8;
    static final int DR_OFFSET = 5;
    static final int DR_COUNT = 3;
    static final int COMP_MODE_FLAG_POS = 4;
    static final int COMP_POL_FLAG_POS = 3;
    static final int COMP_LAT_FLAG_POS = 2;
    static final int COMP_QUEUE_COUNT = 2;
    static final int COMP_QUEUE_POS = 0;

    static final int OS_START_SINGLE = 1;
    static final int OS_ONGOING_CONV = 0;
    static final int OS_NO_CONV = 1;

    static final int PGA_6_144 = 0;
    static final int PGA_4_096 = 1;
    static final int PGA_2_048 = 2;
    static final int PGA_1_024 = 3;
    static final int PGA_0_512 = 4;
    static final int PGA_0_256 = 5;


    static final double PGA_6_144_MULT = 0.1875;
    static final double PGA_4_096_MULT = 0.125;
    static final double PGA_2_048_MULT = 0.0625;
    static final double PGA_1_024_MULT = 0.03125;
    static final double PGA_0_512_MULT = 0.0015625;
    static final double PGA_0_256_MULT = 0.00078125;
}
