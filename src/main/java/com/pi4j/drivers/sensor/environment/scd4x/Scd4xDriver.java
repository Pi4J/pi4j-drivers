package com.pi4j.drivers.sensor.environment.scd4x;

import com.pi4j.io.i2c.I2C;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Pi4J-based driver for SCD4X CO₂ (+ temperature and humidity) sensors.
 * <p>
 * Product datasheet link: https://sensirion.com/media/documents/48C4B7FB/64C134E7/Sensirion_SCD4x_Datasheet.pdf
 */
public class Scd4xDriver implements Closeable {

    /**
     * The I2C address of the device (needed for constructing an I2C instance)
     */
    public static final int I2C_ADDRESS = 0x62;

    private final I2C i2c;
    private final ByteBuffer ioBuf = ByteBuffer.allocate(9);

    private Instant busyUntil = Instant.now();
    private Mode mode = Mode.IDLE;

    /**
     * Creates a driver instance, connected via the given I2C instance. Note that the i2c device value needs to be set
     * to I2C_ADDRESS when building the I2C instance.
     * <p>
     * In most use cases, should make sense to first bring the device into a well-defined state via
     * safeInit(). After this, the sensor can be configured as desired (e.g. by setting the altitude).
     * To initiate data collection, call startPeriodicMeasurement() or startLowPowerPeriodicMeasurement().
     * After this, measurements can be obtained via readMeasurement() as needed.
     */
    public Scd4xDriver(I2C i2c) {
        this.i2c = i2c;
        safeInit();
    }

    // Basic commands; Chapter 3.5

    /**
     * Starts periodic measurement at an interval of 5 seconds.
     */
    public void startPeriodicMeasurement() {
        sendConfigurationCommand(CommandCodes.START_PERIODIC_MEASUREMENT, 0);
        mode = Mode.PERIODIC_MEASUREMENT;
    }

    /**
     * Read a measurement. This command will implicitly wait until a measurement is available and throw an
     * exception if a measurement will not be available within the interval time implied by the measurement mode.
     * <p>
     * Reading the value will clear it internally, so the next read won't be available until the measurement
     * time implied by the measurement mode.
     */
    public Measurement readMeasurement() {
        materializeDelay();

        int expectedInterval = (mode == Mode.LOW_POWER_PERIODIC_MEASUREMENT ? 30_000 : 5_000);
        // Multiply with 1.5 to allow some tolerance.
        long timeOut = System.currentTimeMillis() + expectedInterval * 3 / 2;

        // getDataReadyStatus will check that we are in one of the measurement modes.
        while (!getDataReadyStatus()) {
            if (System.currentTimeMillis() > timeOut) {
                String message = "Unable to read measurement withing the expected time frame (" + expectedInterval + "ms) for " + mode + " mode";
                if (mode == Mode.SINGLE_SHOT_MEASUREMENT) {
                    mode = Mode.IDLE;
                }
                throw new RuntimeException(message);
            }
            try {
                Thread.sleep(expectedInterval / 32);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        sendCommand(CommandCodes.READ_MEASUREMENT, 1);
        i2c.read(ioBuf.array(), 3 * 3);
        // i2c.read(buf, 0, 3*3);
        int co2 = getWord(0);
        int raw_temperature = getWord(3);
        int raw_humidity = getWord(6);

        if (mode == Mode.SINGLE_SHOT_MEASUREMENT) {
            mode = Mode.IDLE;
        }

        return new Measurement(
                co2,
                -45 + 175.0f * raw_temperature / 65535.0f,
                100.0f * raw_humidity / 65535.0f);
    }

    /**
     * Stops periodic measurements to save power and goes back to IDLE mode.
     */
    public void stopPeriodicMeasurement() {
        sendCommand(CommandCodes.STOP_PERIODIC_MEASUREMENT, 500);
        mode = Mode.IDLE;
    }

    // On-chip output signal compensation; chapter 3.6

    /**
     * Set the temperature offset in °C.
     * <p>
     * Correctly setting the temperature offset is required for
     * accurate humidity and temperature readings. This offset doesn't affect the sensor's
     * CO₂ accuracy.
     * <p>
     * Several factors can influence the correct temperature offset, including:
     * <ul>
     * <li>The SCD4x's measurement mode
     * <li>Heat from nearby components
     * <li>Ambient temperature
     * <li>Airflow
     * </ul>
     * Because of this, the correct temperature offset should be determined
     * under typical operating conditions. This means the device should be in its normal
     * operating mode and have reached thermal equilibrium.
     * <p>
     * By default, the temperature offset is set to 4° C. To permanently save a new offset
     * value, the persistSetting command is required.
     */
    public void setTemperatureOffset(double offsetC) {
        sendConfigurationCommand(CommandCodes.SET_TEMPERATURE_OFFSET, 1, (int) (offsetC * 65536.0 / 175.0));
    }

    /**
     * Returns the current temperature offset in °C.
     */
    public double getTemperatureOffset() {
        sendConfigurationCommand(CommandCodes.GET_TEMPERATURE_OFFSET, 1);
        return readValue() * 175.0 / 65536.0;
    }

    /**
     * Set the sensor's altitude in meter above sea level.
     * <p>
     * The sensor's altitude must be read and written only while the SCD4x is in idle mode.
     * Typically, this setting is configured just once after the device has been installed.
     * To permanently save the altitude to the EEPROM, the persist setting command must be issued.
     * By default, the sensor altitude is set to 0 meters above sea level.
     * <p>
     * The input value will be capped to the valid range from 0 to 3000m.
     */
    public void setSensorAltitude(int altitudeMasl) {
        sendConfigurationCommand(CommandCodes.SET_SENSOR_ALTITUDE, 1, Math.max(0, Math.min(altitudeMasl, 3000)));
    }

    /**
     * Returns the sensor altitude as set in m.
     */
    public int getSensorAltitude() {
        sendConfigurationCommand(CommandCodes.GET_SENSOR_ALTITUDE, 1);
        return readValue();
    }

    /**
     * Sets the ambient pressure in pascal. The default value is 101'300 Pa. Values will be capped
     * to the valid range from 70'000 pascal to 120'000 pascal. In contrast to other configuration data,
     * this value can be written and read when the device is in a measurement mode.
     */
    public void setAmbientPressure(int pressurePa) {
        sendCommand(CommandCodes.SET_AMBIENT_PRESSURE, 1, Math.max(70_000, Math.min(pressurePa, 120_000)) / 100);
    }

    // Chapter 3.7: Field Calibration

    /**
     * Please refer to chapter 3.7.1 of the Sensirion specification for the steps necessary to perform a
     * successful forced recalibration with a given reference value.
     */
    public int performForcedRecalibration(int referenceCo2ppm) {
        sendConfigurationCommand(CommandCodes.PERFORM_FORCED_RECALIBRATION, 400, referenceCo2ppm);
        int result = readValue();
        if (result == 0xffff) {
            throw new IllegalStateException("Recalibration has failed.");
        }
        return result - 0x8000;
    }

    /**
     * Enables or disables automatic self calibration. Enabled self calibration is the default.
     */
    public void setAutomaticSelfCalibrationEnabled(boolean enabled) {
        sendConfigurationCommand(CommandCodes.SET_AUTOMATIC_SELF_CALIBRATION_ENABLED, 1, enabled ? 1 : 0);
    }

    /**
     * Returns whether automatic self calibration is currently enabled.
     */
    public boolean getAutomaticSelfCalibrationEnabled() {
        sendConfigurationCommand(CommandCodes.GET_AUTOMATIC_SELF_CALIBRATION_ENABLED, 1);
        return readValue() != 0;
    }

    // Chapter 3.8: Low Power Operation

    /**
     * Enables periodic measurement approximately every 30 seconds (opposed to every 5 seconds via
     * startPeriodicMeasurement())
     */
    public void startLowPowerPeriodicMeasurement() {
        sendConfigurationCommand(CommandCodes.START_LOW_POWER_PERIODIC_MEASUREMENT, 0);
        mode = Mode.LOW_POWER_PERIODIC_MEASUREMENT;
    }

    /**
     * Returns true if a measurement is available; false otherwise.
     */
    public boolean getDataReadyStatus() {
        if (mode == Mode.IDLE || mode == Mode.SLEEP) {
            throw new IllegalStateException("Measurements can't be performed in " + mode + " mode.");
        }

        sendCommand(CommandCodes.GET_DATA_READY_STATUS, 1);
        int readyState = readValue();
        return (readyState & 0b011111111111) != 0;
    }

    // Chapter 3.9: Advanced Features

    /**
     * Saves changed calibration settings to EEPROM. Without performing this action, calibration data will be
     * lost during a power cycle.
     */
    public void persistSettings() {
        sendConfigurationCommand(0x3615, 800);
    }

    /**
     * Returns the 48 bit serial number of the device as a long value.
     */
    public long getSerialNumber() {
        sendConfigurationCommand(CommandCodes.GET_SERIAL_NUMBER, 1);
        materializeDelay();
        i2c.read(ioBuf.array(), 0, 3 * 3);
        return (((long) getWord(0)) << 32)
                | ((long) getWord(3) << 16)
                | getWord(6);
    }

    /**
     * Returns a value other than 0 if an issue was detected.
     * Note that this command takes a very long time (10s)
     */
    public int performSelfTest() {
        sendConfigurationCommand(CommandCodes.PERFORM_SELF_TEST, 10000);
        return readValue();
    }

    /**
     * Resets all configuration settings stored in the EEPROM and erases the FRC and ASC algorithm history.
     */
    public void performFactoryReset() {
        sendConfigurationCommand(CommandCodes.PERFORM_FACTORY_RESET, 1200);
    }

    /**
     * Resets all volatile configuration settings (similar to a power cycle).
     */
    public void reInit() {
        sendConfigurationCommand(CommandCodes.RE_INIT, 30);
    }

    // Chapter 3.10: Low power single shot (SCD41)

    /**
     * Requests a single shot measurement; <strong>only available for the SCD41 sensor.</strong>
     * For details, please refer to section 3.10 of the device specification.
     */
    public void measureSingleShot() {
        sendCommand(CommandCodes.MEASURE_SINGLE_SHOT, 5000);
        mode = Mode.SINGLE_SHOT_MEASUREMENT;
    }

    /**
     * Requests a single shot measurement limited to humidity and temperature;
     * 0 will be returned for the co2 value.
     * <p>
     * <strong>NOTE</strong>: This command is only available for the SCD41 sensor.
     */
    public void measureSingleShotRhtOnly() {
        sendCommand(CommandCodes.MEASURE_SINGLE_SHOT_RHT_ONLY, 50);
        mode = Mode.SINGLE_SHOT_MEASUREMENT;
    }

    /**
     * Put the sensor from IDLE to SLEEP mode in order ot save power.
     */
    public void powerDown() {
        sendConfigurationCommand(CommandCodes.POWER_DOWN, 1);
        mode = Mode.SLEEP;
    }

    /**
     * Wake the device up from SLEEP mode and put it back into IDLE mode.
     */
    public void wakeUp() {
        sendCommand(CommandCodes.WAKE_UP, 30);
        mode = Mode.IDLE;
    }

    /**
     * Sets the initial period for automatic self correction (ASC). The default is 44 hours.
     * Allowed values are integer multiples of four hours. Please refer to section 3.10.5 of the sensor spec
     * for details.
     */
    public void setAutomaticSelfCalibrationInitialPeriod(int initialPeriodHours) {
        sendConfigurationCommand(CommandCodes.SET_AUTOMATIC_SELF_CALIBRATION_INITIAL_PERIOD, 1, Math.max(0, Math.min(initialPeriodHours, 0xffff)) & 0xfffc);
    }

    /**
     * Returns the current initial ASC period in hours.
     */
    public int getAutomaticSelfCalibrationInitialPeriod() {
        sendConfigurationCommand(CommandCodes.GET_AUTOMATIC_SELF_CALIBRATION_INITIAL_PERIOD, 1);
        return readValue();
    }

    /**
     * Sets the standard period for automatic self correction (ASC). The default is 156 hours.
     * Allowed values are integer multiples of four hours. Please refer to section 3.10.7 of the sensor specification
     * for details.
     */
    public void setAutomaticSelfCalibrationStandardPeriod(int standardPeriodHours) {
        sendConfigurationCommand(CommandCodes.SET_AUTOMATIC_SELF_CALIBRATION_STANDARD_PERIOD, 1, Math.max(0, Math.min(standardPeriodHours, 0xffff)) & 0xfffc);
    }

    /**
     * Returns the current ASC standard period in hours.
     */
    public int getAutomaticSelfCalibrationStandardPeriod() {
        sendConfigurationCommand(CommandCodes.GET_AUTOMATIC_SELF_CALIBRATION_STANDARD_PERIOD, 1);
        return readValue();
    }

    // Additional methods provided by the driver.

    /**
     * Returns the current mode of the device as implied by mode changing methods (this method uses internal
     * state and does not query the device.
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Returns the time when the device will have fully processed the last issued
     * command and is ready to process commands again. This is limited to configuration / state command processing and
     * does not denote when a measurement will be available
     */
    public Instant getBusyUntil() {
        return busyUntil;
    }

    /**
     * Safely brings the sensor to an IDLE state regardless of the current state and resets all volatile values.
     */
    public void safeInit() {

        try {
            stopPeriodicMeasurement();
            getSerialNumber();
        } catch (Exception e) {
            wakeUp();
        }

        reInit();
    }

    // Internal helpers

    private int getWord(int byteIndex) {
        int word = ioBuf.getShort(byteIndex) & 0xffff;
        byte calculatedCrc = crc(ioBuf, byteIndex, 2);
        byte receivedCrc = ioBuf.get(byteIndex + 2);
        if (calculatedCrc != receivedCrc) {
            throw new IllegalStateException("Calculated crc: " + Integer.toHexString(calculatedCrc & 0xff) + " for: " + Integer.toHexString(word)
                    + " does not match the reived crc: " + Integer.toHexString(receivedCrc & 0xff));
        }
        return word;
    }

    private static byte crc(ByteBuffer data, int offset, int count) {
        byte crc = (byte) 0xff;
        for (int index = offset; index < offset + count; index++) {
            crc ^= data.get(index);
            for (int crcBit = 8; crcBit > 0; --crcBit) {
                if ((crc & 0x80) != 0) {
                    crc = (byte) ((crc << 1) ^ 0x31);
                } else {
                    crc <<= 1;
                }
            }
        }
        return crc;
    }

    /**
     * Checks that the mode is IDLE and then calls sendCommand.
     */
    private void sendConfigurationCommand(int cmdCode, int timeMs, int... args) {
        if (mode != Mode.IDLE) {
            throw new IllegalStateException("Command 0x" + Integer.toHexString(cmdCode) + " can only be issued in IDLE mode.");
        }
        sendCommand(cmdCode, timeMs, args);
    }

    /**
     * Sends the given command to the chip after materializing the delay implied
     * by the previous command and keeps track of the delay implied by this command
     * (from the timeMs parameter) in busyUntil.
     */
    private void sendCommand(int cmdCode, int timeMs, int... args) {
        materializeDelay();

        ioBuf.putShort((short) cmdCode);

        for (int i = 0; i < args.length; i++) {
            int p0 = ioBuf.position();
            ioBuf.putShort((short) args[i]);
            ioBuf.put(crc(ioBuf, p0, 2));
        }

        // .array() as Pi4j does sketchy stuff when handing in byte buffers directly
        i2c.write(ioBuf.array(), ioBuf.position());
        ioBuf.clear();

        setDelayMs(1);
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

    private int readValue() {
        materializeDelay();
        // .array() as Pi4j does sketchy stuff when handing in byte buffers directly
        i2c.read(ioBuf.array(), 3);
        return getWord(0);
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

    @Override
    public void close() {
        i2c.close();
    }

    public enum Mode {
        /**
         * After construction, we don't know the mode -- it might be different from IDLE from previous usage
         */
        IDLE,
        PERIODIC_MEASUREMENT,
        LOW_POWER_PERIODIC_MEASUREMENT,
        SINGLE_SHOT_MEASUREMENT,
        SLEEP,
    }

    /**
     * A measurement record containing the measured values returned form readMeasurement()
     */
    public static class Measurement {
        private final int co2;
        private final float temperature;
        private final float humidity;

        public Measurement(int co2, float temperature, float humidity) {
            this.co2 = co2;
            this.temperature = temperature;
            this.humidity = humidity;
        }

        @Override
        public String toString() {
            return "co2 = " + co2 + " ppm; temperature = " + temperature + " °C; humidity = " + humidity + " %";
        }

        /**
         * Measured co2 concentration in ppm
         */
        public int getCo2() {
            return co2;
        }

        /**
         * Measured temperature in C
         */
        public float getTemperature() {
            return temperature;
        }

        /**
         * Measured relative humidity in %
         */
        public float getHumidity() {
            return humidity;
        }
    }
}
