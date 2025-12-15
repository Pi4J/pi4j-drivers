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

import com.pi4j.context.Context;
import com.pi4j.drivers.sensor.Sensor;
import com.pi4j.drivers.sensor.SensorDescriptor;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CRegisterDataReaderWriter;
import com.pi4j.io.spi.Spi;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Driver for BME 280 and BMP 280 chips.
 * <p>
 * Datasheet: https://www.bosch-sensortec.com/media/boschsensortec/downloads/datasheets/bst-bme280-ds002.pdf
 * <p>
 * This implementation is based on corresponding examples from the pi4-example-devices project.
 */
public class Bmx280Driver implements Sensor {

    public final static int ADDRESS_BMP_280 = 0x077;
    public final static int ADDRESS_BME_280_PRIMARY = 0x076;
    public final static int ADDRESS_BME_280_SECONDARY = 0x077;

    public final static SensorDescriptor DESCRIPTOR_BMP_280 = new SensorDescriptor.Builder()
            .addValue(SensorDescriptor.Kind.TEMPERATURE)
            .addValue(SensorDescriptor.Kind.PRESSURE)
            .addI2cAddress(ADDRESS_BMP_280)
            .setI2cSensorDetector(i2c -> i2c.readRegister(Bmp280Constants.CHIP_ID) == Bmp280Constants.ID_VALUE_BMP ? new Bmx280Driver(i2c) : null)
            .build();

    public final static SensorDescriptor DESCRIPTOR_BME_280 = new SensorDescriptor.Builder()
            .addValue(SensorDescriptor.Kind.TEMPERATURE)
            .addValue(SensorDescriptor.Kind.PRESSURE)
            .addValue(SensorDescriptor.Kind.HUMIDITY)
            .addI2cAddress(ADDRESS_BME_280_PRIMARY)
            .addI2cAddress(ADDRESS_BME_280_SECONDARY)
            .setI2cSensorDetector(i2c -> i2c.readRegister(Bmp280Constants.CHIP_ID) == Bmp280Constants.ID_VALUE_BME ? new Bmx280Driver(i2c) : null)
            .build();

    private final static double[] BME_280_STANDBY_TIMES = {0.5, 62.5, 125, 250, 500, 1000, 2000, 4000};
    private final static double[] BMP_280_STANDBY_TIMES = {0.5, 62.5, 125, 250, 500, 1000, 10, 20};

    private final I2CRegisterDataReaderWriter registerAccess;
    private final Model model;

    /** Calibration values for temperature */
    private final int digT1, digT2, digT3;
    /** Calibration values for pressure */
    private final int digP1, digP2, digP3, digP4, digP5, digP6, digP7, digP8, digP9;
    /** Calibration values for humidity */
    private final int digH1, digH2, digH3, digH4, digH5, digH6;

    // ByteBuffer doesn't seem to help a lot, given mixed big and little endian access.
    private final byte[] ioBuf = new byte[8];

    private MeasurementMode measurementMode = MeasurementMode.SLEEPING;
    private Instant busyUntil = Instant.now();
    private int standByTimeIndex = 0;
    private int filterCoefficientIndex = 0;
    private boolean spi3WireMode = false;

    private SensorMode temperatureMode = SensorMode.ENABLED;
    private SensorMode pressureMode = SensorMode.ENABLED;
    private SensorMode humidityMode;

    public static Bmx280Driver detect(Context context, int bus) {
        int[] addresses = new int[] {ADDRESS_BME_280_PRIMARY, ADDRESS_BME_280_SECONDARY};
        for (int address: addresses) {
            try {
                I2C i2c = context.create(I2C.newConfigBuilder(context).bus(bus).device(address));
                Bmx280Driver driver = new Bmx280Driver(i2c);
                return driver;
            } catch (Exception e) {
                // Ignore all exceptions
            }
        }
        return null;
    }


    /**
     * Creates a BMx280 SPI driver using the given Spi instance. As the device requires csb to remain low
     * during each register access, a dedicated pin is needed (opposed to a "standard" cs pin managed by the
     * SPI driver.
     */
    public Bmx280Driver(Spi spi, DigitalOutput csb) {
        this (new SpiRegisterAccess(spi, csb));
    }

    /**
     * Creates a BMx280 I2C driver using the given parameter
     */
    public Bmx280Driver(I2C i2c) {
        this ((I2CRegisterDataReaderWriter) i2c);
    }

    // Visible for testing
    protected Bmx280Driver(I2CRegisterDataReaderWriter registerAccess) {
        this.registerAccess = registerAccess;

        int chipId = registerAccess.readRegister(Bmp280Constants.CHIP_ID);
        if (chipId == Bmp280Constants.ID_VALUE_BMP) {
            model = Model.BMP280;
            digH1 = digH2 = digH3 = digH4 = digH5 = digH6 = 0;
            humidityMode = SensorMode.DISABLED;

        } else if (chipId == Bmp280Constants.ID_VALUE_BME) {
            model = Model.BME280;

            digH1 = registerAccess.readRegister(Bme280Constants.REG_DIG_H1);
            digH2 = readRegisterS16(Bme280Constants.REG_DIG_H2);
            digH3 = registerAccess.readRegister(Bme280Constants.REG_DIG_H3);

            int e4 = registerAccess.readRegister(0xe4);
            int e5 = registerAccess.readRegister(0xe5);

            int h4Hsb = e4 * 16;
            int h4Lsb = e5 & 0x0f;
            digH4 = h4Hsb | h4Lsb;

            int e6 = registerAccess.readRegister(0xe6);

            int h5Lsb = e5 >> 4;
            int h5Hsb = e6 * 16;
            digH5 = h5Hsb | h5Lsb;

            // Casting to byte will force a sign extension
            digH6 = (byte) registerAccess.readRegister(Bme280Constants.REG_DIG_H6);
            humidityMode = SensorMode.ENABLED;

        } else {
            throw new IllegalStateException("Unrecognized chip ID: " + chipId);
        }

        // Read calibration values.

        digT1 = readRegisterU16(Bmp280Constants.REG_DIG_T1);
        digT2 = readRegisterS16(Bmp280Constants.REG_DIG_T2);
        digT3 = readRegisterS16(Bmp280Constants.REG_DIG_T3);

        digP1 = readRegisterU16(Bmp280Constants.REG_DIG_P1);
        digP2 = readRegisterS16(Bmp280Constants.REG_DIG_P2);
        digP3 = readRegisterS16(Bmp280Constants.REG_DIG_P3);
        digP4 = readRegisterS16(Bmp280Constants.REG_DIG_P4);
        digP5 = readRegisterS16(Bmp280Constants.REG_DIG_P5);
        digP6 = readRegisterS16(Bmp280Constants.REG_DIG_P6);
        digP7 = readRegisterS16(Bmp280Constants.REG_DIG_P7);
        digP8 = readRegisterS16(Bmp280Constants.REG_DIG_P8);
        digP9 = readRegisterS16(Bmp280Constants.REG_DIG_P9);
    }

    /**
     * Returns the instant when the chip will be ready for new commands after processing the last command.
     * This can be used for scheduling purposes, avoiding forced sleep time when the next command is issued.
     */
    public Instant getBusyUntil() {
        return busyUntil;
    }

    @Override
    public void close() {
        if (registerAccess instanceof Closeable) {
            try {
                ((Closeable) registerAccess).close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public SensorDescriptor getDescriptor() {
        return model == Model.BME280 ? DESCRIPTOR_BME_280 : DESCRIPTOR_BMP_280;
    }

    /**
     * If the mode doesn't match the current mode, send all settings to the chip and set the BMP/E280 measurement mode.
     */
    public void setMeasurementMode(MeasurementMode mode) {
        if (measurementMode == mode) {
            return;
        }
        this.measurementMode = mode;

        materializeDelay();

        int config = (spi3WireMode ? 1 : 0)
                | (filterCoefficientIndex << 2)
                | (standByTimeIndex << 5);
        registerAccess.writeRegister(Bmp280Constants.CONFIG, config);

        if (model == Model.BME280) {
            int ctlHum = registerAccess.readRegister(Bme280Constants.CTRL_HUM);
            ctlHum = (ctlHum & ~Bme280Constants.CTRL_HUM_MSK) | humidityMode.ordinal();
            registerAccess.writeRegister(Bme280Constants.CTRL_HUM, ctlHum);
        }

        int ctlReg = Bmp280Constants.POWERMODE_FORCED
                | (temperatureMode.ordinal() << Bmp280Constants.CTRL_TEMP_POS)
                | (pressureMode.ordinal() << Bmp280Constants.CTRL_PRESS_POS);

        registerAccess.writeRegister(Bmp280Constants.CTRL_MEAS, ctlReg);

        setDelayMs((int) Math.ceil(getMeasurementTime()));
    }

    /** Measurement time for the current sensor modes in milliseconds, as documented in section 9.1 */
    public float getMeasurementTime() {
        return (1.25f +
                (temperatureMode == SensorMode.DISABLED ? 0 : (2.3f * (1 << temperatureMode.ordinal()) + 0.5f)) +
                (pressureMode == SensorMode.DISABLED ? 0 : (2.3f * (1 << pressureMode.ordinal()) + 0.575f)) +
                (temperatureMode == SensorMode.DISABLED ? 0 : 2.3f * (1 << temperatureMode.ordinal()) + 0.575f));
    }

    /**
     * Sets the standby time in milliseconds, selecting the closest available value (depending on the sensor).
     * The selected value is returned. The total interval time in CONTINUOUS measurement mode consists of the
     * measurement time and the standby time.
     */
    public double setStandbyTime(double ms) {
        assertSleepingModeForSettings();
        double[] list = model == Model.BMP280 ? BMP_280_STANDBY_TIMES : BME_280_STANDBY_TIMES;
        double bestDelta = Double.POSITIVE_INFINITY;
        for (int i = 0; i < list.length; i++) {
            double delta = Math.abs(list[i] - ms);
            if (delta < bestDelta) {
                bestDelta = Math.abs(list[i] - ms);
                standByTimeIndex = i;
            }
        }
        return list[standByTimeIndex];
    }

    /**
     * Sets the SPI 3 wire mode.
     */
    public void setSpi3WireMode(boolean enable) {
        assertSleepingModeForSettings();
        spi3WireMode = enable;
    }

    /**
     * Sets the IIR filter coefficient to the best match of the requested coefficient (0, 2, 4, 8 or 16).
     * The best available match is returned.
     */
    public int setFilterCoefficient(int coefficient) {
        assertSleepingModeForSettings();
        int index = (int) Math.round(Math.log(coefficient / 2.0)/Math.log(2));
        filterCoefficientIndex = index < 0 ? 0 : index > 8 ? 8 : index;
        return filterCoefficientIndex == 0 ? 0 : (2 >> filterCoefficientIndex);
    }

    /** Disables or enables temperature measurement in the given oversampling mode */
    public void setTemperatureMode(SensorMode mode) {
        assertSleepingModeForSettings();
        this.temperatureMode = mode;
    }

    /** Disables or enables pressure measurement in the given oversampling mode */
    public void setPressureMode(SensorMode mode) {
        assertSleepingModeForSettings();
        this.pressureMode = mode;
    }

    /** Disables or enables humidity measurement in the given oversampling mode */
    public void setHumidityMode(SensorMode mode) {
        assertSleepingModeForSettings();
        this.humidityMode = mode;
    }

    /**
     * Read measure registers 0xF7 - 0xFC in single read to ensure all the data pertains to
     * a single measurement. The result is returned in a "Measurement" instance.
     * <p>
     * If the current mode is SLEEPING, a single measurement will be requested and the code will block
     * for the time determined by getMeasurementTime.
     * <p>
     * Blocking can be avoided by setting FORCED or NORMAL mode ahead of time.
     */
    public Measurement readMeasurement() {
        if (measurementMode == MeasurementMode.SLEEPING) {
            setMeasurementMode(MeasurementMode.FORCED);
        }

        materializeDelay();

        registerAccess.readRegister(Bmp280Constants.PRESS_MSB, ioBuf, 0, model == Model.BME280 ? 8 : 6);

        float adcT = ((ioBuf[3] & 0xFF) << 12) + ((ioBuf[4] & 0xFF) << 4) + (ioBuf[5] & 0xFF);
        float adcP = ((ioBuf[0] & 0xFF) << 12) + ((ioBuf[1] & 0xFF) << 4) + (ioBuf[2] & 0xFF);

        // Temperature
        double var1 = (adcT / 16384.0 - digT1 / 1024.0) * digT2;
        double var2 = ((adcT / 131072.0 - digT1 / 8192.0) *
                    (adcT / 131072.0 - digT1 / 8192.0)) * digT3;
        double tFine = var1 + var2;
        double temperature = tFine / 5120.0;

        // Pressure
        double pressure = Double.NaN;
        if (pressureMode != SensorMode.DISABLED) {
            var1 = (tFine / 2.0) - 64000.0;
            var2 = var1 * var1 * digP6 / 32768.0;
            var2 = var2 + var1 * digP5 * 2.0;
            var2 = (var2 / 4.0) + (digP4 * 65536.0);
            var1 = (digP3 * var1 * var1 / 524288.0 + digP2 * var1) / 524288.0;
            var1 = (1.0 + var1 / 32768.0) * digP1;
            if (var1 == 0.0) {
                pressure = 0;   // // avoid exception caused by division by zero
            } else {
                pressure = 1048576.0 - adcP;
                pressure = (pressure - (var2 / 4096.0)) * 6250.0 / var1;
                var1 = digP9 * pressure * pressure / 2147483648.0;
                var2 = pressure * digP8 / 32768.0;
                pressure = pressure + (var1 + var2 + digP7) / 16.0;
           }
        }
        float adcH = Float.NaN;
        double humidity = Double.NaN;
        if (model == Model.BME280 && humidityMode != SensorMode.DISABLED) {
            // Humidity

            adcH = ((ioBuf[6] & 0xFF) << 8) | (ioBuf[7] & 0xFF);

            double varH = tFine - 76800.0;
            varH = (adcH - (digH4 * 64.0 + digH5 / 16384.0 *
                    varH)) * (digH2 / 65536.0 * (1.0 + digH6 /
                    67108864.0 * varH *
                    (1.0 + digH3 / 67108864.0 * varH)));
            varH = varH * (1.0 - digH1 * varH / 524288.0);

            if (varH > 100.0) {
                varH = 100.0;
            } else if (varH < 0.0) {
                varH = 0.0;
            }
            humidity = varH;
        }

        if (measurementMode == MeasurementMode.FORCED) {
            measurementMode = MeasurementMode.SLEEPING;
        }

        return new Measurement((float) temperature, (float) pressure, (float) humidity);
    }

    /**
     * Write the reset command to the BMP280.
     */
    public void reset() {
        materializeDelay();
        registerAccess.writeRegister(Bmp280Constants.RESET, Bmp280Constants.RESET_CMD);
        setDelayMs(100);
    }

    // Returns the sensor model (BME_280 or BMP_280)
    public Model getModel() {
        return model;
    }

    // Internal methods

    /**
     * Asserts that the device is in sleeping mode for settings changes. This is not strictly a device requirement
     * but all the settings only get actually updated via a mode change, so the transition from sleeping to any of the
     * measurement modes will make sure that the settings will be taken into account.
     */
    private void assertSleepingModeForSettings() {
        if (measurementMode != MeasurementMode.SLEEPING) {
            throw new IllegalStateException("Settings can only be changed while the device is SLEEPING mode.");
        }
    }

    /**
     * Sets a delay in ms that will be applied before the next instruction is sent to the chip, counting from now.
     * The corresponding instant can be queried via getBusyUntil().
     */
    private void setDelayMs(int delayMs) {
        Instant target = Instant.now().plusMillis(delayMs);
        if (target.isAfter(busyUntil)) {
            busyUntil = target;
        }
    }

    private void materializeDelay() {
        while (true) {
            long remaining = Instant.now().until(busyUntil, ChronoUnit.MILLIS);
            if (remaining < 0) {
                break;
            }
            try {
                Thread.sleep(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    private int readRegisterS16(int register) {
        registerAccess.readRegister(register, ioBuf, 0, 2);
        return (ioBuf[0] & 0xFF) | (ioBuf[1] << 8);
    }

    private int readRegisterU16(int register) {
        return readRegisterS16(register) & 0xFFFF;
    }

    @Override
    public void readMeasurement(double[] values) {
        Measurement measurement = readMeasurement();
        values[0] = measurement.temperature;
        values[1] = measurement.pressure;
        if (model == Model.BME280) {
            values[2] = measurement.humidity;
        }
    }

    // Nested types

    public static class Measurement {
        private final float temperature;
        private final float pressure;
        private final float humidity;

        Measurement(float temperature, float pressure, float humidity) {
            this.temperature = temperature;
            this.pressure = pressure;
            this.humidity = humidity;
        }

        public float getTemperature() {
            return temperature;
        }

        public float getHumidity() {
            return humidity;
        }

        public float getPressure() {
            return pressure;
        }

        @Override
        public String toString() {
            return "Temperature = " + temperature + " °C; Humidity = " + humidity + " %; Pressure = " + pressure + " Pa";
        }
    }

    /** The enabled and oversampling mode for each sensor (temperature, pressure and humidity). */
    public enum SensorMode {
        /**
         * Don't measure anything for this sensor. Caveat: Humidity and pressure corrections depend on the measured
         * temperature; disabling temperature measurement may lead to unexpected results. *
         */
        DISABLED,
        /** Perform measurements without any oversampling */
        ENABLED,
        OVERSAMPLE_2X,
        OVERSAMPLE_4X,
        OVERSAMPLE_8X,
        OVERSAMPLE_16X
    }

    public enum MeasurementMode {
        /**
         * The initial "default" mode. The chip will not perform any measurements in this mode.
         * Certain configuration commands will only work in this mode.
         */
        SLEEPING,
        /**
         * Continuous measuring mode. The chip will perform a measurement (taking getMeasurementTime() ms,
         * depending on the sampling settings), then sleep for the time set via setStandbyTime() in
         * a continuous loop.
         */
        CONTINUOUS,
        /**
         * Forces a single measurement and then goes back to SLEEPING. The measured values
         * will be available after getMeasurementTime().
         */
        FORCED
    }

    public enum Model {
        BME280, BMP280
    }
}
