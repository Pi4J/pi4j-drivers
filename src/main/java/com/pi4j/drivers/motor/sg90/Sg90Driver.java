package com.pi4j.drivers.motor.sg90;

import com.pi4j.io.pwm.Pwm;


/*
 * Driver for a SG90 servo motor.
 * The SG90 defines a specific PWM interface and exactly how the PWM signal
 * controls the servo
 * <p>
 *  <a href="https://www.scribd.com/document/420327552/Servo"> Seervo Details</a>
 *  <a href="https://www.friendlywire.com/projects/ne555-servo-safe/SG90-datasheet.pdf"> Seervo Details</a>
 * </p>
 * These devices do not have a customary DataSheet.  Most servo devices provide limited specifications
 * for required pulse width, they just say 1 - 2 millisecond.
 *
 * The SG90 defines a specific PWM interface and exactly how the PWN signal
 * controls the servo. There are many different manufacturers of the SG90 servo.
 * The pulse width required for setting the servo at 0 degrees or 180 degrees varies by
 * manufacturer. The driver by default uses the pulse width timing as
 * pulseWidthMin 1000 - pulseWidthMax 2000 microseconds. The user of this example application can change these defaults
 * when first invoking the program.
 *
 * If the servo does not rotate to the desired position, Example: to rotate closer to the 0 degree
 * angle reduce the pulseWidthMin argument, to rotate closer to the 180 angle increase the
 * pulseWidthMax argument. The pulseWidthMin pulseWidthMax change should be balanced or 90 degree will not properly align.
 * Meaning if you reduce the pulseWidthMin by 200, you should increase the pulseWidthMax by 200, or visa-versa.
 *
 *
 */

public class Sg90Driver {

    public static final int PWM_FREQUENCY = 50;

    private final Pwm pwm;

    private static final double PULSE_WIDTH_MIN = 1000.0;
    private static final double PULSE_WIDTH_MAX = 2000.0;
    /* these are the pulse width times for the extreme positions clockwise and counterclockwise.
     These values and their range are used in calculating the PulseWidth  (@see setAngle())
      */
    private double pulseWidthMin;
    private double pulseWidthMax;
    private double totalPulseWidthRange;

    /**
     *
     * @param pwm Hardware PWM
     */
    public Sg90Driver(Pwm pwm) {
        this(pwm, PULSE_WIDTH_MIN, PULSE_WIDTH_MAX);
        if (pwm == null) {
            throw new IllegalArgumentException("PWM is null");
        }
    }

    /**
     *
     * @param pwm           pwm device
     * @param pulseWidthMin value in microseconds for 0 degree position
     * @param pulseWidthMax value in microseconds for 180 degree position
     */
    public Sg90Driver(Pwm pwm, double pulseWidthMin, double pulseWidthMax) {
        this.pwm = pwm;
        this.pulseWidthMin = pulseWidthMin;
        this.pulseWidthMax = pulseWidthMax;
        this.totalPulseWidthRange = this.pulseWidthMax - this.pulseWidthMin;

        if (pwm == null) {
            throw new IllegalArgumentException("PWM is null");
        }
        if (!Double.isFinite(pulseWidthMin) || pulseWidthMin < 500.0 || pulseWidthMin > 1500.0) {
            throw new IllegalArgumentException("PULSE_WIDTH_MIN range 500 ... 1500 ");
        }
        if (!Double.isFinite(pulseWidthMax) || pulseWidthMax < 1500.0 || pulseWidthMax > 2500.0) {
            throw new IllegalArgumentException("pulseWidthMax range 1500 ... 2500 ");
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
     * Move the servo output shaft to the position
     * expressed in degrees
     *
     * @param degree 0 ... 180
     */
    public void setAngle(double degree) {
        pwm.on(degreeToDutyCycle(degree), PWM_FREQUENCY);
    }

    /**
     *
     * @param degree Degree value used to set armature position.  This is converted to
     *               pulse width.
     * @return duty cycle
     */
    double degreeToDutyCycle(double degree) {
        double targetPulseWidth = this.pulseWidthMin + (degree / 180.0 * totalPulseWidthRange);
        return (targetPulseWidth / 20000.0) * 100.0;
    }

}
