package com.pi4j.drivers.radio.lora.lr11xx;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.pi4j.io.IO;
import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.OnOffRead;
import com.pi4j.io.OnOffWrite;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.spi.Spi;

/**
 * {@link Lr11xxIo} over Pi4J: SPI for the bytes, three digital lines for reset,
 * busy and the interrupt.
 *
 * <p>The three lines are held as {@link OnOffRead} / {@link OnOffWrite} rather than
 * as {@code DigitalInput} and {@code DigitalOutput}, which is what a Raspberry Pi's
 * own GPIO and an I/O expander have in common. Nothing here needs more than that:
 * the reset line is driven, the busy line is read, and the interrupt line is
 * listened to.
 *
 * <p>Package private on purpose. It is what {@link Lr1121Driver} builds when it is
 * handed Pi4J objects, and there is nothing here a caller needs to name.
 */
class Pi4jLr11xxIo implements Lr11xxIo {

    /** How long a reset pulse is held, and how long the radio then needs. */
    private static final Duration RESET_PULSE = Duration.ofMillis(5);
    private static final Duration RESET_RECOVERY = Duration.ofMillis(300);

    private static final int BUSY_POLL_MICROS = 200;

    private final Spi spi;
    private final OnOffWrite<?> reset;
    private final OnOffRead<?> busy;
    private final ListenableOnOffRead<?> interrupt;

    /**
     * Whether the bus and the lines are ours to close.
     *
     * <p>They are when the {@link Lr1121Driver} constructor that takes a Pi4J
     * context created them, and they are not when a caller built them and handed
     * them over: that caller may well have plans for the pins after the radio is
     * done with them.
     */
    private final boolean ownsIo;

    /**
     * Edges the radio has raised and nobody has collected yet.
     *
     * <p>Pi4J reports edges to a listener; this driver needs to block until one
     * arrives. A semaphore is the bridge, and it has to be one rather than a
     * rendezvous: the caller puts the radio into receive mode and only then starts
     * waiting, so an edge landing in between has to be remembered rather than
     * dropped. The kernel does exactly that with the events it queues on the line's
     * file descriptor.
     */
    private final Semaphore edges = new Semaphore(0);

    private final Consumer<Boolean> edgeListener = on -> {
        /*
           Pi4J has no rising-edge-only request, so both edges arrive here and the
           falling one is dropped. The radio raises the interrupt line to signal and
           lowers it when the interrupt is cleared, so counting both would report
           twice as many receptions as there were.
        */
        if (on) {
            edges.release();
        }
    };

    Pi4jLr11xxIo(Spi spi, OnOffWrite<?> reset, OnOffRead<?> busy,
                 ListenableOnOffRead<?> interrupt) {
        this(spi, reset, busy, interrupt, false);
    }

    Pi4jLr11xxIo(Spi spi, OnOffWrite<?> reset, OnOffRead<?> busy,
                 ListenableOnOffRead<?> interrupt, boolean ownsIo) {
        this.ownsIo = ownsIo;
        this.spi = spi;
        this.reset = reset;
        this.busy = busy;
        this.interrupt = interrupt;

        requireUndebounced("busy", busy);
        requireUndebounced("interrupt", interrupt);

        interrupt.addConsumer(edgeListener);
    }

    /**
     * Refuses a debounced input, which is the difference between this driver working
     * and this driver looking like a dead antenna.
     *
     * <p>{@code DigitalInput.DEFAULT_DEBOUNCE} is 10 000 µs and an unset config
     * materialises it, so an input built without a word about debouncing arrives
     * here debounced by ten milliseconds. That value is not a filter in Java: Pi4J
     * passes it to the kernel as {@code GPIO_V2_LINE_ATTR_ID_DEBOUNCE}, and the
     * kernel then requires the line to be stable for that long before reporting
     * anything at all. A pulse shorter than the window produces no event, not a late
     * one.
     *
     * <p>This radio holds its interrupt line high only until the driver clears the
     * interrupt — reading a buffer status, a payload and a packet status — which at a
     * few megahertz is an order of magnitude inside that window. So the default
     * costs every reception, silently, and the symptom is indistinguishable from a
     * wrong spreading factor or a missing antenna. Those are the first three things
     * anyone debugging a radio looks at, and none of them is the problem.
     *
     * <p>Hence an exception at construction rather than a warning. The driver cannot
     * fix the input — the caller built it — and a radio that never receives is a
     * worse outcome than a constructor that will not run. Neither of these lines
     * comes from anything that bounces; they are chip outputs.
     *
     * <p>Only a Pi4J {@code DigitalInput} carries a debounce setting. A line that
     * arrives as something else — an expander pin, a test double — has no kernel
     * window to get wrong, so there is nothing to refuse.
     */
    private static void requireUndebounced(String which, OnOffRead<?> line) {
        if (line instanceof DigitalInput input) {
            requireUndebounced(which, input.config().debounce());
        }
    }

    /** The check itself, on the value, so it can be tested without a GPIO line. */
    static void requireUndebounced(String which, Long debounce) {
        if (debounce != null && debounce != 0L) {
            throw new IllegalArgumentException(("The %s line is debounced by %d us, which for an"
                    + " LR11xx means the kernel discards the pulses this driver waits for. Build"
                    + " the input with .debounce(0L). Note that leaving debounce unset is not the"
                    + " same thing: Pi4J defaults it to %d us.")
                    .formatted(which, debounce, DigitalInput.DEFAULT_DEBOUNCE));
        }
    }

    @Override
    public void writeBytes(byte[] bytes) {
        spi.write(bytes, 0, bytes.length);
    }

    @Override
    public void readBytes(byte[] into) {
        spi.read(into, 0, into.length);
    }

    /**
     * Polls the busy line rather than waiting on an edge. The radio is busy for
     * microseconds after most commands, and an edge subscription set up and torn
     * down each time would cost more than the wait.
     */
    @Override
    public void awaitReady(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (busy.isOn()) {
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException(
                        "The radio has been busy for " + timeout + ": either the busy wire is not"
                        + " connected, or the radio is being held in reset, or it has no power.");
            }
            sleepMicros(BUSY_POLL_MICROS);
        }
    }

    /**
     * Pulses NRST, which is active low: the line's off state holds the radio in
     * reset and its on state runs it.
     */
    @Override
    public void reset() {
        this.reset.off();
        sleep(RESET_PULSE);
        this.reset.on();
        sleep(RESET_RECOVERY);

        /*
           A reset raises and lowers the interrupt line on its way back up, and an
           edge left in the semaphore would be collected by the next receive() as
           though a packet had arrived — which reads as a reception of zero bytes.
        */
        edges.drainPermits();
    }

    @Override
    public boolean awaitInterrupt(Duration timeout) {
        try {
            if (!edges.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                return false;
            }
            /*
               One wait consumes the whole burst. The caller reads the radio's
               interrupt register to find out what happened, and that register
               reports everything since it was last cleared — so a second permit
               would only send it back to read a register it has already emptied.
            */
            edges.drainPermits();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the radio", e);
        }
    }

    @Override
    public void close() {
        interrupt.removeConsumer(edgeListener);

        if (ownsIo) {
            closeLine(interrupt);
            closeLine(busy);
            closeLine(reset);
            spi.close();
        }
    }

    /**
     * Closes a line the driver created itself. The lines are held as on/off
     * interfaces, which say nothing about closing — but anything Pi4J handed out is
     * an {@link IO}, and that does.
     */
    private static void closeLine(Object line) {
        if (line instanceof IO<?, ?, ?> io) {
            io.close();
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while resetting the radio", e);
        }
    }

    private static void sleepMicros(long micros) {
        try {
            Thread.sleep(Duration.ofNanos(micros * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the radio", e);
        }
    }
}
