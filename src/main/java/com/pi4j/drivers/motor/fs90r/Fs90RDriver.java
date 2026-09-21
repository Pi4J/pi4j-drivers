package com.pi4j.drivers.motor.fs90r;


import com.pi4j.io.pwm.Pwm;

/**
 * Driver for a FS90R servo motor.
 * The Fs90R defines a specific PWM interface and exactly how the PWN signal
 * controls the servo
 *
 * https://www.pololu.com/product/2820
 *
 *  FS90R Duty Cycle Reference TableAssuming a standard 50Hz frequency
 *  (20ms total wave period),  here is how the pulse width and duty cycle
 *  map to direction and rotation speed for the FS90R Servo Motor:
 *  |  Direction Rotation       |  Pulse Width (us)  |    Duty Cycle (%)    |
 *  |-----------------------------------------------------------------------|
 *  |   clockwise max           |      0.5 us        |     2.5 %            |
 *  |   stopped                 |      1.5 us        |     7.5 %            |
 *  |   counter clockwise max   |      2.5 us        |     12.5 %           |
 *  |-----------------------------------------------------------------------|
 *
 *
 */


    public class Fs90RDriver {

    Pwm pwm;


    /**
     *
     * @param pwm  Hardware PWM
     */
    public Fs90RDriver(Pwm pwm){
        this.pwm = pwm;
    }

    /**
     *  Set  the servo output shaft direction os rotation and RPM.
     *  expressed in degrees
     * @param degree         0 ... 180
     */
    public void setServoRotation(double degree){
        pwm.on(degreeToDutyCycle(degree), 50);
    }

    /**
     *
     * @param degree   Desired location of the servo output shaft solved for required Duty Cycle.
     *                 See table above.
     * @return duty cycle
     */
    double degreeToDutyCycle(double degree){
        return (degree/180 * 10 ) + 2.5;
    }
}
