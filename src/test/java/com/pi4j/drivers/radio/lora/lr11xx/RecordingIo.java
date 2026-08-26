package com.pi4j.drivers.radio.lora.lr11xx;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;

/**
 * A radio made of a list. It records every byte written and plays back answers
 * queued in advance, which is enough to check every command this driver builds
 * without any hardware.
 *
 * <p>The answers are queued <em>without</em> the status byte the radio sends
 * before a response: this adds it, so that a test reads like the datasheet rather
 * than like the wire.
 */
class RecordingIo implements Lr11xxIo {

    /** An answer, and whether the radio puts a status byte in front of it. */
    private record Answer(byte[] bytes, boolean statusPrefixed) {
    }

    private final List<byte[]> written = new ArrayList<>();
    private final Deque<Answer> answers = new ArrayDeque<>();

    /**
     * What the radio says once the queued answers run out, or null to insist that
     * every read was expected. A receiver waiting for a packet reads the flags
     * for as long as it waits, and there is no useful number of times to queue.
     */
    private Answer whenNothingLeft;

    int resets;
    int readyWaits;
    boolean interruptFires = true;
    boolean closed;

    /**
     * Queues the answer to a command, status byte excluded — the radio sends one
     * in front of every response and this adds it, so that a test reads like the
     * datasheet rather than like the wire.
     */
    void willAnswer(int... bytes) {
        answers.addLast(new Answer(toBytes(bytes), true));
    }

    /**
     * Queues the bytes of a direct read, which has no status byte in front of it
     * because its first byte is one. GetStatus is read this way.
     */
    void willAnswerDirectly(int... bytes) {
        answers.addLast(new Answer(toBytes(bytes), false));
    }

    /** What a direct read returns from then on, however many times it happens. */
    void willKeepAnsweringDirectly(int... bytes) {
        whenNothingLeft = new Answer(toBytes(bytes), false);
    }

    /** The same for a command's answer, for a radio that never changes its mind. */
    void willKeepAnswering(int... bytes) {
        whenNothingLeft = new Answer(toBytes(bytes), true);
    }

    private static byte[] toBytes(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }

    @Override
    public void writeBytes(byte[] bytes) {
        written.add(bytes.clone());
    }

    @Override
    public void readBytes(byte[] into) {
        Answer answer = answers.pollFirst();
        if (answer == null) {
            answer = whenNothingLeft;
        }
        if (answer == null) {
            throw new AssertionError("The driver read an answer that no test queued");
        }
        int offset = answer.statusPrefixed() ? 1 : 0;
        if (offset == 1) {
            into[0] = 0x00;
        }
        System.arraycopy(answer.bytes(), 0, into, offset,
                Math.min(answer.bytes().length, into.length - offset));
    }

    @Override
    public void awaitReady(Duration timeout) {
        readyWaits++;
    }

    @Override
    public void reset() {
        resets++;
    }

    @Override
    public boolean awaitInterrupt(Duration timeout) {
        return interruptFires;
    }

    @Override
    public void close() {
        closed = true;
    }

    // ------------------------------------------------------------------

    /** Every transaction, as hex, oldest first. */
    List<String> transactions() {
        return written.stream().map(HexFormat.of().withUpperCase()::formatHex).toList();
    }

    /** Whether a transaction with exactly these bytes was written. */
    boolean wrote(String hex) {
        return transactions().contains(hex);
    }

    /** The first transaction that begins with this opcode, as hex. */
    String transactionFor(String opcodeHex) {
        return transactions().stream()
                .filter(transaction -> transaction.startsWith(opcodeHex))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No command " + opcodeHex + " among " + transactions()));
    }

    int transactionCount() {
        return written.size();
    }
}
