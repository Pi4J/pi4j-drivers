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
package com.pi4j.driver.sensor.bmx280;

import com.pi4j.io.i2c.I2CRegisterDataReaderWriter;
import com.pi4j.io.spi.Spi;

/**
 * Internal helper that implements BMx280 register access for SPI mode.
 */
class SpiRegisterAccess implements I2CRegisterDataReaderWriter {
    private final Spi spi;

    public SpiRegisterAccess(Spi spi) {
        this.spi = spi;
    }

    @Override
    public int readRegister(int register) {
        spi.write((byte) (0b10000000 | register));
        return spi.readByte();
    }

    // Not used in Bmx280Driver
    @Override
    public int readRegister(byte[] register, byte[] data, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readRegister(int register, byte[] data, int offset, int length) {
        spi.write((byte) (0b10000000 | register));
        return spi.read(data, offset, length);
    }


    @Override
    public int writeRegister(int register, byte data) {
        // send read request to BMP chip via SPI channel
        return spi.write((byte) (0b01111111 & register), data);
    }


    @Override
    public int writeRegister(int register, byte[] buffer, int i1, int i2) {
        spi.write((byte) (0b01111111 & register));
        return spi.write(buffer, i1, i2);
    }

    // Not used in Bmx280Driver
    @Override
    public int writeRegister(byte[] bytes, byte[] bytes1, int i, int i1) {
        throw new UnsupportedOperationException();
    }
}
