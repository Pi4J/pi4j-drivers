/*
 * Copyright (C) 2012 - 2025 Pi4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pi4j.drivers.sensor.environment.bmx280;

/**
 * Internal constants for register address, values and masks
 */
class Bmp280Constants {

    /*  Begin device register definitions. */
    static final int TEMP_XLSB = 0xFC;
    static final int TEMP_LSB = 0xFB;
    static final int TEMP_MSB = 0xFA;
    static final int PRESS_XLSB = 0xF9;
    static final int PRESS_LSB = 0xF8;
    static final int PRESS_MSB = 0xF7;
    static final int CONFIG = 0xF5;
    static final int CTRL_MEAS = 0xF4;
    static final int STATUS = 0xF3;
    static final int RESET = 0xE0;
    static final int CHIP_ID = 0xD0;


    // errata register definitions
    static final int REG_DIG_T1 = 0x88;
    static final int REG_DIG_T2 = 0x8A;
    static final int REG_DIG_T3 = 0x8C;

    static final int REG_DIG_P1 = 0x8E;
    static final int REG_DIG_P2 = 0x90;
    static final int REG_DIG_P3 = 0x92;
    static final int REG_DIG_P4 = 0x94;
    static final int REG_DIG_P5 = 0x96;
    static final int REG_DIG_P6 = 0x98;
    static final int REG_DIG_P7 = 0x9A;
    static final int REG_DIG_P8 = 0x9C;
    static final int REG_DIG_P9 = 0x9E;

    // register contents
    static final int RESET_CMD = 0xB6;  // written to reset

    // Pertaining to 0xF3 status register
    static final int STAT_MEASURE = 0x08;  // set, conversion running
    static final int STAT_UPDATE = 0x01;  // set, NVM being copied

    // Pertaining to 0xF4 ctrl_meas register
    static final int TEMP_OVER_SAMPLE_MSK = 0xE0;  // mask bits 5,6,7
    static final int PRES_OVER_SAMPLE_MSK = 0x1C;  // mask bits 2,3,4
    static final int PWR_MODE_MSK = 0x03;  // mask bits 0,1


    // Pertaining to 0xF5 config register
    static final int INACT_DURATION_MSK = 0xE0;  // mask bits 5,6,7
    static final int IIR_FLT_MSK = 0x1C;  // mask bits 2,3,4
    static final int ENABLE_SPI_MSK = 0x01;  // mask bits 0

    // Pertaining to 0xF7 0xF8 0xF9 press  register
    static final int PRESS_MSB_MSK = 0xFF;  // mask bits 0 - 7
    static final int PRESS_LSB_MSK = 0xFF;  // mask bits 0 - 7
    static final int PRESS_XLSB_MSK = 0x0F;  // mask bits 0 - 3

    // Pertaining to 0xFA 0xFB 0xFC temp  register
    static final int TEMP_MSB_MSK = 0xFF;  // mask bits 0 - 7
    static final int TEMP_LSB_MSK = 0xFF;  // mask bits 0 - 7
    static final int TEMP_XLSB_MSK = 0x0F;  // mask bits 0 - 3
    static final int ID_VALUE_BMP = 0x58;   // expected chpId value BMP280
    static final int ID_VALUE_BME = 0x60;   // expected chpId value BME280

    // For the control reg 0xf4
    static final int POWERMODE_SLEEP = 0x00;
    static final int POWERMODE_FORCED = 0x01;
    static final int POWERMODE_NORMAL = 0x02;

    static final int NO_OVERSAMPLING  = 0x00;
    static final int OVERSAMPLING_1X  = 0x01;
    static final int OVERSAMPLING_2X  = 0x02;
    static final int OVERSAMPLING_4X   = 0x03;
    static final int OVERSAMPLING_8X  = 0x04;
    static final int OVERSAMPLING_16X = 0x05;
    static final int OVERSAMPLING_MAX = 16;


    static final int CTRL_HUM_MSK          = 0x07;
    static final int CTRL_HUM_POS          = 0x00;
    static final int CTRL_PRESS_MSK        = 0x1C;
    static final int CTRL_PRESS_POS        = 0x02;
    static final int CTRL_TEMP_MSK         = 0xE0;
    static final int CTRL_TEMP_POS         = 0x05;

    static final int CTL_TEMP_SAMP_1 = 0x20;   // oversample *1
    static final int CTL_PRCTL_FORCEDESS_SAMP_1 = 0x04;   // oversample *1


}
