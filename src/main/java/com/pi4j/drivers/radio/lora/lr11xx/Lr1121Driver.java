package com.pi4j.drivers.radio.lora.lr11xx;

import java.io.Closeable;
import java.time.Duration;
import java.util.Optional;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiChipSelect;
import com.pi4j.io.spi.SpiConfigBuilder;
import com.pi4j.io.spi.SpiMode;

/**
 * A Semtech LR1121 (or LR1110 / LR1120) driven over SPI.
 *
 * <p>Everything the chip understands is built here, in Java, on top of a
 * {@link Lr11xxIo} that does nothing but move bytes. That split is what
 * makes this testable: a fake transport records what was written and plays back
 * what the radio would answer, so every command in this class is checked against
 * the bytes Semtech's own C driver sends — without a radio, a Raspberry Pi or a
 * soldering iron.
 *
 * <p>The opcodes and encodings were transcribed from that driver, which Waveshare
 * ships with the Core1121 demo, rather than read off a datasheet. Where the two
 * would disagree, the driver is what the hardware has actually been tested with.
 *
 * <h2>How the chip is spoken to</h2>
 *
 * Every command is a two byte big-endian opcode followed by its arguments. A
 * command that only sets something is one SPI transaction. A command that answers
 * is two: the command goes out, the busy line drops, and then the answer is
 * clocked in — <b>with a status byte in front of it</b> that is not part of the
 * response. Forgetting that byte shifts every field by one and produces values
 * that look almost right, which is why it is handled in one place here.
 *
 * @see <a href="https://github.com/Lora-net/SWDR001">SWDR001, the C driver</a>
 */
public class Lr1121Driver implements Closeable {

    // System
    private static final int GET_STATUS = 0x0100;
    private static final int GET_VERSION = 0x0101;
    private static final int GET_ERRORS = 0x010D;
    private static final int CLEAR_ERRORS = 0x010E;
    private static final int CALIBRATE = 0x010F;
    private static final int CALIBRATE_IMAGE = 0x0111;
    private static final int SET_DIO_AS_RF_SWITCH = 0x0112;
    private static final int SET_DIO_IRQ_PARAMS = 0x0113;
    private static final int CLEAR_IRQ = 0x0114;
    private static final int CFG_LFCLK = 0x0116;
    private static final int SET_TCXO_MODE = 0x0117;
    private static final int SET_STANDBY = 0x011C;

    // Memory
    private static final int WRITE_BUFFER8 = 0x0109;
    private static final int READ_BUFFER8 = 0x010A;

    // Radio
    private static final int GET_RX_BUFFER_STATUS = 0x0203;
    private static final int GET_PACKET_STATUS = 0x0204;
    private static final int SET_RX = 0x0209;
    private static final int SET_TX = 0x020A;
    private static final int SET_RF_FREQUENCY = 0x020B;
    private static final int SET_PACKET_TYPE = 0x020E;
    private static final int SET_MODULATION_PARAMS = 0x020F;
    private static final int SET_PACKET_PARAMS = 0x0210;
    private static final int SET_TX_PARAMS = 0x0211;
    private static final int SET_PA_CONFIG = 0x0215;
    private static final int SET_LORA_SYNC_WORD = 0x022B;

    private static final int PACKET_TYPE_LORA = 0x02;
    private static final int LFCLK_XTAL = 0x01;
    private static final int STANDBY_RC = 0x00;

    /** Every calibration block: oscillators, PLL, ADC and image. */
    private static final int CALIBRATE_ALL = 0x3F;

    public static final int IRQ_TX_DONE = 1 << 2;
    public static final int IRQ_RX_DONE = 1 << 3;
    public static final int IRQ_HEADER_ERROR = 1 << 6;
    public static final int IRQ_CRC_ERROR = 1 << 7;
    public static final int IRQ_TIMEOUT = 1 << 10;
    public static final int IRQ_COMMAND_ERROR = 1 << 22;

    /**
     * The radio counts time in ticks of its 32.768 kHz clock, so a timeout in
     * milliseconds is scaled by this. 1 ms is 32.768 ticks.
     */
    private static final double TICKS_PER_MS = 32.768;

    /** How long a command may leave the radio busy before something is wrong. */
    private static final Duration READY_TIMEOUT = Duration.ofSeconds(1);

    private final Lr11xxIo io;

    /** Remembered from configure(), because the amplifier belongs to the board. */
    private BoardConfig board = BoardConfig.core1121();

    /**
     * 2 MHz, which is what this driver asks for unless told otherwise.
     *
     * <p>The vendor's demo uses 10 MHz. That is fine down a short ribbon to a HAT
     * and marginal over jumper wires with no ground return beside each signal — and
     * marginal SPI does not fail cleanly. It corrupts the odd byte into a plausible
     * number, such as a version that reads 0x14 instead of 0x22. Two megahertz
     * carries this radio's packets with time to spare.
     */
    public static final int DEFAULT_BAUD = 2_000_000;

    /**
     * The ordinary way in: name the wiring, get a radio.
     *
     * <p>This asks for the things only the person who wired the board can know, and
     * sets everything that is the chip's business rather than theirs. All of it is
     * required, because a constructor that cannot be called without the pin numbers
     * is a better guarantee than any amount of checking afterwards.
     *
     * <pre>{@code
     * try (Lr1121Driver radio = new Lr1121Driver(pi4j, 0, SpiChipSelect.CS_0, 22, 24, 23)) {
     *     ...
     * }
     * }</pre>
     *
     * <p>What it sets for you, and why none of it belongs to the caller:
     *
     * <ul>
     * <li><b>SPI mode 0.</b> The chip clocks on the rising edge with the clock
     * idling low, and speaks no other mode.</li>
     * <li><b>Reset high from the moment the line is claimed</b>, and left high on
     * shutdown. Reset is active low, so an output that comes up at zero holds the
     * radio in reset — after which it answers nothing, its busy line never falls,
     * and every symptom points at the wiring.</li>
     * <li><b>No debounce on either input.</b> Pi4J defaults inputs to 10 000 µs and
     * passes that to the kernel, which then reports no edge at all for a shorter
     * pulse. This radio raises its interrupt line only until the driver clears the
     * interrupt — three short reads — so the default costs every reception,
     * silently, and looks exactly like a missing antenna.</li>
     * <li><b>No pull resistor.</b> Both lines are driven by the chip.</li>
     * </ul>
     *
     * @param pi4j the caller's context. The radio creates its bus and lines from it
     *        and closes them again on {@link #close()}, and does not touch the
     *        context itself
     * @param spiBus the SPI bus, 0 on a Raspberry Pi's header
     * @param chipSelect the controller's own chip select, which the radio's NSS pin
     *        goes to — {@code CS_0} is CE0. The kernel drives it around each
     *        transfer, so it is not one of the pins below
     * @param resetPin the radio's NRST pin, as a BCM number
     * @param busyPin the radio's BUSY pin, as a BCM number
     * @param interruptPin the radio's DIO9 pin, as a BCM number
     */
    public Lr1121Driver(Context pi4j, int spiBus, SpiChipSelect chipSelect,
                        int resetPin, int busyPin, int interruptPin) {
        this(pi4j, spiBus, chipSelect, resetPin, busyPin, interruptPin, DEFAULT_BAUD);
    }

    /**
     * The same, with a bus speed of your own.
     *
     * @param baud hertz. See {@link #DEFAULT_BAUD} for why faster is opt-in
     */
    public Lr1121Driver(Context pi4j, int spiBus, SpiChipSelect chipSelect,
                        int resetPin, int busyPin, int interruptPin, int baud) {
        this(new Pi4jLr11xxIo(
                pi4j.create(SpiConfigBuilder.newInstance(pi4j)
                        .id("lr11xx-spi")
                        .bus(spiBus)
                        .chipSelect(chipSelect)
                        .mode(SpiMode.MODE_0)
                        .baud(baud)
                        .build()),
                pi4j.create(DigitalOutputConfigBuilder.newInstance(pi4j)
                        .id("lr11xx-reset")
                        .bcm(resetPin)
                        .initial(DigitalState.HIGH)
                        .shutdown(DigitalState.HIGH)
                        .build()),
                input(pi4j, "lr11xx-busy", busyPin),
                input(pi4j, "lr11xx-irq", interruptPin),
                true));
    }

    private static DigitalInput input(Context pi4j, String id, int bcm) {
        return pi4j.create(DigitalInputConfigBuilder.newInstance(pi4j)
                .id(id)
                .bcm(bcm)
                .pull(PullResistance.OFF)
                .debounce(0L)
                .build());
    }

    /**
     * The ordinary way in: the SPI bus the radio is on, and the three lines that
     * are not part of it.
     *
     * <p>Chip select is <b>not</b> among them. The radio's NSS pin goes to the SPI
     * controller's own chip select — CE0 on a Raspberry Pi — and the kernel drives
     * it around each transfer.
     *
     * <p>The reset line must be created high. It is active low, so an output that
     * comes up at zero holds the radio in reset, after which it answers nothing and
     * every symptom points at the wiring.
     *
     * <p>The two inputs <b>must</b> be created with {@code .debounce(0L)}, and this
     * constructor refuses them otherwise. Pi4J debounces by ten milliseconds unless
     * told not to, and passes that to the kernel — which then reports no edge at all
     * for a pulse shorter than the window. This radio's pulses are far shorter, so
     * the default costs every reception and looks exactly like a missing antenna.
     *
     * <pre>{@code
     * Spi spi = pi4j.create(SpiConfigBuilder.newInstance(pi4j)
     *         .bus(0).chipSelect(SpiChipSelect.CS_0).mode(SpiMode.MODE_0).baud(2_000_000));
     * DigitalOutput reset = pi4j.create(DigitalOutputConfigBuilder.newInstance(pi4j)
     *         .bcm(22).initial(DigitalState.HIGH).shutdown(DigitalState.HIGH));
     * DigitalInput busy = pi4j.create(DigitalInputConfigBuilder.newInstance(pi4j)
     *         .bcm(24).debounce(0L));
     * DigitalInput irq = pi4j.create(DigitalInputConfigBuilder.newInstance(pi4j)
     *         .bcm(23).debounce(0L));
     *
     * try (Lr1121Driver radio = new Lr1121Driver(spi, reset, busy, irq)) {
     *     ...
     * }
     * }</pre>
     *
     * @param spi the bus, in mode 0. 10 MHz is what the vendor's demo uses; 2 MHz
     *        is a better choice over jumper wires, where a marginal bus corrupts
     *        the odd byte into a plausible-looking value rather than failing
     * @param reset the radio's NRST pin, active low
     * @param busy the radio's BUSY pin
     * @param interrupt the radio's DIO9 pin
     */
    public Lr1121Driver(Spi spi, DigitalOutput reset, DigitalInput busy, DigitalInput interrupt) {
        this(new Pi4jLr11xxIo(spi, reset, busy, interrupt));
    }

    /**
     * For a radio reached some other way — a USB adapter, or a recording in a
     * test. {@link Lr11xxIo} is the whole of what this driver needs from the wires.
     */
    public Lr1121Driver(Lr11xxIo io) {
        this.io = io;
    }

    /**
     * The chip's own answer to "what are you", which is the first thing to ask:
     * a wiring mistake produces zeros or 0xFF here rather than an error anywhere
     * else.
     *
     * @return hardware, use case type and firmware version
     */
    public Version version() {
        byte[] answer = query(GET_VERSION, 4);
        return new Version(answer[0] & 0xFF, answer[1] & 0xFF,
                ((answer[2] & 0xFF) << 8) | (answer[3] & 0xFF));
    }

    /** Hardware revision, device type, and firmware version. */
    public record Version(int hardware, int useCase, int firmware) {

        /** What an LR1121 calls itself. An LR1110 is 0x01, an LR1120 is 0x02. */
        public static final int LR1121 = 0x03;

        /**
         * What the chip calls itself while the bootloader is running, before it
         * has handed over to the application firmware.
         */
        public static final int BOOTLOADER = 0xDF;

        public boolean isBootloader() {
            return useCase == BOOTLOADER;
        }

        @Override
        public String toString() {
            String what = switch (useCase) {
                case LR1121 -> "LR1121";
                case BOOTLOADER -> "bootloader";
                default -> "unknown device";
            };
            return "hardware 0x%02X, type 0x%02X (%s), firmware 0x%04X"
                    .formatted(hardware, useCase, what, firmware);
        }
    }

    /**
     * Brings the radio up: reset, the board's own oscillator and antenna switch,
     * then a calibration.
     *
     * <p>The order matters and is not obvious. The oscillator has to be configured
     * before calibrating, because the calibration measures against it — calibrate
     * first and the radio is trimmed for a clock it is not going to use.
     */
    public void configure(BoardConfig board) {
        this.board = board;
        awaitApplicationFirmware();
        standby();

        if (board.hasTcxo()) {
            command(SET_TCXO_MODE,
                    board.tcxoVoltage(),
                    (board.tcxoStartupTicks() >> 16) & 0xFF,
                    (board.tcxoStartupTicks() >> 8) & 0xFF,
                    board.tcxoStartupTicks() & 0xFF);
        }

        command(SET_DIO_AS_RF_SWITCH,
                board.rfSwitchEnable(), board.rfSwitchStandby(), board.rfSwitchRx(),
                board.rfSwitchTx(), board.rfSwitchTxHp(), board.rfSwitchTxHf(),
                0 /* gnss */, 0 /* wifi */);

        // The low frequency clock, with the "wait until the 32 kHz is ready" bit.
        command(CFG_LFCLK, LFCLK_XTAL | (1 << 2));

        clearErrors();
        command(CALIBRATE, CALIBRATE_ALL);
    }

    /**
     * The modulation and the channel. Both ends of a link must call this with the
     * same values; there is nothing in LoRa that would notice if they did not.
     */
    public void configureLora(long frequencyHz, LoraSettings settings) {
        command(SET_PACKET_TYPE, PACKET_TYPE_LORA);
        command(SET_RF_FREQUENCY,
                (int) ((frequencyHz >> 24) & 0xFF), (int) ((frequencyHz >> 16) & 0xFF),
                (int) ((frequencyHz >> 8) & 0xFF), (int) (frequencyHz & 0xFF));
        calibrateImage(frequencyHz);
        command(SET_MODULATION_PARAMS,
                settings.spreadingFactor(), settings.bandwidth(), settings.codingRate(),
                settings.lowDataRateOptimisation() ? 1 : 0);
        command(SET_LORA_SYNC_WORD, settings.syncWord());
    }

    /**
     * Waits for a packet.
     *
     * @param settings the same settings the sender used
     * @param timeout how long to listen
     * @return the packet, or empty if nothing arrived in time or what arrived was
     *         corrupt. A CRC error is reported as nothing rather than as bytes,
     *         which is the only safe reading of a packet the radio knows is wrong
     */
    public Optional<ReceivedPacket> receive(LoraSettings settings, Duration timeout) {
        // 255: the largest a LoRa packet can be, since we do not know yet.
        packetParams(settings, 255);
        command(SET_DIO_IRQ_PARAMS, irqMask(IRQ_RX_DONE | IRQ_CRC_ERROR | IRQ_HEADER_ERROR | IRQ_TIMEOUT));
        clearIrq(0xFFFFFFFF);

        /*
           Zero means "listen until told otherwise" rather than "do not listen".
           The waiting is done here, so that the caller can interrupt a receiver
           that would otherwise sit in the radio's own timeout.
        */
        command(SET_RX, 0, 0, 0);

        int irq = awaitReception(timeout);
        clearIrq(irq);

        if (irq == 0) {
            standby();
            return Optional.empty();
        }

        if ((irq & IRQ_RX_DONE) == 0 || (irq & (IRQ_CRC_ERROR | IRQ_HEADER_ERROR)) != 0) {
            standby();
            return Optional.empty();
        }

        byte[] status = query(GET_RX_BUFFER_STATUS, 2);
        int length = status[0] & 0xFF;
        int start = status[1] & 0xFF;

        byte[] payload = query(READ_BUFFER8, length, start, length);

        byte[] packetStatus = query(GET_PACKET_STATUS, 3);
        int rssi = -(packetStatus[0] & 0xFF) / 2;
        double snr = ((byte) packetStatus[1]) / 4.0;

        standby();
        return Optional.of(new ReceivedPacket(payload, rssi, snr));
    }

    /**
     * Sends a packet and waits for the radio to say it has gone.
     *
     * @param powerDbm the output power. The high power path reaches 22 dBm; what
     *        is legal depends on where you are, and in EU868 it is 14 dBm ERP on
     *        most channels
     */
    public void transmit(byte[] payload, LoraSettings settings, int powerDbm, Duration timeout) {
        if (payload.length > 255) {
            throw new IllegalArgumentException(
                    "A LoRa packet holds 255 bytes, was given " + payload.length);
        }

        writeBuffer(payload);
        packetParams(settings, payload.length);
        transmitPower(powerDbm);
        command(SET_DIO_IRQ_PARAMS, irqMask(IRQ_TX_DONE | IRQ_TIMEOUT));
        clearIrq(0xFFFFFFFF);

        command(SET_TX, 0, 0, 0);

        if (!io.awaitInterrupt(timeout)) {
            standby();
            throw new IllegalStateException("The radio did not report the packet as sent within " + timeout);
        }

        int irq = irqStatus();
        clearIrq(irq);
        standby();

        if ((irq & IRQ_TX_DONE) == 0) {
            throw new IllegalStateException("Transmission ended without a done interrupt, IRQ 0x%08X"
                    .formatted(irq));
        }
    }

    /**
     * Resets the radio and waits for it to hand over from its bootloader.
     *
     * <p>An LR11xx starts in a bootloader that checks the flash and then jumps to
     * the application firmware. Asked for its version during that window it
     * answers honestly — as the bootloader, device 0xDF — and a bootloader
     * accepts firmware updates and nothing else. Every ordinary command then
     * fails, which looks like a broken radio rather than an impatient host.
     *
     * <p>So the reset is followed by asking until the answer changes, and by
     * resetting again if it does not. A module whose firmware really is missing
     * never changes its answer, and says so.
     */
    private void awaitApplicationFirmware() {
        for (int attempt = 0; attempt < BOOT_ATTEMPTS; attempt++) {
            io.reset();

            long deadline = System.nanoTime() + BOOT_TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                if (!version().isBootloader()) {
                    return;
                }
                sleep(BOOT_POLL);
            }
        }
        throw new IllegalStateException(
                "The radio is still in its bootloader after " + BOOT_ATTEMPTS
                + " resets, so it has no working firmware to hand over to."
                + " It will accept a firmware update and nothing else.");
    }

    private static final int BOOT_ATTEMPTS = 3;
    private static final Duration BOOT_TIMEOUT = Duration.ofMillis(500);
    private static final Duration BOOT_POLL = Duration.ofMillis(20);

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the radio to boot", e);
        }
    }

    /**
     * Calibrates the image rejection for the band actually in use.
     *
     * <p>This is the one calibration that cannot be done in advance, because it
     * is about a band rather than about the chip: {@code Calibrate} trims the
     * image filter for wherever the radio was last pointed, which before a
     * frequency is set is nowhere useful. The cost of skipping it is not an error
     * at the time — it is a receiver that works and hears less than it should.
     *
     * <p>The band edges are given in units of 4 MHz, rounded outwards, which for
     * 868 MHz gives the 0xD7 and 0xDB that the vendor's own driver uses for
     * 863–870.
     */
    public void calibrateImage(long frequencyHz) {
        double megahertz = frequencyHz / 1_000_000.0;
        int lower = (int) Math.floor((megahertz - BAND_MARGIN_MHZ) / 4.0);
        int upper = (int) Math.ceil((megahertz + BAND_MARGIN_MHZ) / 4.0);
        command(CALIBRATE_IMAGE, lower & 0xFF, upper & 0xFF);
    }

    /**
     * How far either side of the carrier the image calibration covers. Wide
     * enough that one calibration serves a whole ISM band rather than a single
     * channel, which is what the vendor's own values do.
     */
    private static final double BAND_MARGIN_MHZ = 8.0;

    /**
     * Waits for the radio to finish a reception, and returns the flags saying how
     * it went — or zero if the time ran out.
     *
     * <p>The interrupt line is used as a hint, not as the truth. The flags in the
     * chip are what actually says a packet arrived, and asking for them costs one
     * short SPI read; depending on a single wire to learn something the radio
     * will tell you anyway is a way to have a receiver that is silent for a
     * reason nothing reports.
     *
     * <p>So this waits on the line in short slices and reads the flags after each
     * one. A working interrupt makes it return promptly; a disconnected one costs
     * a fraction of a second and nothing else.
     */
    private int awaitReception(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        int interesting = IRQ_RX_DONE | IRQ_CRC_ERROR | IRQ_HEADER_ERROR | IRQ_TIMEOUT;

        while (System.nanoTime() < deadline) {
            io.awaitInterrupt(POLL_SLICE);

            int irq = irqStatus();
            if ((irq & interesting) != 0) {
                return irq;
            }
        }
        return 0;
    }

    /**
     * How long each wait on the interrupt line lasts before the flags are read
     * anyway. Short enough that a dead line costs little, long enough that a live
     * one does the waiting.
     */
    private static final Duration POLL_SLICE = Duration.ofMillis(200);

    /** Puts the radio in standby on its internal oscillator. */
    public void standby() {
        command(SET_STANDBY, STANDBY_RC);
    }

    /**
     * The errors the radio has accumulated. Worth reading after configuring: a
     * failed image calibration shows up here and nowhere else, and it costs
     * sensitivity rather than producing an error.
     */
    public int errors() {
        byte[] answer = query(GET_ERRORS, 2);
        return ((answer[0] & 0xFF) << 8) | (answer[1] & 0xFF);
    }

    public void clearErrors() {
        command(CLEAR_ERRORS);
    }

    /**
     * The interrupt flags, read the way this one command has to be read.
     *
     * <p>GetStatus is not a command with a response — it is a <b>direct read</b>.
     * Clocking bytes out without sending anything first returns the two status
     * bytes and then the interrupt word, and there is no leading status byte to
     * discard because the first byte <em>is</em> it.
     *
     * <p>Treating it like an ordinary command shifts the whole word one byte
     * left, which puts RX_DONE at 0x0800 instead of 0x08. Nothing errors: the
     * interrupt fires, the flags are read, and every packet is quietly dropped as
     * "not a reception". That cost an evening.
     */
    public int irqStatus() {
        byte[] status = directRead(6);
        return ((status[2] & 0xFF) << 24) | ((status[3] & 0xFF) << 16)
                | ((status[4] & 0xFF) << 8) | (status[5] & 0xFF);
    }

    public void clearIrq(int mask) {
        command(CLEAR_IRQ, (mask >>> 24) & 0xFF, (mask >>> 16) & 0xFF,
                (mask >>> 8) & 0xFF, mask & 0xFF);
    }

    @Override
    public void close() {
        io.close();
    }

    // ------------------------------------------------------------------
    // The two shapes every command has
    // ------------------------------------------------------------------

    /** A command that only tells the radio something. */
    private void command(int opcode, int... arguments) {
        byte[] bytes = new byte[2 + arguments.length];
        bytes[0] = (byte) (opcode >> 8);
        bytes[1] = (byte) opcode;
        for (int i = 0; i < arguments.length; i++) {
            bytes[2 + i] = (byte) arguments[i];
        }
        io.awaitReady(READY_TIMEOUT);
        io.writeBytes(bytes);
    }

    /**
     * A command that answers. Two transactions with a wait between them, and the
     * first byte clocked back is the radio's status rather than the answer — it is
     * dropped here so that no caller has to remember it.
     */
    private byte[] query(int opcode, int responseLength, int... arguments) {
        command(opcode, arguments);

        io.awaitReady(READY_TIMEOUT);
        byte[] raw = new byte[responseLength + 1];
        io.readBytes(raw);

        byte[] response = new byte[responseLength];
        System.arraycopy(raw, 1, response, 0, responseLength);
        return response;
    }

    /**
     * Bytes clocked out with no command sent first, which is how the chip reports
     * its own state. See {@link #irqStatus()} for why this is not the same as a
     * command that answers.
     */
    private byte[] directRead(int length) {
        io.awaitReady(READY_TIMEOUT);
        byte[] bytes = new byte[length];
        io.readBytes(bytes);
        return bytes;
    }

    private void packetParams(LoraSettings settings, int payloadLength) {
        command(SET_PACKET_PARAMS,
                (settings.preambleSymbols() >> 8) & 0xFF, settings.preambleSymbols() & 0xFF,
                settings.explicitHeader() ? 0x00 : 0x01,
                payloadLength,
                settings.crc() ? 0x01 : 0x00,
                settings.invertIq() ? 0x01 : 0x00);
    }

    private void writeBuffer(byte[] payload) {
        int[] arguments = new int[payload.length];
        for (int i = 0; i < payload.length; i++) {
            arguments[i] = payload[i] & 0xFF;
        }
        command(WRITE_BUFFER8, arguments);
    }

    /**
     * The power amplifier and the ramp.
     *
     * <p>Which amplifier to use is a property of the board, not of the power
     * asked for. A module routes one of the chip's outputs to its antenna and
     * leaves the others unconnected, so choosing by power — the low power path
     * below 14 dBm, the high power one above — transmits into a pin that goes
     * nowhere for half of the range.
     *
     * <p>That is what this did, and it cost most of an evening: the Core1121
     * wires the high power output, the vendor's table selects it at every level
     * from -9 to +22 dBm, and asking for 14 dBm here selected the other one.
     * Nothing reports it. The radio transmits happily and the far end hears the
     * leakage.
     *
     * <p>The byte order was wrong as well — it is select, supply, duty cycle,
     * size — so the duty cycle was being set from the supply's value.
     */
    private void transmitPower(int powerDbm) {
        command(SET_PA_CONFIG,
                board.amplifier(),
                board.amplifierSupply(),
                board.amplifierDutyCycle(),
                board.amplifierSize());
        command(SET_TX_PARAMS, powerDbm & 0xFF, 0x04 /* 48 µs ramp */);
    }

    /** The 32 bit interrupt mask, sent for both DIO lines. */
    private static int[] irqMask(int mask) {
        return new int[] {
                (mask >>> 24) & 0xFF, (mask >>> 16) & 0xFF, (mask >>> 8) & 0xFF, mask & 0xFF,
                0, 0, 0, 0,
        };
    }

    static int millisecondsToTicks(long milliseconds) {
        return (int) Math.round(milliseconds * TICKS_PER_MS);
    }

    // ------------------------------------------------------------------
    // Value types, nested so client code can say Lr1121Driver.LoraSettings
    // ------------------------------------------------------------------

    /**
     * The handful of values that belong to the board rather than to the radio.
     *
     * <p>These are the ones a datasheet cannot tell you, because they describe how
     * the chip was wired: what supplies the crystal oscillator, and which DIO pins
     * drive the antenna switch. Getting them wrong produces a radio that answers
     * every command correctly and hears nothing, which is the worst failure a first
     * link can have.
     *
     * @param tcxoVoltage one of the {@code TCXO_} constants, or {@link #NO_TCXO}
     * @param tcxoStartupTicks how long to let the oscillator settle, in units of
     *        30.52 µs
     * @param rfSwitchEnable which DIO pins are used as the antenna switch at all
     * @param rfSwitchStandby their levels in standby
     * @param rfSwitchRx their levels while receiving
     * @param rfSwitchTx their levels while transmitting
     * @param rfSwitchTxHp the same for the high power path
     * @param rfSwitchTxHf the same for the high frequency (2.4 GHz) path
     */
    public record BoardConfig(int tcxoVoltage, int tcxoStartupTicks,
                              int rfSwitchEnable, int rfSwitchStandby, int rfSwitchRx,
                              int rfSwitchTx, int rfSwitchTxHp, int rfSwitchTxHf,
                              int amplifier, int amplifierSupply,
                              int amplifierDutyCycle, int amplifierSize) {

        public static final int NO_TCXO = -1;

        public static final int TCXO_1_6V = 0x00;
        public static final int TCXO_1_7V = 0x01;
        public static final int TCXO_1_8V = 0x02;
        public static final int TCXO_2_2V = 0x03;
        public static final int TCXO_2_4V = 0x04;
        public static final int TCXO_2_7V = 0x05;
        public static final int TCXO_3_0V = 0x06;
        public static final int TCXO_3_3V = 0x07;

        /** Which power amplifier the board's antenna is actually wired to. */
        public static final int PA_LOW_POWER = 0x00;
        public static final int PA_HIGH_POWER = 0x01;
        public static final int PA_HIGH_FREQUENCY = 0x02;

        public static final int PA_SUPPLY_REGULATOR = 0x00;
        public static final int PA_SUPPLY_BATTERY = 0x01;

        public static final int RFSW0 = 1 << 0;
        public static final int RFSW1 = 1 << 1;
        public static final int RFSW2 = 1 << 2;
        public static final int RFSW3 = 1 << 3;
        public static final int RFSW4 = 1 << 4;

        /**
         * The Waveshare Core1121, both the HF and the LF variant.
         *
         * <p>Taken from the vendor's own demo rather than worked out: a 3.0 V TCXO
         * with a 300 tick settling time, RFSW0 for receive and RFSW1 for transmit,
         * and no high frequency path in the switch — the 2.4 GHz output is a separate
         * connector.
         *
         * <p>The pin-by-pin wiring this was tested with is in the javadoc of
         * {@code Lr11xxLinkCheck}, alongside the hardware checks that use it.
         *
         * @see <a href="https://www.waveshare.com/wiki/Core1121-XF">Core1121-XF</a>
         */
        public static BoardConfig core1121() {
            return new BoardConfig(TCXO_3_0V, 300,
                    RFSW0 | RFSW1, 0, RFSW0, RFSW1, RFSW1, 0,
                    /*
                       The high power amplifier, from the battery rail, at every
                       power level — which is what the vendor's own table does for
                       all of -9 to +22 dBm without variation.
                
                       That uniformity is the tell. A board that never uses its low
                       power amplifier is a board whose low power output goes
                       nowhere, and selecting it transmits into an unconnected pin.
                    */
                    PA_HIGH_POWER, PA_SUPPLY_BATTERY, 0x04, 0x07);
        }

        public boolean hasTcxo() {
            return tcxoVoltage != NO_TCXO;
        }
    }

    /**
     * The modulation, which both ends of a link have to agree on exactly. One
     * mismatched field and the receiver hears nothing at all — there is no
     * negotiation and no error, which is what makes a link that will not come up so
     * frustrating to debug.
     *
     * @param spreadingFactor 5 to 12. Higher reaches further and takes longer: 16
     *        bytes is about 165 ms at SF9 and 1.3 s at SF12
     * @param bandwidth one of the {@code BW_} constants
     * @param codingRate one of the {@code CR_} constants
     * @param syncWord 0x12 for private networks, 0x34 for public LoRaWAN ones
     * @param preambleSymbols 8 is the usual choice and what LoRaWAN uses
     * @param explicitHeader whether the length travels in the packet. With an
     *        implicit header both ends must agree the length in advance
     * @param crc whether the radio appends and checks a CRC, which is how a corrupt
     *        packet is dropped rather than delivered as noise
     * @param invertIq LoRaWAN inverts it downlink so that gateways do not hear each
     *        other. For a plain link, false at both ends
     */
    public record LoraSettings(int spreadingFactor, int bandwidth, int codingRate, int syncWord,
                               int preambleSymbols, boolean explicitHeader, boolean crc,
                               boolean invertIq) {

        public static final int BW_125_KHZ = 0x04;
        public static final int BW_250_KHZ = 0x05;
        public static final int BW_500_KHZ = 0x06;

        public static final int CR_4_5 = 0x01;
        public static final int CR_4_6 = 0x02;
        public static final int CR_4_7 = 0x03;
        public static final int CR_4_8 = 0x04;

        /** The usual sync word for a link that is not part of a LoRaWAN network. */
        public static final int SYNC_WORD_PRIVATE = 0x12;
        public static final int SYNC_WORD_PUBLIC = 0x34;

        public LoraSettings {
            if (spreadingFactor < 5 || spreadingFactor > 12) {
                throw new IllegalArgumentException(
                        "Spreading factor is 5 to 12, was " + spreadingFactor);
            }
            if (preambleSymbols < 1 || preambleSymbols > 0xFFFF) {
                throw new IllegalArgumentException(
                        "Preamble length does not fit in the packet, was " + preambleSymbols);
            }
        }

        /**
         * A sensible starting point for a link across a house or a garden: SF9 at
         * 125 kHz, which sends a short packet in about a sixth of a second.
         */
        public static LoraSettings defaults() {
            return new LoraSettings(9, BW_125_KHZ, CR_4_5, SYNC_WORD_PRIVATE, 8, true, true, false);
        }

        /*
           A wither per field anyone realistically varies, so that a link can be
           described by naming what differs from the defaults. The canonical
           constructor takes eight arguments, two of which are bare booleans next to
           each other — write those two the wrong way round and the radio hears
           nothing, with no error to say so. Nobody should have to reach for it.
        */

        public LoraSettings withSpreadingFactor(int spreadingFactor) {
            return new LoraSettings(spreadingFactor, bandwidth, codingRate, syncWord,
                    preambleSymbols, explicitHeader, crc, invertIq);
        }

        /** One of the {@code BW_} constants. */
        public LoraSettings withBandwidth(int bandwidth) {
            return new LoraSettings(spreadingFactor, bandwidth, codingRate, syncWord,
                    preambleSymbols, explicitHeader, crc, invertIq);
        }

        /** One of the {@code CR_} constants. */
        public LoraSettings withCodingRate(int codingRate) {
            return new LoraSettings(spreadingFactor, bandwidth, codingRate, syncWord,
                    preambleSymbols, explicitHeader, crc, invertIq);
        }

        /** {@link #SYNC_WORD_PRIVATE} or {@link #SYNC_WORD_PUBLIC}. */
        public LoraSettings withSyncWord(int syncWord) {
            return new LoraSettings(spreadingFactor, bandwidth, codingRate, syncWord,
                    preambleSymbols, explicitHeader, crc, invertIq);
        }

        /**
         * Whether the radio appends and checks a CRC. Both ends must agree, and a
         * receiver expecting one hears a packet without it as a packet that failed
         * its check — which is indistinguishable from no packet at all.
         */
        public LoraSettings withCrc(boolean crc) {
            return new LoraSettings(spreadingFactor, bandwidth, codingRate, syncWord,
                    preambleSymbols, explicitHeader, crc, invertIq);
        }

        /**
         * Whether the low data rate optimisation has to be on. It is required when a
         * symbol lasts longer than 16 ms, which is the combination of a high
         * spreading factor and a narrow bandwidth — and both ends must make the same
         * decision, so it is derived rather than configured.
         */
        public boolean lowDataRateOptimisation() {
            double bandwidthHz = switch (bandwidth) {
                case BW_125_KHZ -> 125_000.0;
                case BW_250_KHZ -> 250_000.0;
                case BW_500_KHZ -> 500_000.0;
                default -> throw new IllegalStateException("Unknown bandwidth " + bandwidth);
            };
            return (Math.pow(2, spreadingFactor) / bandwidthHz) > 0.016;
        }
    }

    /**
     * A packet that arrived, with the two numbers that say how well.
     *
     * @param payload the bytes, exactly as the sender wrote them
     * @param rssiDbm signal strength; a LoRa link is workable well below -120
     * @param snrDb signal to noise; LoRa decodes below zero, which is the whole
     *        point of it — down to about -20 at SF12
     */
    public record ReceivedPacket(byte[] payload, int rssiDbm, double snrDb) {

        public int length() {
            return payload.length;
        }

        @Override
        public String toString() {
            StringBuilder hex = new StringBuilder(payload.length * 2);
            for (byte b : payload) {
                hex.append("%02X".formatted(b));
            }
            return "%d bytes, RSSI %d dBm, SNR %.1f dB: %s"
                    .formatted(payload.length, rssiDbm, snrDb, hex);
        }
    }
}
