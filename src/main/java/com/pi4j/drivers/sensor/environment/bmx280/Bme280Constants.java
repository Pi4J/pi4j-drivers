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

/** Additional constants for the BME280 */
class Bme280Constants {
    static final int REG_DIG_H1 = 0xA1;
    static final int REG_DIG_H2 = 0xE1;
    static final int REG_DIG_H3 = 0xE3;
    static final int REG_DIG_H4 = 0xE4;
    static final int REG_DIG_H5 = 0xE5;
    static final int REG_DIG_H6 = 0xE7;

    static final int CTRL_HUM = 0xF2;
    static final int HUM_MSB = 0xFD;
    static final int HUM_LSB = 0xFE;

    static int CTRL_HUM_SAMP_1 = 0x01;
    static int CTRL_HUM_MSK = 7;
}
