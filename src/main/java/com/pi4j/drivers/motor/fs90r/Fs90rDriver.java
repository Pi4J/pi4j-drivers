package com.pi4j.drivers.motor.fs90r;

import com.pi4j.io.pwm.Pwm;

/**
 * Driver for a FS90R servo motor.
 * The Fs90R defines a specific PWM interface and exactly how the PWM signal
 * controls the servo
 * <p>
 *      <a href="https://www.pololu.com/product/2820"> Seervo Details</a>
 * <p>
 * <p>
 * These devices do not have a customary DataSheet.  Most servo devices provide limited specifications
 * for required pulse width, they just say 1 - 2 millisecond.
 * <p>
 * The FS90 R 360 defines a specific PWM interface and exactly how the PWN signal
 * controls the servo. There are many different manufacturers of the FS90 servo.
 * A minimal pulse width produces maximum clockwise (CW) rotation, while a maximum pulse width  produces maximum
 * counterclockwise (CCW) rotation; the exact pulse widths vary by manufacturer. The application defaults
 * (low)1000 - (high)2000 microseconds. The user of this example application can change these defaults via
 * setPulseWidthMin() and setPulseWidthMax()
 * <p>
 * If the servo does not rotate to the expected RPM,Example to rotate faster CW  reduce the low,
 * to rotate CCW faster increase the high. The low high change should be balanced or 90 degree will not properly align.
 * Meaning if you reduce the low by 200, you should increase the high by 200, or vice versa.
 *
 *
 */

public class Fs90rDriver {

    public static final int PWM_FREQUENCY = 50;

    /* The servo direction of rotation initially is UNSET. This requires the application to
    setDirection() before setSpeed() usage.   This ensure the direction is fully controlled by the application
     */
    public enum ServoDirection {
        UNSET,
        CLOCKWISE,
        COUNTERCLOCKWISE
    }

    private static final double PULSE_WIDTH_MIN_CLOCKWISE = 1000.0;
    private static final double PULSE_WIDTH_MAX_COUNTERCLOCKWISE = 2000.0;
    private ServoDirection direction;
    private boolean isRotating;
    private final Pwm pwm;
    /* these are the pulse width times for the extreme (MAX) speed clockwise and counterclockwise.
    These values and their range are used in calculating the PulseWidth based upon percentage (@see setSpeed())
     */
    private double pulseWidthMin;
    private double pulseWidthMax;
    private double totalPulseWidthRange;

    /**
     *
     * @param pwm Hardware PWM
     */
    public Fs90rDriver(Pwm pwm) {
        this(pwm, PULSE_WIDTH_MIN_CLOCKWISE, PULSE_WIDTH_MAX_COUNTERCLOCKWISE);
    }

    /**
     *
     * @param pwm           pwm device
     * @param pulseWidthMin value in microseconds for 0 degree max clockwise
     * @param pulseWidthMax value in microseconds for 180 degree max counterclockwise
     */
    public Fs90rDriver(Pwm pwm, double pulseWidthMin, double pulseWidthMax) {
        this.pwm = pwm;
        this.pulseWidthMin = pulseWidthMin;
        this.pulseWidthMax = pulseWidthMax;
        this.totalPulseWidthRange = this.pulseWidthMax - this.pulseWidthMin;
        this.direction = ServoDirection.UNSET;
        if (pwm == null) {
            throw new IllegalArgumentException("PWM is null");
        }
        if (!Double.isFinite(pulseWidthMin) || pulseWidthMin < 500.0 || pulseWidthMin > 1500.0) {
            throw new IllegalArgumentException("pulseWidthMin range 500.0 ... 1500.0 ");
        }
        if (!Double.isFinite(pulseWidthMax) || pulseWidthMax < 1500.0 || pulseWidthMax > 2500.0) {
            throw new IllegalArgumentException("pulseWidthMax range 1500.0 ... 2500.0 ");
        }
        if (pulseWidthMin >= pulseWidthMax) {
            throw new IllegalArgumentException("pulseWidthMin must be less than pulseWidthMax");
        }
    }

    /**
     * Minimal duration pulse width to use.
     *
     * @param pulseWidthMin Greater than 500.0 less than 1500.0
     */
    public void setPulseWidthMin(double pulseWidthMin) {
        if (!Double.isFinite(pulseWidthMin) || pulseWidthMin < 500.0 || pulseWidthMin > 1500.0) {
            throw new IllegalArgumentException("pulseWidthMin range 500.0 ... 1500.0 ");
        }
        this.pulseWidthMin = pulseWidthMin;
        this.totalPulseWidthRange = this.pulseWidthMax - this.pulseWidthMin;
    }

    /**
     * Maximum duration pulse width to use.
     *
     * @param pulseWidthMax Greater than 1500.0 less than 2500.0
     */
    public void setPulseWidthMax(double pulseWidthMax) {
        if (!Double.isFinite(pulseWidthMax) || pulseWidthMax < 1500.0 || pulseWidthMax > 2500.0) {
            throw new IllegalArgumentException("pulseWidthMax range 1500.0 ... 2500.0 ");
        }
        this.pulseWidthMax = pulseWidthMax;
        this.totalPulseWidthRange = this.pulseWidthMax - this.pulseWidthMin;
    }

    /**
     * Control direction of rotation.  After this has been set the setSpeed()
     * will use this value in rotating the servo clockwise or counterclockwise
     *
     * @param direction Servo must be stopped before direction can be changed
     */
    public void setDirection(ServoDirection direction) {

        if (!isRotating || (direction == this.direction)) {
            this.direction = direction;
        } else {
            throw new IllegalStateException("Cannot set new direction without stopping rotation");
        }
    }

    /**
     * Sets the servo rotation speed to this percentage of maximum. The servo direction, clockwise/counterclockwise,
     * determined by the setDirection().
     *
     * @param percent rotation command from 0% through 100%.  setDirection() must be configured.
     */
    public void setSpeed(double percent) {

        if (!Double.isFinite(percent) || percent < 0.0 || percent > 100.0) {
            throw new IllegalArgumentException("percentage must be finite and between 0.0% and 100.0%");
        }
        if (direction == ServoDirection.UNSET) {
            throw new IllegalArgumentException("percent control not possible, rotation direction not set");
        }

        double degree;

        this.isRotating = percent != 0.0;

        if (direction == ServoDirection.COUNTERCLOCKWISE) {
            degree = 90.0 + (90.0 * percent / 100);
        } else {
            degree = 90.0 - (90.0 * percent / 100);
        }

        pwm.on(degreeToDutyCycle(degree), PWM_FREQUENCY);
    }

    /**
     *
     * @param degree Degree value used to set direction and speed of rotation.  This is converted to
     *               pulse width.
     * @return duty cycle
     */
    double degreeToDutyCycle(double degree) {
        double targetPulseWidth = this.pulseWidthMin + (degree / 180.0 * totalPulseWidthRange);
        return (targetPulseWidth / 20000.0) * 100.0;
    }
}
