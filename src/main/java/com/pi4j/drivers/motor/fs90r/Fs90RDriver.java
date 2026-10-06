package com.pi4j.drivers.motor.fs90r;


import com.pi4j.io.pwm.Pwm;

/**
* Driver for a FS90R servo motor.
* The Fs90R defines a specific PWM interface and exactly how the PWM signal
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
* A command of 0 produces maximum clockwise (CW) rotation, while 180 produces maximum
* counterclockwise (CCW) rotation; the exact pulse widths vary by manufacturer.
* (low)1000 - (high)2000 microseconds. The user of this example application can change these defaults
* when first invoking the program.
*
* If the servo does not rotate to the expected RPM,Example to rotate faster CW  reduce the -low
*  argument, to rotate CCW faster increase the -high argument. The -low -high change should be balanced or 90 degree will not properly align.
* Meaning if you reduce the -low by 200, you should increase the -high by 200, or vice versa.
*
*
*/


public class Fs90RDriver {

    private static final class Constants {
        static final double PW_ZERO_DEFAULT = 1000.0;
        static final double PW_ONE_EIGHTY_DEFAULT = 2000.0;
        static final int PWM_FREQUENCY = 50;
    }

    private final Pwm pwm;
    private double pwZero ;
    private double pwOneEighty ;
    private double totalPwRange ;



    /**
     *
     * @param pwm  Hardware PWM
     */
    public Fs90RDriver(Pwm pwm) {
        this(pwm,Constants.PW_ZERO_DEFAULT, Constants.PW_ONE_EIGHTY_DEFAULT);
    }

        /**
         *
         * @param pwm           pwm device
         * @param pwZero   value in microseconds for 0 degree position
         * @param pwOneEighty value in microseconds for 180 degree position
         */
    public Fs90RDriver(Pwm pwm, double pwZero, double pwOneEighty ) {
            this.pwm = pwm;
            this.pwZero = pwZero;
            this.pwOneEighty = pwOneEighty;
            this.totalPwRange =  this.pwOneEighty - this.pwZero ;

            if(pwm == null){
                throw new IllegalArgumentException("PWM is null");
            }
            if (!Double.isFinite(pwZero) || pwZero < 500.0 || pwZero > 1500.0){
                throw new IllegalArgumentException("pwZero range 500.0 ... 1500.0 ");
            }
            if (!Double.isFinite(pwOneEighty) || pwOneEighty < 1500.0 || pwOneEighty > 2500.0){
                throw new IllegalArgumentException("pwOneEighty range 1500.0 ... 2500.0 ");
            }
            if (pwZero >= pwOneEighty) {
                throw new IllegalArgumentException("pwZero must be less than pwOneEighty");
            }
        }


    /**
     *  Set  the servo output shaft direction os rotation and RPM.
     *  expressed in degrees
     * @param degree         0 ... 180
     */
    public void setServoRotation(double degree){

        if (!Double.isFinite(degree) || degree < 0.0 || degree > 180.0) {
            throw new IllegalArgumentException("degree must be finite and between 0.0 and 180.0");
        }
        pwm.on(degreeToDutyCycle(degree), Constants.PWM_FREQUENCY);
    }

    /**
     *
     * @param degree   Desired location of the servo output shaft solved for required Duty Cycle.
     *                 See table above.
     * @return duty cycle
     */
    double degreeToDutyCycle(double degree){
        double targetPulseWidth  = this.pwZero + (degree * (totalPwRange / 180.0));
        return  (targetPulseWidth/ 20000.0)  * 100.0;
    }
}
