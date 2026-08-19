package com.pi4j.drivers.multipurpose.piborg;

import com.pi4j.io.i2c.I2C;

import java.io.IOException;
import java.util.Objects;

/**
 * Driver for the <a href="https://www.piborg.org/ultraborg">PiBorg UltraBorg</a>, a Raspberry Pi add-on board that
 * reads up to four HC-SR04 style ultrasonic distance sensors and drives up to four hobby servos, all over I2C.
 * <p>
 * This is a Java port of PiBorg's original {@code UltraBorg.py} driver. The Python driver defines a near identical set
 * of methods for each of the four channels (e.g. {@code
 * GetDistance1}..{@code GetDistance4}); this port instead exposes a single method per operation that takes a
 * {@link Channel} argument, with the per-channel command codes derived arithmetically from the UltraBorg command table.
 * <p>
 * Basic usage:
 *
 * <pre>{@code
 * Context pi4j = Pi4J.newAutoContext();
 * I2CProvider i2CProvider = pi4j.provider("linuxfs-i2c");
 * I2CConfig config = I2C.newConfigBuilder(pi4j).id("UltraBorg").bus(1).device(UltraBorgDriver.DEFAULT_ADDRESS).build();
 *
 * try (I2C i2c = i2CProvider.create(config);
 *         UltraBorgDriver ultraBorg = new UltraBorgDriver(i2c)) {
 *
 *     double distanceMm = ultraBorg.getDistanceMm(UltraBorgDriver.Channel.CHANNEL_1);
 *
 *     ultraBorg.setServoPosition(UltraBorgDriver.Channel.CHANNEL_1, 0.5); // 50% to the right
 * }
 * }</pre>
 * <p>
 * Multiple boards can be used at the same time by giving each one a different I2C address (see
 * {@link #setI2cAddress(int)}) and creating a separate {@code I2C} device / driver instance for each address.
 * <p>
 * This class is not thread safe; callers should synchronize externally if a single instance is shared between threads.
 */
public class UltraBorgDriver implements AutoCloseable {

    /** Factory-default I2C address of the UltraBorg board. */
    public static final int DEFAULT_ADDRESS = 0x36;

    /** Lowest allowed I2C address, addresses below this are reserved. */
    public static final int MIN_I2C_ADDRESS = 0x03;

    /** Highest allowed I2C address, addresses above this are reserved. */
    public static final int MAX_I2C_ADDRESS = 0x77;

    /**
     * Sentinel PWM value which, when used with {@link #setServoStartup(Channel, int)}, requests "unset" (centred)
     * startup behaviour instead of a specific PWM level.
     */
    public static final int PWM_UNSET = 0xFFFF;

    /** Typical minimum servo PWM burst: approximately a 1 ms pulse, ~3% duty cycle. */
    public static final int PWM_TYPICAL_MIN = 2000;

    /** Typical maximum servo PWM burst: approximately a 2 ms pulse, ~6.1% duty cycle. */
    public static final int PWM_TYPICAL_MAX = 4000;

    // Board identifier returned in COMMAND_GET_ID replies.
    private static final int BOARD_ID = 0x36;

    // All replies from the board are exactly 4 bytes: [echoed command, data high, data low, 0].
    private static final int REPLY_LENGTH = 4;

    // Conversion factor from an ultrasonic echo time (microseconds) to a distance (millimeters).
    private static final double MICROSECONDS_TO_MM = 0.1715;

    // Time to wait after writing an EEPROM-backed value before it is safe to read back.
    private static final long EEPROM_WRITE_DELAY_MS = 10;

    private static final int DEFAULT_RETRY_COUNT = 5;
    private static final long RETRY_DELAY_MS = 100;

    // Delay between writing a command byte and reading the reply, giving the UltraBorg's
    // microcontroller time to prepare its response. The Python driver sleeps 1 microsecond here;
    // Thread.sleep can't reliably sleep sub-millisecond, so this rounds up to 1 ms, which is still
    // negligible next to the polling rates this device is normally used at.
    private static final long REPLY_DELAY_MS = 1;

    // Command table. Per-channel commands are derived as BASE + channel.index() * STRIDE.
    private static final int CMD_GET_RAW_TIME_USM_BASE = 1; // stride 1, channels 0..3 -> 1..4
    private static final int CMD_SET_PWM_BASE = 5; // stride 2
    private static final int CMD_GET_PWM_BASE = 6; // stride 2
    private static final int CMD_CALIBRATE_PWM_BASE = 13; // stride 1
    private static final int CMD_GET_PWM_MIN_BASE = 17; // stride 3
    private static final int CMD_GET_PWM_MAX_BASE = 18; // stride 3
    private static final int CMD_GET_PWM_BOOT_BASE = 19; // stride 3
    private static final int CMD_SET_PWM_MIN_BASE = 29; // stride 3
    private static final int CMD_SET_PWM_MAX_BASE = 30; // stride 3
    private static final int CMD_SET_PWM_BOOT_BASE = 31; // stride 3
    private static final int CMD_GET_FILTERED_TIME_USM_BASE = 41; // stride 1

    private static final int CMD_GET_ID = 0x99;
    private static final int CMD_SET_I2C_ADDRESS = 0xAA;

    /** Identifies one of the four ultrasonic / servo channels on the board. */
    public enum Channel {
        CHANNEL_1, CHANNEL_2, CHANNEL_3, CHANNEL_4;

        private int index() {
            return ordinal();
        }

        /**
         * Returns the {@link Channel} corresponding to the given zero-based index (i.e. {@code 0} for
         * {@link #CHANNEL_1} through {@code 3} for {@link #CHANNEL_4}).
         *
         * @throws IllegalArgumentException
         *             if {@code index} is not between 0 and 3 inclusive
         */
        public static Channel of(int index) {
            Channel[] channels = values();
            if (index < 0 || index >= channels.length) {
                throw new IllegalArgumentException(String.format(
                        "Channel index %d is out of range, must be between 0 and %d", index, channels.length - 1));
            }
            return channels[index];
        }
    }

    private final I2C i2c;

    // Cached calibration limits per channel, used to convert servo positions (-1..1) to/from
    // raw PWM levels. Populated from the board's EEPROM in the constructor and kept in sync
    // whenever setServoMinimum/setServoMaximum are called.
    private final int[] pwmMinimum = new int[Channel.values().length];
    private final int[] pwmMaximum = new int[Channel.values().length];

    /**
     * Wraps an already configured I2C device for an UltraBorg board.
     *
     * @param i2c
     *            an open I2C device, configured with the UltraBorg's address (see {@link #DEFAULT_ADDRESS})
     *
     * @throws IOException
     *             if the board cannot be reached, or the device found at the given address does not identify itself as
     *             an UltraBorg
     */
    public UltraBorgDriver(I2C i2c) throws IOException {
        this.i2c = Objects.requireNonNull(i2c, "i2c must not be null");

        int id = getBoardId();
        if (id != BOARD_ID) {
            throw new IOException(
                    String.format("Device did not identify itself as an UltraBorg (expected id 0x%02X, got 0x%02X). "
                            + "Check the wiring and the configured I2C address.", BOARD_ID, id));
        }

        for (Channel channel : Channel.values()) {
            pwmMinimum[channel.index()] = getServoMinimum(channel);
            pwmMaximum[channel.index()] = getServoMaximum(channel);
        }
    }

    /**
     * Checks whether an UltraBorg board answers on the given, already opened, I2C device. Useful for scanning an I2C
     * bus for UltraBorg boards before constructing a driver instance.
     *
     * @param i2c
     *            an open I2C device pointing at the address to probe
     *
     * @return {@code true} if the device identifies itself as an UltraBorg
     */
    public static boolean probe(I2C i2c) {
        i2c.write((byte) CMD_GET_ID);
        byte[] reply = new byte[REPLY_LENGTH];
        int read = i2c.read(reply, 0, REPLY_LENGTH);
        return read == REPLY_LENGTH && (reply[0] & 0xFF) == CMD_GET_ID && (reply[1] & 0xFF) == BOARD_ID;
    }

    /**
     * Returns the filtered distance measured by the ultrasonic module on the given channel, in millimeters. Filtering
     * makes the reading less jumpy than {@link #getRawDistanceMm(Channel)} at the cost of a slower response to changes.
     *
     * @return the distance in millimeters, or {@code 0} if no object is in range or no ultrasonic module is attached to
     *         this channel
     */
    public double getDistanceMm(Channel channel) throws IOException {
        return readDistanceMm(CMD_GET_FILTERED_TIME_USM_BASE + channel.index());
    }

    /**
     * Returns the raw (unfiltered) distance measured by the ultrasonic module on the given channel, in millimeters.
     * This reacts faster than {@link #getDistanceMm(Channel)} but is noisier.
     *
     * @return the distance in millimeters, or {@code 0} if no object is in range or no ultrasonic module is attached to
     *         this channel
     */
    public double getRawDistanceMm(Channel channel) throws IOException {
        return readDistanceMm(CMD_GET_RAW_TIME_USM_BASE + channel.index());
    }

    private double readDistanceMm(int command) throws IOException {
        byte[] reply = rawRead(command);
        int timeUs = unsigned16(reply);
        if (timeUs == 0xFFFF) {
            timeUs = 0;
        }
        return timeUs * MICROSECONDS_TO_MM;
    }

    /**
     * Returns the current drive position of the servo on the given channel, normalized to the configured
     * minimum/maximum PWM range.
     *
     * @return a value from -1 (fully at the configured minimum) to +1 (fully at the configured maximum), with 0 being
     *         central
     */
    public double getServoPosition(Channel channel) throws IOException {
        int idx = channel.index();
        int pwmDuty = getRawPwm(channel);
        double powerOut = (double) (pwmDuty - pwmMinimum[idx]) / (pwmMaximum[idx] - pwmMinimum[idx]);
        return (2.0 * powerOut) - 1.0;
    }

    /**
     * Returns the current raw PWM duty value for the servo output on the given channel, as read directly from the
     * board, without normalizing it against the configured minimum/maximum range. Unlike
     * {@link #getServoPosition(Channel)}, this is not scaled to -1..+1, so it is useful for UIs (e.g. a calibration
     * GUI) that want to display or compare the actual PWM level.
     */
    public int getRawPwm(Channel channel) throws IOException {
        return unsigned16(rawRead(CMD_GET_PWM_BASE + channel.index() * 2));
    }

    /**
     * Sets the drive position of the servo on the given channel, normalized to the configured minimum/maximum PWM
     * range. The resulting PWM level is checked against the configured range on the board.
     *
     * @param position
     *            -1 for the configured minimum, 0 for central, +1 for the configured maximum
     */
    public void setServoPosition(Channel channel, double position) throws IOException {
        int idx = channel.index();
        double powerOut = (position + 1.0) / 2.0;
        int pwmDuty = (int) Math.round(powerOut * (pwmMaximum[idx] - pwmMinimum[idx]) + pwmMinimum[idx]);
        writePwm16(CMD_SET_PWM_BASE + idx * 2, pwmDuty);
    }

    /**
     * Directly sets the PWM burst length for the servo output on the given channel, bypassing the configured
     * minimum/maximum limit checks. Intended for use while calibrating those limits; for normal operation use
     * {@link #setServoPosition(Channel, double)} instead.
     *
     * @param pwmLevel
     *            the PWM level where 2000 represents approximately a 1 ms burst and 4000 approximately a 2 ms burst
     */
    public void calibrateServoPwm(Channel channel, int pwmLevel) throws IOException {
        writePwm16(CMD_CALIBRATE_PWM_BASE + channel.index(), pwmLevel);
    }

    /**
     * Returns the minimum PWM level configured for the servo output on the given channel. This corresponds to a
     * {@link #getServoPosition(Channel)} of -1.
     */
    public int getServoMinimum(Channel channel) throws IOException {
        return unsigned16(rawRead(CMD_GET_PWM_MIN_BASE + channel.index() * 3));
    }

    /**
     * Returns the maximum PWM level configured for the servo output on the given channel. This corresponds to a
     * {@link #getServoPosition(Channel)} of +1.
     */
    public int getServoMaximum(Channel channel) throws IOException {
        return unsigned16(rawRead(CMD_GET_PWM_MAX_BASE + channel.index() * 3));
    }

    /** Returns the PWM level the servo output on the given channel will use at board startup. */
    public int getServoStartup(Channel channel) throws IOException {
        return unsigned16(rawRead(CMD_GET_PWM_BOOT_BASE + channel.index() * 3));
    }

    /**
     * Sets, and persists to the board's EEPROM, the minimum PWM level for the servo output on the given channel. We
     * recommend using PiBorg's tuning GUI to determine appropriate limits for a given servo before setting them here.
     */
    public void setServoMinimum(Channel channel, int pwmLevel) throws IOException {
        int idx = channel.index();
        writePwm16(CMD_SET_PWM_MIN_BASE + idx * 3, pwmLevel);
        sleep(EEPROM_WRITE_DELAY_MS);
        pwmMinimum[idx] = pwmLevel;
    }

    /**
     * Sets, and persists to the board's EEPROM, the maximum PWM level for the servo output on the given channel. We
     * recommend using PiBorg's tuning GUI to determine appropriate limits for a given servo before setting them here.
     */
    public void setServoMaximum(Channel channel, int pwmLevel) throws IOException {
        int idx = channel.index();
        writePwm16(CMD_SET_PWM_MAX_BASE + idx * 3, pwmLevel);
        sleep(EEPROM_WRITE_DELAY_MS);
        pwmMaximum[idx] = pwmLevel;
    }

    /**
     * Sets, and persists to the board's EEPROM, the startup PWM level for the servo output on the given channel, i.e.
     * the position the servo will move to as soon as the board powers up.
     *
     * @param pwmLevel
     *            a level within the channel's configured minimum/maximum range, or {@link #PWM_UNSET} to request
     *            centred startup behaviour
     *
     * @throws IllegalArgumentException
     *             if {@code pwmLevel} is outside the channel's configured minimum/maximum range and is not
     *             {@link #PWM_UNSET}
     */
    public void setServoStartup(Channel channel, int pwmLevel) throws IOException {
        int idx = channel.index();
        if (pwmLevel != PWM_UNSET && !isWithinConfiguredRange(idx, pwmLevel)) {
            throw new IllegalArgumentException(
                    String.format("Startup PWM level %d for %s is outside the configured limits of %d to %d", pwmLevel,
                            channel, pwmMinimum[idx], pwmMaximum[idx]));
        }
        writePwm16(CMD_SET_PWM_BOOT_BASE + idx * 3, pwmLevel);
        sleep(EEPROM_WRITE_DELAY_MS);
    }

    private boolean isWithinConfiguredRange(int idx, int pwmLevel) {
        int min = pwmMinimum[idx];
        int max = pwmMaximum[idx];
        return min <= max ? (pwmLevel >= min && pwmLevel <= max) : (pwmLevel <= min && pwmLevel >= max);
    }

    /** Returns the board identifier reported by the device, expected to be {@code 0x36}. */
    public int getBoardId() throws IOException {
        return rawRead(CMD_GET_ID)[1] & 0xFF;
    }

    /**
     * Changes the I2C address the board answers on and persists it, so it survives a power cycle.
     * <p>
     * After this call succeeds, this driver instance (and the underlying {@link I2C} device passed to the constructor,
     * which was opened against the old address) can no longer talk to the board. Callers must open a new {@link I2C}
     * device configured with {@code newAddress} and construct a new {@code UltraBorgDriver} to continue communicating
     * with the board.
     *
     * @param newAddress
     *            the new I2C address, between {@link #MIN_I2C_ADDRESS} and {@link #MAX_I2C_ADDRESS} inclusive
     */
    public void setI2cAddress(int newAddress) throws IOException {
        if (newAddress < MIN_I2C_ADDRESS || newAddress > MAX_I2C_ADDRESS) {
            throw new IllegalArgumentException(
                    String.format("I2C address 0x%02X is out of range, must be between 0x%02X and 0x%02X", newAddress,
                            MIN_I2C_ADDRESS, MAX_I2C_ADDRESS));
        }
        rawWrite(CMD_SET_I2C_ADDRESS, (byte) newAddress);
        sleep(RETRY_DELAY_MS);
    }

    /** Closes the underlying I2C device. */
    @Override
    public void close() throws IOException {
        i2c.close();
    }

    // -- Low level I2C helpers -----------------------------------------------------------------

    private static int unsigned16(byte[] reply) {
        return ((reply[1] & 0xFF) << 8) | (reply[2] & 0xFF);
    }

    private void writePwm16(int command, int pwmLevel) throws IOException {
        byte high = (byte) ((pwmLevel >> 8) & 0xFF);
        byte low = (byte) (pwmLevel & 0xFF);
        rawWrite(command, high, low);
    }

    /**
     * Writes {@code command} followed by {@code data} as a single I2C write transaction. Note this deliberately does
     * <em>not</em> use {@code I2C.writeRegister}: the UltraBorg firmware expects one plain write containing the command
     * byte followed by any data bytes, not a register-address-plus-data framing.
     */
    private void rawWrite(int command, byte... data) throws IOException {
        byte[] payload = new byte[data.length + 1];
        payload[0] = (byte) command;
        System.arraycopy(data, 0, payload, 1, data.length);
        int written = i2c.write(payload);
        if (written != payload.length) {
            throw new IOException("Short I2C write for UltraBorg command " + command);
        }
    }

    /**
     * Sends a "get" command and reads back the fixed-length reply, retrying a few times if the board does not answer,
     * or echoes back an unexpected command byte.
     * <p>
     * The UltraBorg firmware needs the command byte and the reply to travel as two entirely separate I2C transactions
     * (write, then a short delay, then a fresh read) rather than a single combined write-then-repeated-start-read
     * transaction: its microcontroller needs a moment to prepare the reply data after seeing the command, and does not
     * support clock stretching or a repeated START into the read. Using {@code I2C.readRegister(...)} here (a single
     * combined transaction) reliably fails against the real board, which is why this uses plain
     * {@code write}/{@code read} instead.
     */
    private byte[] rawRead(int command) throws IOException {
        IOException lastFailure = null;
        for (int attempt = 0; attempt < DEFAULT_RETRY_COUNT; attempt++) {
            i2c.write((byte) command);
            sleep(REPLY_DELAY_MS);
            byte[] reply = new byte[REPLY_LENGTH];
            int read = i2c.read(reply, 0, REPLY_LENGTH);
            if (read == REPLY_LENGTH && (reply[0] & 0xFF) == command) {
                return reply;
            }
            lastFailure = new IOException("Unexpected reply reading UltraBorg command " + command);
            sleep(RETRY_DELAY_MS);
        }
        i2c.close();
        throw new IOException(
                "I2C read for UltraBorg command " + command + " failed after " + DEFAULT_RETRY_COUNT + " attempts",
                lastFailure);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
