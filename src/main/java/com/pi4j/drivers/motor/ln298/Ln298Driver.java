package com.pi4j.drivers.motor.ln298;

import com.pi4j.io.OnOffWrite;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.util.Delay;

/**
 * A driver for controlling a single motor with a LN298 chip.
 * To control both motors, please use two instances of this class.
 */
public class Ln298Driver {
    private final OnOffWrite<?> forwardPin;
    private final OnOffWrite<?> backwardsPin;
    private final Pwm speedPwm;
    private final Delay delay = new Delay();

    private double minDutyCycle = 0;
    private double maxDutyCycle = 100;
    private double currentSpeed = 0;
    private double startBoostSpeed = 0;
    private int startBoostDurationMs = 0;
    private boolean reverse = false;

    /**
     * Creates a LN298 driver instance that controls the 3 input pins for a single motor, translating the desired
     * speed in the range 0 (stopped) to 100 (full speed forward) to the right state of the three
     * given pins.
     * <p>
     * The speedPwm can be left null if the corresponding ENA or ENB pin is bridged (might be useful for testing
     * purposes). In this case, the speed effectively will be 0 for 0 and 100 for positive values.
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
     * Sets a power boost for the motor, starting with the given duty cycle for the given duration when
     * the motor is started from a speed of 0.
     */
    public void setStartBoost(int dutyCycle, int durationMs) {
        this.startBoostSpeed = dutyCycle;
        this.startBoostDurationMs = durationMs;
    }

    /** Sets the minium duty cycle for the motor, i.e. the duty cycle when the speed is set to 0 +/- epsilon. */
    public void setMinDutyCycle(double value) {
        if (value < 0 || value >= maxDutyCycle) {
            throw new IllegalArgumentException("Min duty cycle must be between 0 and " + maxDutyCycle + "; was: " + value);
        }
        this.minDutyCycle = value;
    }

    /** Sets the maximum duty cycle for the motor, i.e. the duty cycle when the speed is set to +/-100. */
    public void setMaxDutyCycle(double value) {
        if (value <= minDutyCycle || value > 100) {
            throw new IllegalArgumentException("Max duty cycle must be between " + minDutyCycle + " and 100; was: " + value);
        }
        this.maxDutyCycle = value;
    }

    /** Sets the motor to run in reverse. This can only be changed when the current speed is 0. */
    public void setReverse(boolean reverse) {
        if (currentSpeed != 0 && reverse != this.reverse) {
            throw new IllegalStateException("Cannot set reverse while motor is running");
        }
        this.reverse = reverse;
    }

    /**
     * Sets the speed of the motor in the range from 0 to 100, translating to a
     * duty cycle between minDutyCycle and maxDutyCycle.
     * <p>
     * If the speed is 0, the motor will be stopped.
     * <p>
     * This method will be blocking if the motor is started and a start boost is set.
     */
    public void setSpeed(double speed) {
        if (speed < 0 || speed > 100) {
            throw new IllegalArgumentException("Speed must be between 0 and 100; was: " + speed);
        }
        if (speed == 0) {
            forwardPin.setState(false);
            backwardsPin.setState(false);
            if (speedPwm != null) {
                speedPwm.off();
            }
        } else {
            if (currentSpeed == 0) {
                setDutyCycle(startBoostSpeed);
                delay.setMillis(startBoostDurationMs).materialize();
            }
            setDutyCycle((int) (minDutyCycle + speed / 100.0 * (maxDutyCycle - minDutyCycle)));
        }
        currentSpeed = speed;
    }

    private void setDutyCycle(double dutyCycle) {
        if (speedPwm != null) {
            speedPwm.on((int) Math.round(dutyCycle));
        }
        forwardPin.setState(!reverse);
        backwardsPin.setState(reverse);
    }

}
