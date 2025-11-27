package com.pi4j.drivers.sensor.geospatial.hcsr04;

import com.pi4j.drivers.sensor.Sensor;
import com.pi4j.drivers.sensor.SensorDescriptor;
import com.pi4j.io.OnOffRead;
import com.pi4j.io.OnOffWrite;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;

import java.io.Closeable;
import java.io.IOException;


/**
 * A class to interface with the HC-SR04 ultrasonic distance sensor, allowing
 * distance measurements by calculating the time taken for an ultrasonic pulse
 * to travel to an object and back.
 *
 * <p>This class uses the Pi4J library to control the GPIO pins of a Raspberry Pi.
 * It configures one pin as a trigger (output) and another as an echo (input) to
 * measure distance based on the duration of the echo pulse.
 */
public class Hcsr04Driver implements Sensor {
    public static SensorDescriptor DESCRIPTOR = new SensorDescriptor.Builder().addValue(SensorDescriptor.Kind.DISTANCE).build();

    private static final int TRIGGER_DURATION_NANOS = 10_000; // 10us
    private static final int TIMEOUT_NANOS = 100_000_000;
    private static final int NANOS_PER_SECOND = 1000_000_000;

    private static final double SPEED_OF_SOUND = 342.25; // Speed of sound in meters per second at 18°C


    private final OnOffWrite<?> triggerPin;
    private final OnOffRead<?> echoPin;

    public Hcsr04Driver(DigitalOutput triggerPin, DigitalInput echoPin) {
        this.triggerPin = triggerPin;
        this.echoPin = echoPin;
        triggerPin.off();
    }

    @Override
    public SensorDescriptor getDescriptor() {
        return DESCRIPTOR;
    }


    /**
     * Measures the distance to an object by sending an ultrasonic pulse and measuring the
     * time taken for the pulse to return. The result is returned in meters.
     *
     * <p>This method sends a 10-microsecond pulse on the trigger pin and then measures
     * the duration of the echo pulse on the echo pin. The distance is calculated based
     * on the duration of the echo pulse.
     *
     * @return the calculated distance to the object in meters, or NaN if the
     * measurement times out.
     */
    public double measureDistance() {

        // Set trigger high for 10 microseconds
        triggerPin.on();
        long startTrigger = System.nanoTime();
        while (System.nanoTime() - startTrigger < TRIGGER_DURATION_NANOS) {
            // Busy wait
        }
        triggerPin.off();

        // Start the measurement
        while (echoPin.isOff()) {
            if (System.nanoTime() - startTrigger > TIMEOUT_NANOS) {
                return Double.NaN;
            }
        }

        long startEcho = System.nanoTime();

        // Wait till measurement is finished
        while (echoPin.isOn()) {
            if (System.nanoTime() - startEcho > TIMEOUT_NANOS) {
                return Double.NaN;
            }
        }
        long endEcho = System.nanoTime();

        // Output the distance
        double measuredSeconds = (endEcho - startEcho) / 1000_000_000.0;

        return measuredSeconds * SPEED_OF_SOUND / 2;
    }

    @Override
    public void readMeasurement(float[] values) {
        values[0] = (float) measureDistance();
    }


    @Override
    public void close() {
        if (triggerPin instanceof Closeable) {
            try {
                ((Closeable) triggerPin).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                ((Closeable) echoPin).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Private helpers

    /**
     * Waits for the echo signal to either go high or low, depending on the specified parameter.
     *
     * <p>This method implements a busy-wait with a timeout to prevent infinite waiting.
     *
     * @param high {@code true} to wait for the echo signal to go high, {@code false} to wait for it to go low
     * @return the time in nanoseconds when the echo signal changed, or the current time if timed out
     */
    private long waitForEchoSignal(boolean high) {
        long timeoutStart = System.nanoTime();
        while ((echoPin.isOn() == high) && System.nanoTime() - timeoutStart < 25_000_000) {
            // Busy-wait until signal changes or timeout
        }
        return System.nanoTime();
    }

    /**
     * Calculates the distance to an object based on the duration of the echo pulse.
     *
     * <p>This method uses the speed of sound to convert the measured pulse duration
     * (from the HC-SR04 sensor) into a distance. The calculation is done in
     * centimeters and accounts for the round-trip of the signal (to the object and back).
     *
     * @param pulseStartTime the start time of the echo pulse in nanoseconds
     * @param pulseEndTime   the end time of the echo pulse in nanoseconds
     * @return the calculated distance to the object in centimeters, or -1 if the
     * pulse duration is outside expected bounds (handled in measureDistance).
     */
    private double calculateDistance(long pulseStartTime, long pulseEndTime) {
        long pulseDuration = pulseEndTime - pulseStartTime;
        System.out.println("Pulse duration: " + pulseDuration);// Duration in nanoseconds
        return pulseDuration * (SPEED_OF_SOUND / 1_000_000_000 / 2); // Directly in meters
    }

}
