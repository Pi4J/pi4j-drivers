package com.pi4j.drivers.motor.fs90r;


import com.pi4j.io.pwm.Pwm;

/**
* Driver for a FS90R servo motor.
* The Fs90R defines a specific PWM interface and exactly how the PWN signal
* controls the servo
*
* https://www.pololu.com/product/2820
*
*
*   These devices do not have a customary DataSheet.  Most servo devices provide limited specifications
* for required pulse width, they just say 1 - 2 millisecond.
*
* The FS90 R 360 defines a specific PWM interface and exactly how the PWN signal
* controls the servo. There are many different manufacturers of the FS90 servo.
* The pulse width required for setting the servo at 0 degrees MAX Clockwise (CW) or 180 degrees
* MAX Countrer ClockWise (CCW) varies by manufacturer. The driver by default uses the pulse width timing as
* (low)1000 - (high)2000 microseconds. The user of this example application can change these defaults
* when first invoking the program.
*
* If the servo does not rotate to the expected RPM,Example to rotate faster CW  reduce the -low
*  argument, to rotate CCW faster increase the -high argument. The -low -high change should be balanced or 90 degree will not properly align.
* Meaning if you reduce the -low by 200, you should increase the -high by 200, or visa-versa.
*
*
*/


    public class Fs90RDriver {

    private final Pwm pwm;
    private float pwZero = 1000;
    private float pwOneEighty = 2000;
    private float totalPwRange = pwOneEighty - pwZero ;

    private static final int PWM_FREQUENCY = 50;


    /**
     *
     * @param pwm  Hardware PWM
     */
    public Fs90RDriver(Pwm pwm) {
        this.pwm = pwm;
        if (pwm == null) {
            throw new IllegalArgumentException("PWM is null");
        }
    }

        /**
         *
         * @param pwm           pwm device
         * @param pulseWidth0   value in microseconds for 0 degree position
         * @param pulseWidth180 value in microseconds for 180 degree position
         */
    public Fs90RDriver(Pwm pwm, float pulseWidth0, float pulseWidth180 ) {
            this.pwm = pwm;
            this.pwZero = pulseWidth0;
            this.pwOneEighty = pulseWidth180;
            this.totalPwRange =  this.pwOneEighty - this.pwZero ;

            if(pwm == null){
                throw new IllegalArgumentException("PWM is null");
            }
            if (!Float.isFinite(pulseWidth0) || pulseWidth0 < 500 || pulseWidth0 > 1500){
                throw new IllegalArgumentException("pulseWidth0 range 500 ... 1500 ");
            }
            if (!Float.isFinite(pulseWidth0) || pulseWidth180 < 1500 || pulseWidth180 > 2500){
                throw new IllegalArgumentException("pulseWidth180 range 1500 ... 2500 ");
            }
            if (pulseWidth0 >= pulseWidth180) {
                throw new IllegalArgumentException("pulseWidth0 must be less than pulseWidth180");
            }
        }


    /**
     *  Set  the servo output shaft direction os rotation and RPM.
     *  expressed in degrees
     * @param degree         0 ... 180
     */
    public void setServoRotation(float degree){

        if (!Float.isFinite(degree) || degree < 0 || degree > 180) {
            throw new IllegalArgumentException("degree must be finite and between 0 and 180");
        }
        pwm.on(degreeToDutyCycle(degree), PWM_FREQUENCY);
    }

    /**
     *
     * @param degree   Desired location of the servo output shaft solved for required Duty Cycle.
     *                 See table above.
     * @return duty cycle
     */
    double degreeToDutyCycle(float degree){
        float targetPulseWidth  = this.pwZero + (degree * (totalPwRange / 180));
        return  (targetPulseWidth/ 20000)  * 100;
    }
}
