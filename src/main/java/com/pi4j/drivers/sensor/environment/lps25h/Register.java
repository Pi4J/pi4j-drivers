package com.pi4j.drivers.sensor.environment.lps25h;

class Register {
    static final int AUTO_INCREMENT_FLAG = 0x80;

    static final int REF_P_XL = 0x08;
    static final int REF_P_L = 0x09;
    static final int REF_P_H = 0x0A;
    static final int WHO_AM_I = 0x0F;
    static final int RES_CONF = 0x10;

    static final int CTRL_REG1 = 0x20;
    static final int CTRL_REG2 = 0x21;
    static final int CTRL_REG3 = 0x22;
    static final int CTRL_REG4 = 0x23;
    static final int INT_CFG = 0x24;
    static final int INT_SOURCE = 0x25;

    static final int STATUS_REG = 0x27;
    static final int PRESS_POUT_XL = 0x28;
    static final int PRESS_OUT_L = 0x29;
    static final int PRESS_OUT_H = 0x2A;
    static final int TEMP_OUT_L = 0x2B;
    static final int TEMP_OUT_H = 0x2C;

    static final int FIFO_CTRL = 0x2E;
    static final int FIFO_STATUS = 0x2F;
    static final int THS_P_L = 0x30;
    static final int THS_P_H = 0x31;

    static final int RPDS_L = 0x39;
    static final int RPDS_H = 0x3A;
}
