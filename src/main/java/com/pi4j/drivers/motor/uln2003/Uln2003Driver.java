package com.pi4j.drivers.motor.uln2003;

import com.pi4j.io.OnOffWrite;

import java.time.Duration;

/** A driver for the ULN2003 motor driver */
public class Uln2003Driver {

    private static final int[] SEQUENCE = {0b1000, 0b1100, 0b0100, 0b0110, 0b0010, 0b0011, 0b0001, 0b1001};
    private static final int[] REVERSE = {0b1001, 0b0001, 0b0011, 0b0010, 0b0110, 0b0100, 0b1100, 0b1000};
    private static final Duration DEFAULT_DELAY = Duration.ofMillis(5);

    private final OnOffWrite<?> pin1;
    private final OnOffWrite<?> pin2;
    private final OnOffWrite<?> pin3;
    private final OnOffWrite<?> pin4;

    /** Creates a new driver with the given digital output pins connected to in1..in4 */
    public Uln2003Driver(OnOffWrite<?> pin1, OnOffWrite<?> pin2, OnOffWrite<?> pin3, OnOffWrite<?> pin4) {
        this.pin1 = pin1;
        this.pin2 = pin2;
        this.pin3 = pin3;
        this.pin4 = pin4;
    }

    /**
     * Moves the motor the given number of steps forward; backwards for negative step counts, using a delay
     * of 5ms between steps.
     */
    public void move(int steps) {
        move(steps, DEFAULT_DELAY);
    }

    /**
     * Moves the motor the given number of steps forward; backwards for negative step counts, using the given delay
     * between steps.
     */
    public void move(int steps, Duration delay) {
        int absSteps = Math.abs(steps);
        int[] sequence = steps < 0 ? REVERSE : SEQUENCE;
        for (int i = 0; i < absSteps; i++) {
            for (int pattern : sequence) {
                pin1.setState((pattern & 8) != 0);
                pin2.setState((pattern & 4) != 0);
                pin3.setState((pattern & 2) != 0);
                pin4.setState((pattern & 1) != 0);
                try {
                    Thread.sleep(delay);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
