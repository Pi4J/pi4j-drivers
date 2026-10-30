package com.pi4j.drivers.motor.fs90r;

import com.pi4j.io.pwm.Pwm;

/**
 * Driver for a FS90R servo motor.
 * The Fs90R defines a specific PWM interface and exactly how the PWM signal
 * controls the servo
 * <p>
 * https://www.pololu.com/product/2820
 * <p>
 * <p>
 * These devices do not have a customary DataSheet.  Most servo devices provide limited specifications
 * for required pulse width, they just say 1 - 2 millisecond.
 * <p>
 * The FS90 R 360 defines a specific PWM interface and exactly how the PWN signal
 * controls the servo. There are many different manufacturers of the FS90 servo.
 * A command of 0 produces maximum clockwise (CW) rotation, while 180 produces maximum
 * counterclockwise (CCW) rotation; the exact pulse widths vary by manufacturer.
 * (low)1000 - (high)2000 microseconds. The user of this example application can change these defaults
 * when first invoking the program.
 * <p>
 * If the servo does not rotate to the expected RPM,Example to rotate faster CW  reduce the -low
 * argument, to rotate CCW faster increase the high argument. The low high change should be balanced or 90 degree will not properly align.
 * Meaning if you reduce the low by 200, you should increase the high by 200, or vice versa.
 *
 *
 */

public class Fs90rDriver {

    public static final double PW_MIN_CW = 1000.0;
    public static final double PW_MAX_CCW = 2000.0;
    public static final int PWM_FREQUENCY = 50;

    public enum ServoDirection {
        UNSET,
        CW,
        CCW
    }

    private ServoDirection direction;
    private boolean rotating;
    private final Pwm pwm;
    private double pwMin;
    private double pwMax;
    private double totalPwRange;

    /**
     *
     * @param pwm Hardware PWM
     */
    public Fs90rDriver(Pwm pwm) {
        this(pwm, PW_MIN_CW, PW_MAX_CCW);
    }

    /**
     *
     * @param pwm   pwm device
     * @param pwMin value in microseconds for 0 degree position
     * @param pwMax value in microseconds for 180 degree position
     */
    public Fs90rDriver(Pwm pwm, double pwMin, double pwMax) {
        this.pwm = pwm;
        this.pwMin = pwMin;
        this.pwMax = pwMax;
        this.totalPwRange = this.pwMax - this.pwMin;
        this.direction = ServoDirection.UNSET;
        if (pwm == null) {
            throw new IllegalArgumentException("PWM is null");
        }
        if (!Double.isFinite(pwMin) || pwMin < 500.0 || pwMin > 1500.0) {
            throw new IllegalArgumentException("pwMin range 500.0 ... 1500.0 ");
        }
        if (!Double.isFinite(pwMax) || pwMax < 1500.0 || pwMax > 2500.0) {
            throw new IllegalArgumentException("pwMax range 1500.0 ... 2500.0 ");
        }
        if (pwMin >= pwMax) {
            throw new IllegalArgumentException("pwMin must be less than pwMax");
        }
    }

    public void setPwMin(double pwMin) {
        this.pwMin = pwMin;
        this.totalPwRange = this.pwMax - this.pwMin;
    }

    public void setPwMax(double pwMax) {
        this.pwMax = pwMax;
        this.totalPwRange = this.pwMax - this.pwMin;
    }

    /**
     * Setting rotation by degree assumes the user understands the spec
     * and does NOT test the servo being stopped nor that the degree range
     * coincides with the present direction of rotation.
     * <p>
     * Sets the servo rotation command: 0 is maximum clockwise, 90 is neutral,
     * and 180 is maximum counterclockwise.
     *
     * @param degree rotation command from 0 through 180
     */
    public void setServoRotationByDegree(double degree) {

        if (!Double.isFinite(degree) || degree < 0.0 || degree > 180.0) {
            throw new IllegalArgumentException("degree must be finite and between 0.0 and 180.0");
        }
        rotating = true;
        if (degree < 90.0) {
            direction = ServoDirection.CW;
        } else if (degree > 90.0) {
            direction = ServoDirection.CCW;
        } else {
            direction = ServoDirection.UNSET;
            rotating = false;
        }
        pwm.on(degreeToDutyCycle(degree), PWM_FREQUENCY);
    }

    /**
     * Control direction of rotation
     * Servo must be stopped before direction can be changed
     *
     * @param direction
     */
    public void setRotationDirection(ServoDirection direction) {

        if (rotating && direction != this.direction) {
            throw new IllegalStateException("Cannot set new direction without stopping rotation");
        }
        this.direction = direction;
    }

    /**
     * Sets the servo rotation CCW percentage of max: 0% to 100% ,
     *
     * @param percent rotation command from 0 through 100
     */
    public void setServoRotationByPercent(double percent) {

        if (!Double.isFinite(percent) || percent < 0.0 || percent > 100.0) {
            throw new IllegalArgumentException("percentage must be finite and between 0.0% and 100.0%");
        }
        if (direction == ServoDirection.UNSET) {
            throw new IllegalArgumentException("percent control not possible, rotation direction not set");
        }

        double degree;

        this.rotating = percent != 0.0;

        if (direction == ServoDirection.CCW) {
            degree = 90.0 + (90.0 * percent / 100);
        } else {
            degree = 90.0 - (90.0 * percent / 100);
        }

        pwm.on(degreeToDutyCycle(degree), PWM_FREQUENCY);
    }

    /**
     *
     * @param degree Desired location of the servo output shaft solved for required Duty Cycle.
     *               See table above.
     * @return duty cycle
     */
    double degreeToDutyCycle(double degree) {
        double targetPulseWidth = this.pwMin + (degree / 180 * totalPwRange);
        return (targetPulseWidth / 20000.0) * 100.0;
    }
}
