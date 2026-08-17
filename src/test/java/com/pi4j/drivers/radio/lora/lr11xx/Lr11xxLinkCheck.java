package com.pi4j.drivers.radio.lora.lr11xx;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.spi.SpiChipSelect;

/**
 * Tries an LR11xx on real hardware, from a {@code main} as well as from a test.
 *
 * <h2>Why a main as well</h2>
 *
 * A radio cannot hear itself, so proving this driver means two machines at once —
 * and the convenient way to drive two machines is over SSH from a third. Maven on
 * the far end works and is what {@link Lr1121DriverHardwareTest} documents, but it
 * wants a checkout, a settings file and a warm repository on every device. A class
 * with a {@code main} wants a JRE and a directory of jars, which {@code rsync}
 * puts there in one go:
 *
 * <pre>
 * mvn -q test-compile dependency:copy-dependencies -DincludeScope=test
 * rsync -a target/classes target/test-classes target/dependency pi-b:/tmp/lr11xx/
 * ssh pi-b 'cd /tmp/lr11xx &amp;&amp; java --enable-native-access=ALL-UNNAMED \
 *     -cp "classes:test-classes:dependency/*" \
 *     com.pi4j.drivers.radio.lora.lr11xx.Lr11xxLinkCheck receive'
 * </pre>
 *
 * <p>Roles are {@code check}, {@code transmit} and {@code receive}. Run
 * {@code check} first on each machine: it asks the chip what it is, and a radio
 * that will not answer has a wiring fault that no amount of link debugging will
 * find. Then start {@code receive} on one and {@code transmit} on the other.
 *
 * <h2>The wiring these defaults assume</h2>
 *
 * A Waveshare Core1121 on a Raspberry Pi's 40 pin header, SPI0:
 *
 * <pre>
 *   Core1121   Raspberry Pi   BCM   Header pin
 *   ------------------------------------------
 *   VCC        3V3             -    1
 *   GND        GND             -    6
 *   SCK        SPI0 SCLK      11    23
 *   MOSI       SPI0 MOSI      10    19
 *   MISO       SPI0 MISO       9    21
 *   NSS        SPI0 CE0        8    24
 *   RESET      GPIO           22    15
 *   BUSY       GPIO           24    18
 *   DIO9       GPIO           23    16
 * </pre>
 *
 * <p><b>DIO9, not DIO1.</b> The Core1121 brings several DIO pins out and only one of
 * them carries the interrupt this driver waits on.
 *
 * <p>The first six rows are not configurable here and do not need to be: they follow
 * from the SPI bus and chip select, and the kernel drives NSS around each transfer.
 * Only the last three are pin numbers this code passes to Pi4J. <b>The BCM column is
 * the one that matters</b> — that is what the properties below take. The header pin
 * column is for wiring by eye and nothing reads it.
 *
 * <p>Enable SPI first, or the bus will not exist: {@code sudo raspi-config} →
 * Interface Options → SPI. The process also needs {@code spi}, {@code gpio},
 * {@code dialout} and {@code i2c} — Pi4J's FFM provider checks the last two even
 * for a device that uses neither, and it reads the group database, so
 * {@code usermod -aG} rather than a systemd unit's {@code SupplementaryGroups=}.
 *
 * <h2>Parameters</h2>
 *
 * Override any of it with system properties, which work the same from Maven and
 * from the command line:
 *
 * <pre>
 * -Dlr11xx.bus=0 -Dlr11xx.reset=22 -Dlr11xx.busy=24 -Dlr11xx.irq=23
 * -Dlr11xx.frequency=868000000 -Dlr11xx.sf=7 -Dlr11xx.power=14 -Dlr11xx.seconds=30
 * </pre>
 *
 * <p><b>Both ends must agree on the frequency and the spreading factor.</b> There is
 * no negotiation in LoRa and no error when two ends disagree — a mismatch and a
 * missing antenna both sound like silence — so change them together, and one at a
 * time.
 *
 * <p>The antenna goes on before power. A transmitter driving an open port is a
 * transmitter damaging itself.
 */
public final class Lr11xxLinkCheck {

    /** Waveshare's own numbers for the Core1121 on a Raspberry Pi. */
    static final int BUS = intProperty("lr11xx.bus", 0);
    static final int RESET_PIN = intProperty("lr11xx.reset", 22);
    static final int BUSY_PIN = intProperty("lr11xx.busy", 24);
    static final int IRQ_PIN = intProperty("lr11xx.irq", 23);

    static final long FREQUENCY_HZ = longProperty("lr11xx.frequency", 868_000_000L);
    static final int SPREADING_FACTOR = intProperty("lr11xx.sf", 7);

    /** 14 dBm is the EU868 limit on most channels. */
    static final int POWER_DBM = intProperty("lr11xx.power", 14);

    /** How long the receiver waits, and how long the transmitter keeps sending. */
    static final Duration DURATION = Duration.ofSeconds(longProperty("lr11xx.seconds", 30));

    private Lr11xxLinkCheck() {
    }

    public static void main(String[] args) {
        String role = args.length > 0 ? args[0] : System.getProperty("lr11xx.role", "check");
        try {
            switch (role) {
                case "check" -> System.out.println("Radio reports: " + check());
                case "transmit" -> System.out.println("Sent " + transmit() + " packet(s)");
                case "receive" -> {
                    int heard = receive();
                    System.out.println("Heard " + heard + " packet(s)");
                    if (heard == 0) {
                        System.err.println(NOTHING_HEARD);
                        System.exit(1);
                    }
                }
                default -> {
                    System.err.println("Roles are: check, transmit, receive");
                    System.exit(2);
                }
            }
        } catch (Exception | LinkageError e) {
            /*
               LinkageError too. Several Pi4J providers may be on the classpath and
               the auto context picks whichever registers, so a machine where one
               declines can end up on another that then fails to load its native
               library — an Error, which would otherwise leave no explanation.
            */
            e.printStackTrace();
            System.exit(1);
        }
    }

    static final String NOTHING_HEARD =
            "Nothing arrived. Either the transmitter is not running, or the two ends disagree:"
            + " compare the frequency, the spreading factor and the CRC setting, and check that"
            + " both antennas are on. A mismatch sounds exactly like a missing antenna.";

    /**
     * Asks the chip what it is, then configures it.
     *
     * @return what it said, so a caller can judge it. Zeros or 0xFF mean the wiring
     *         rather than anything the radio did
     */
    static Lr1121Driver.Version check() {
        Context pi4j = context();
        try (Lr1121Driver radio = radio(pi4j)) {
            Lr1121Driver.Version version = radio.version();
            System.out.println("Radio reports: " + version);

            radio.configure(Lr1121Driver.BoardConfig.core1121());
            radio.configureLora(FREQUENCY_HZ, settings());

            int errors = radio.errors();
            if (errors != 0) {
                throw new IllegalStateException(("The chip reports errors 0x%04X after"
                        + " configuring. On this board that is usually the TCXO voltage.")
                        .formatted(errors));
            }
            return version;
        } finally {
            shutdownQuietly(pi4j);
        }
    }

    /** Sends until the time runs out. Start the listener on the other machine first. */
    static int transmit() {
        Context pi4j = context();
        try (Lr1121Driver radio = radio(pi4j)) {
            radio.configure(Lr1121Driver.BoardConfig.core1121());
            radio.configureLora(FREQUENCY_HZ, settings());

            System.out.printf("Sending on %.3f MHz, SF%d, %d dBm for %s%n",
                    FREQUENCY_HZ / 1e6, SPREADING_FACTOR, POWER_DBM, DURATION);

            int sent = 0;
            Instant deadline = Instant.now().plus(DURATION);
            while (Instant.now().isBefore(deadline)) {
                byte[] payload = ("pi4j-drivers " + sent).getBytes();
                radio.transmit(payload, settings(), POWER_DBM, Duration.ofSeconds(10));
                sent++;
                System.out.println("  sent " + new String(payload));
                sleep(Duration.ofSeconds(2));
            }
            return sent;
        } finally {
            shutdownQuietly(pi4j);
        }
    }

    /** Listens for the duration and reports what arrived, with its signal strength. */
    static int receive() {
        Context pi4j = context();
        try (Lr1121Driver radio = radio(pi4j)) {
            radio.configure(Lr1121Driver.BoardConfig.core1121());
            radio.configureLora(FREQUENCY_HZ, settings());

            System.out.printf("Listening on %.3f MHz, SF%d for %s%n",
                    FREQUENCY_HZ / 1e6, SPREADING_FACTOR, DURATION);

            int heard = 0;
            Instant deadline = Instant.now().plus(DURATION);
            while (Instant.now().isBefore(deadline)) {
                Optional<Lr1121Driver.ReceivedPacket> packet =
                        radio.receive(settings(), Duration.ofSeconds(5));
                if (packet.isPresent()) {
                    heard++;
                    System.out.printf("  %d bytes, RSSI %d dBm, SNR %.1f dB: %s%n",
                            packet.get().payload().length, packet.get().rssiDbm(),
                            packet.get().snrDb(), new String(packet.get().payload()));
                }
            }
            return heard;
        } finally {
            shutdownQuietly(pi4j);
        }
    }

    // ------------------------------------------------------------------

    static Lr1121Driver.LoraSettings settings() {
        return Lr1121Driver.LoraSettings.defaults()
                .withSpreadingFactor(SPREADING_FACTOR)
                .withCrc(false);
    }

    static Context context() {
        return Pi4J.newAutoContext();
    }

    static Lr1121Driver radio(Context pi4j) {
        return new Lr1121Driver(pi4j, BUS, SpiChipSelect.CS_0, RESET_PIN, BUSY_PIN, IRQ_PIN);
    }

    /**
     * A shutdown that throws must not replace what the run was actually reporting.
     * In a {@code finally} block it would, and then a clean "no radio here" arrives
     * as an error about the shutdown.
     */
    static void shutdownQuietly(Context pi4j) {
        try {
            pi4j.shutdown();
        } catch (Exception | LinkageError e) {
            System.out.println("Pi4J shutdown complained, which is not the result: "
                    + e.getMessage());
        }
    }

    static String wiring() {
        return "SPI bus " + BUS + " with reset/busy/irq on BCM "
                + RESET_PIN + "/" + BUSY_PIN + "/" + IRQ_PIN;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static int intProperty(String name, int fallback) {
        return (int) longProperty(name, fallback);
    }

    private static long longProperty(String name, long fallback) {
        String value = System.getProperty(name);
        return value == null || value.isBlank() ? fallback : Long.parseLong(value.trim());
    }
}
