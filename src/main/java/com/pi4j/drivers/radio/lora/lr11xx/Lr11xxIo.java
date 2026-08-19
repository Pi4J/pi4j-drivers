package com.pi4j.drivers.radio.lora.lr11xx;

import java.io.Closeable;
import java.time.Duration;

/**
 * The wires: SPI bytes, a reset line, a busy line and an interrupt line.
 *
 * <p>Deliberately below the radio rather than beside it. Everything that makes an
 * LR11xx an LR11xx — the opcodes, the two-transaction read, the status byte that
 * arrives before every response — lives in {@link Lr1121Driver}, where it can be tested
 * against a recording of the bytes. What is left here is what genuinely differs
 * between a Raspberry Pi, a laptop with a USB adapter and a test: moving bytes.
 *
 * <p>This mirrors the six functions of Semtech's own HAL, which is the interface
 * their C driver is written against. Keeping the same shape means a Java port of
 * anything they publish translates rather than being redesigned.
 */
public interface Lr11xxIo extends Closeable {

    /** One SPI transaction, MOSI only: chip select down, these bytes, chip select up. */
    void writeBytes(byte[] bytes);

    /**
     * One SPI transaction, MISO only, filling the array. The radio answers a
     * command in a transaction of its own, and the first byte of it is a status
     * byte rather than data — {@link Lr1121Driver} accounts for that, not this.
     */
    void readBytes(byte[] into);

    /**
     * Blocks until the busy line says the radio is ready for a command.
     *
     * @throws IllegalStateException if it is still busy when the time runs out, which
     *         means the radio is wedged or not there at all
     */
    void awaitReady(Duration timeout);

    /** Pulses the reset line and waits for the radio to come back. */
    void reset();

    /**
     * Waits for the interrupt line to go high.
     *
     * @return true if it did, false if the time ran out — which is not an error:
     *         a receiver that hears nothing for a minute is the ordinary case
     */
    boolean awaitInterrupt(Duration timeout);

    @Override
    void close();
}
