package com.pi4j.drivers.sensor.environment.hts221;

class Register {
    static final int AUTO_INCREMENT_FLAG = 0x80;

    static final int WHO_AM_I = 0x0F;
    static final int AV_CONF = 0x10; // 0x1b

    static final int CTRL_REG1 = 0x20;
    static final int CTRL_REG2 = 0x21;
    static final int CTRL_REG3 = 0x22;

    static final int STATUS_REG = 0x27;
    static final int HUMIDITY_OUT_L = 0x28;
    static final int HUMIDITY_OUT_H = 0x29;
    static final int TEMP_OUT_L = 0x2A;
    static final int TEMP_OUT_H = 0x2B;

    static final int CALIB_0 = 0x30;

    static final int CALIB_H0_RH_X2 = 0x30;
    static final int CALIB_H1_RH_X2 = 0x31;

    static final int CALIB_T0_DEGC_X8 = 0x32;
    static final int CALIB_T1_DEGC_X8 = 0x33;

    static final int CALIB_T1_T0_MSB = 0x35;

    static final int CALIB_H0_T0_OUT = 0x36;
    static final int CALIB_H1_T0_OUT = 0x3a;

    static final int CALIB_T0_OUT = 0x3c;
    static final int CALIB_T1_OUT = 0x3e;

    static final int ADDRESS_SPACE_END = 0x40;
    static final int CALIBRATION_DATA_SIZE = 0x10;
}
