package com.pi4j.drivers.sensor.geospatial.lsm9ds1;

/** LSM9DS1 Register name constants */
class Register {
    // Accelerometer / Gyroscope
    static final int ACT_THS = 4;
    static final int ACT_DUR = 5;
    static final int INT_GEN_CFG_XL = 6;
    static final int INT_GEN_THS_X_XL = 7;
    static final int INT_GEN_THS_Y_XL = 8;
    static final int INT_GEN_THS_Z_XL = 9;
    static final int INT_GEN_DUR_XL = 0x0a;
    static final int REFERENCE_G = 0x0b;
    static final int INT1_CTRL = 0x0c;
    static final int INT2_CTRL = 0x0d;
    static final int WHO_AM_I = 0x0f;

    static final int CTRL_REG1_G = 0x10;
    static final int CTRL_REG2_G = 0x11;
    static final int CTRL_REG3_G = 0x12;
    static final int ORIENT_CFG_G = 0x13;
    static final int INT_GEN_SRC_G = 0x14;
    static final int OUT_TEMP_L = 0x15;
    static final int OUT_TEMP_H = 0x16;
    static final int STATUS_REG_1 = 0x17;
    static final int OUT_X_L_G = 0x18;
    static final int OUT_X_H_G = 0x19;
    static final int OUT_Y_L_G = 0x1A;
    static final int OUT_Y_H_G = 0x1B;
    static final int OUT_Z_L_G = 0x1C;
    static final int OUT_Z_H_G = 0x1D;
    static final int CTRL_REG4 = 0x1E;
    static final int CTRL_REG5_XL = 0x1f;

    static final int CTRL_REG6_XL = 0x20;
    static final int CTRL_REG7_XL = 0x21;
    static final int CTRL_REG8 = 0x22;
    static final int CTRL_REG9 = 0x23;
    static final int CTRL_REG10 = 0x24;

    static final int INT_GEN_SRC_XL = 0x26;
    static final int STATUS_REG_2 = 0x27;
    static final int OUT_X_L_XL = 0x28;
    static final int OUT_X_H_XL = 0x29;
    static final int OUT_Y_L_XL = 0x2A;
    static final int OUT_Y_H_XL = 0x2B;
    static final int OUT_Z_L_XL = 0x2C;
    static final int OUT_Z_H_XL = 0x2D;
    static final int FIFO_CTRL = 0x2E;
    static final int FIFO_SRC = 0x2F;
    static final int INT_GEN_CFG_G = 0x30;
    static final int INT_GEN_THS_XH_G = 0x31;
    static final int INT_GEN_THS_XL_G = 0x32;
    static final int INT_GEN_THS_YH_G = 0x33;
    static final int INT_GEN_THS_YL_G = 0x34;
    static final int INT_GEN_THS_ZH_G = 0x35;
    static final int INT_GEN_THS_ZL_G = 0x36;
    static final int INT_GEN_DUR_G = 0x37;

    // Magnetometer

    static final int OFFSET_X_REG_L_M = 0x05;

    static final int OFFSET_X_REG_H_M = 0x06;
    static final int OFFSET_Y_REG_L_M = 0x07;
    static final int OFFSET_Y_REG_H_M = 0x08;
    static final int OFFSET_Z_REG_L_M = 0x09;
    static final int OFFSET_Z_REG_H_M = 0x0A;

    static final int WHO_AM_I_M = 0x0F;

    static final int CTRL_REG1_M = 0x20;

    static final int CTRL_REG2_M = 0x21;
    static final int CTRL_REG3_M = 0x22;
    static final int CTRL_REG4_M = 0x23;
    static final int CTRL_REG5_M = 0x24;

    static final int STATUS_REG_M = 0x27;
    static final int OUT_X_L_M = 0x28;

    static final int OUT_X_H_M = 0x29;
    static final int OUT_Y_L_M = 0x2A;
    static final int OUT_Y_H_M = 0x2B;
    static final int OUT_Z_L_M = 0x2C;
    static final int OUT_Z_H_M = 0x2D;

    static final int INT_CFG_M = 0x30;

    static final int INT_SRC_M = 0x31;

    static final int INT_THS_L_M = 0x32;
    static final int INT_THS_H_M = 0x33;
}
