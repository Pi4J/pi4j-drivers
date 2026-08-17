package com.pi4j.drivers.radio.lora.lr11xx;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pi4j.io.gpio.digital.DigitalInput;

import org.junit.jupiter.api.Test;

/**
 * The one part of the Pi4J binding that can be checked without a radio, and the
 * part most likely to save somebody an evening.
 *
 * <p>A debounced input is not a slower driver, it is a driver that receives
 * nothing: Pi4J passes the value to the kernel, which then reports no event at all
 * for a pulse shorter than the window. This radio's pulses are far shorter than the
 * ten milliseconds Pi4J defaults to.
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
}
