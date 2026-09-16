package com.pi4j.drivers.motor.sg90;


import com.pi4j.io.pwm.Pwm;

/**
 * Driver for a SG90 servo motor.
 * The SG90 defines a specific PWM interface and exactly how the PWN signal
 * controls the servo
 *
 *  https://www.friendlywire.com/projects/ne555-servo-safe/SG90-datasheet.pdf
 *
 *  SG90 Duty Cycle Reference TableAssuming a standard 50Hz frequency (20ms total wave period),
 *  here is how the pulse width and duty cycle map to standard angles for the SG90 Servo Motor Guide:
 *  |  Angle (Degrees)  |  Pulse Width (us)     |        Duty Cycle (%)     |
 *  |-----------------------------------------------------------------------|
 *  |     0             |      0.5 us           |         2.5 %             |
 *  |     45            |      1.5 us           |         7.5 %             |
 *  |     90            |      2.5 us           |         12.5 %            |
 *  |-----------------------------------------------------------------------|
 *
 *
 */


public class SG90Driver {
    static final private double DEFAULT_PW_180 = 2.5;

    Pwm pwm;

    /* The servo spec should define the pulse width for 180 degree.
    When creating the driver, the parm value oneEighty will be used in
    calculating the appropriate percentage
     */
    double pw180 ;


    /**
     *  Default value for oneEighty will be used.
     * @param pwm  Hardware PWM
     */
    public SG90Driver(Pwm pwm){
        this(pwm, DEFAULT_PW_180);
    }
    /**
     *
     * @param pwm  Hardware PWM
     * @param oneEighty  servo 180 degree pulse width
     */
    public SG90Driver(Pwm pwm, double oneEighty){
        this.pwm = pwm;
        this.pw180 = oneEighty;
    }

    /**
     *  Move the servo output shaft to the position
     *  expressed in degrees
     * @param degree         0 ... 180
     */
    public void setServoAngle(double degree){
    pwm.on(degreeToDutyCycle(degree), 50);
}

    /**
     *
     * @param degree   Desired location of the servo output shaft solved for required Duty Cycle.
     *                 See table above.
     * @return duty cycle
     */
    double degreeToDutyCycle(double degree){
        return (degree/180 * 10 ) + pw180;
    }
}
