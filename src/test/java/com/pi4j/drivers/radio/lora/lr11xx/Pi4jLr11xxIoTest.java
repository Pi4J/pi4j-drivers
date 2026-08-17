package com.pi4j.drivers.radio.lora.lr11xx;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.gpio.digital.DigitalInput;

import org.junit.jupiter.api.Test;

/**
 * What the Pi4J binding does around the bytes, checked without a radio: the
 * debounce refusal, the lines taken as plain on/off interfaces, and what a reset
 * leaves behind.
 *
 * <p>The debounce refusal is the part most likely to save somebody an evening. A
 * debounced input is not a slower driver, it is a driver that receives nothing:
 * Pi4J passes the value to the kernel, which then reports no event at all for a
 * pulse shorter than the window. This radio's pulses are far shorter than the ten
 * milliseconds Pi4J defaults to.
 */
class Pi4jLr11xxIoTest {

    /** Zero is the only correct answer for a line driven by a chip. */
    @Test
    void anUndebouncedInputIsAccepted() {
        assertDoesNotThrow(() -> Pi4jLr11xxIo.requireUndebounced("interrupt", 0L));
    }

    /**
     * Pi4J's default is the case that matters. An input built without a word about
     * debouncing arrives with 10 000 µs, so this is what a caller who never thought
     * about it will hit — and it has to fail loudly rather than at the antenna.
     */
    @Test
    void pi4jsOwnDefaultIsRefused() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> Pi4jLr11xxIo.requireUndebounced("interrupt",
                        DigitalInput.DEFAULT_DEBOUNCE));

        assertTrue(thrown.getMessage().contains("interrupt"),
                "the message has to say which line, since two are passed in");
        assertTrue(thrown.getMessage().contains(".debounce(0L)"),
                "and it has to say what to do about it");
    }

    /**
     * Any non-zero value, not just the default. A small debounce might look harmless
     * and still be longer than the pulse: the driver clears the interrupt as fast as
     * the bus allows, and it has no way to know how fast that is on someone else's
     * wiring.
     */
    @Test
    void anyDebounceAtAllIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> Pi4jLr11xxIo.requireUndebounced("busy", 1L));
        assertThrows(IllegalArgumentException.class,
                () -> Pi4jLr11xxIo.requireUndebounced("busy", 500L));
    }

    /** A provider that reports nothing has not debounced anything. */
    @Test
    void anAbsentValueIsNotADebounce() {
        assertDoesNotThrow(() -> Pi4jLr11xxIo.requireUndebounced("interrupt", null));
    }

    /**
     * The three lines are on/off interfaces rather than GPIO objects so that an I/O
     * expander's pins can carry them. This is that case: plain on/off lines, no
     * {@code DigitalInput} anywhere, and the driver still waits on the interrupt and
     * still polls busy.
     *
     * <p>The SPI bus is null on purpose — nothing checked here moves a byte, and a
     * fake bus would only be a place for a mistake to hide.
     */
    @Test
    void linesThatAreNotGpioObjectsStillWork() {
        ListenableOnOffRead.Impl busy = new ListenableOnOffRead.Impl(false);
        ListenableOnOffRead.Impl interrupt = new ListenableOnOffRead.Impl(false);

        try (Pi4jLr11xxIo io = new Pi4jLr11xxIo(null, new ListenableOnOffRead.Impl(true),
                busy, interrupt)) {
            assertDoesNotThrow(() -> io.awaitReady(Duration.ofMillis(50)),
                    "a line that is off is a radio that is ready");

            assertFalse(io.awaitInterrupt(Duration.ofMillis(20)),
                    "nothing has happened, so the wait has to time out");

            interrupt.setState(true);
            assertTrue(io.awaitInterrupt(Duration.ofMillis(20)),
                    "the edge the line just raised is what the driver waits for");
        }
    }

    /**
     * Reset is active low, and the line is left switched on — which for a directly
     * wired NRST is high, and for an inverted one is whatever the caller declared on
     * to be. A radio left in reset answers nothing.
     */
    @Test
    void resetLeavesTheLineOn() {
        ListenableOnOffRead.Impl reset = new ListenableOnOffRead.Impl(true);
        ListenableOnOffRead.Impl interrupt = new ListenableOnOffRead.Impl(false);

        try (Pi4jLr11xxIo io = new Pi4jLr11xxIo(null, reset,
                new ListenableOnOffRead.Impl(false), interrupt)) {
            interrupt.setState(true);

            io.reset();

            assertTrue(reset.isOn(), "the radio has to be let out of reset again");
            assertFalse(io.awaitInterrupt(Duration.ofMillis(20)),
                    "the edge from before the reset would read as a packet of nothing");
        }
    }
}
