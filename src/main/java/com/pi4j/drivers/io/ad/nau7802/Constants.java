package com.pi4j.drivers.io.ad.nau7802;

/** Internal constants */
class Constants {
    static final int CAL_SUCCESS = 0;
    static final int CAL_IN_PROGRESS = 1;
    static final int CAL_FAILURE = 2;

    static final int CALMOD_INTERNAL = 0;
    static final int CALMOD_OFFSET = 2;
    static final int CALMOD_GAIN = 3;

    /** Register numbers */
    static class Register {
        static final int PU_CTRL = 0;
        static final int CTRL1 = 1;
        static final int CTRL2 = 2;
        static final int OCAL1_B2 = 3;
        static final int OCAL1_B1 = 4;
        static final int OCAL1_B0 = 5;
        static final int GCAL1_B3 = 6;
        static final int GCAL1_B2 = 7;
        static final int GCAL1_B1 = 8;
        static final int GCAL1_B0 = 9;
        static final int OCAL2_B2 = 10;
        static final int OCAL2_B1 = 11;
        static final int OCAL2_B0 = 12;
        static final int GCAL2_B3 = 13;
        static final int GCAL2_B2 = 14;
        static final int GCAL2_B1 = 15;
        static final int GCAL2_B0 = 16;
        static final int I2C_CONTROL = 17;
        static final int ADCO_B2 = 18;
        static final int ADCO_B1 = 19;
        static final int ADCO_B0 = 20;
        static final int ADC = 0x15;
        static final int OTP_B1 = 0x16;
        static final int OTP_B0 = 0x17;
        static final int PGA = 0x1B;
        static final int PGA_PWR = 0x1C;
        static final int DEVICE_REV = 0x1F;
    }

    /** Bit names for various registers */
    static class Bit {
        static final int PU_CTRL_RR = 0;
        static final int PU_CTRL_PUD = 1;
        static final int PU_CTRL_PUA = 2;
        static final int PU_CTRL_PUR = 3;
        static final int PU_CTRL_CS = 4;
        static final int PU_CTRL_CR = 5;
        static final int PU_CTRL_OSCS = 6;
        static final int PU_CTRL_AVDDS = 7;

        static final int CTRL1_GAIN = 2;
        static final int CTRL1_VLDO = 5;
        static final int CTRL1_DRDY_SEL = 6;
        static final int CTRL1_CRP = 7;

        static final int CTRL2_CALMOD = 0;
        static final int CTRL2_CALS = 2;
        static final int CTRL2_CAL_ERROR = 3;
        static final int CTRL2_CRS = 4;
        static final int CTRL2_CHS = 7;

        static final int PGA_CHP_DIS = 0;
        static final int PGA_INV = 3;
        static final int PGA_BYPASS_EN = 4;
        static final int PGA_OUT_EN = 5;
        static final int PGA_LDOMODE = 6;
        static final int PGA_RD_OTP_SEL = 7;

        static final int PGA_PWR_PGA_CURR = 0;
        static final int PGA_PWR_ADC_CURR = 2;
        static final int PGA_PWR_MSTR_BIAS_CURR = 4;
        static final int PGA_PWR_PGA_CAP_EN = 7;
    }

}
