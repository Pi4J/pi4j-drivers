package com.pi4j.drivers.motor.ln298;

import com.pi4j.io.OnOffWrite;
import com.pi4j.io.pwm.Pwm;

/**
 * A driver for controlling a single motor with a LN298 chip.
 * To control both motors, please use two instances of this class.
 */
public class Ln298Driver {
    private final OnOffWrite<?> forwardPin;
    private final OnOffWrite<?> backwardsPin;
    private final Pwm speedPwm;

    /**
     * Creates a LN298 driver instance that controls the 3 input pins for a single motor, translating the desired
     * speed in the range -100 (full speed backwards) to 100 (full speed forward) to the right state of the three
     * given pins.
     *
     * @param forwardPin The DigitalOutput controlling the pin enabling forward motion; IN1 for motor A and IN3 for motor B.
     * @param backwardsPin The DigitalOutput controlling the pin enabling backwards motion; IN2 for motor A and IN4 for motor B.
     * @param speedPwm The PWM output controlling the speed of the motor; connected to ENA for Motor A or ENB for motor B.
     */
    public Ln298Driver(OnOffWrite<?> forwardPin, OnOffWrite<?> backwardsPin, Pwm speedPwm) {
        this.forwardPin = forwardPin;
        this.backwardsPin = backwardsPin;
        this.speedPwm = speedPwm;
    }

    /**
     * Sets the speed of the motor in the range from -100 to 100. Setting the speed to 0 stops the motor.
     * Negative values spin backwards, positive values spin forward.
     */
    public void setSpeed(double speed) {
        if (speed < -100 || speed > 100) {
            throw new IllegalArgumentException("Speed must be between -100 and 100; was: " + speed);
        }
        if (speed == 0) {
            forwardPin.setState(false);
            backwardsPin.setState(false);
            speedPwm.setState(false);
        } else if (speed > 0) {
            forwardPin.setState(true);
            backwardsPin.setState(false);
            // TODO: Remove rounding when we link to Pi4j 5.0
            speedPwm.setDutyCycle((int) Math.round(speed));
        } else {
            forwardPin.setState(false);
            backwardsPin.setState(true);
            // TODO: Remove rounding when we link to Pi4j 5.0
            speedPwm.setDutyCycle(-(int) Math.round(speed));
        }
    }

}
