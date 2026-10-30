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


public class Fs90rDriver {

    public static final double PW_MIN_CW = 1000.0;
    public static final double PW_MAX_CCW = 2000.0;
    public static final int PWM_FREQUENCY = 50;

    private final Pwm pwm;
    private double pwCwMin ;
    private double pwCcwMax ;
    private double totalPwRange ;



    /**
     *
     * @param pwm  Hardware PWM
     */
    public Fs90rDriver(Pwm pwm) {
        this(pwm,PW_MIN_CW, PW_MAX_CCW);
    }


    /**
         *
         * @param pwm           pwm device
         * @param pwCwMin   value in microseconds for 0 degree position
         * @param pwCcwMax value in microseconds for 180 degree position
         */
    public Fs90rDriver(Pwm pwm, double pwCwMin, double pwCcwMax ) {
            this.pwm = pwm;
            this.pwCwMin = pwCwMin;
            this.pwCcwMax = pwCcwMax;
            this.totalPwRange =  this.pwCcwMax - this.pwCwMin ;

            if(pwm == null){
                throw new IllegalArgumentException("PWM is null");
            }
            if (!Double.isFinite(pwCwMin) || pwCwMin < 500.0 || pwCwMin > 1500.0){
                throw new IllegalArgumentException("pwCwMax range 500.0 ... 1500.0 ");
            }
            if (!Double.isFinite(pwCcwMax) || pwCcwMax < 1500.0 || pwCcwMax > 2500.0){
                throw new IllegalArgumentException("pwCcwMax range 1500.0 ... 2500.0 ");
            }
            if (pwCwMin >= pwCcwMax) {
                throw new IllegalArgumentException("pwCwMax must be less than pwCcwMax");
            }
        }


    public void setPwCwMin(double pwCwMin) {
        this.pwCwMin = pwCwMin;
        this.totalPwRange =  this.pwCcwMax - this.pwCwMin ;
    }

    public void setPwCcwMax(double pwCcwMax) {
        this.pwCcwMax = pwCcwMax;
        this.totalPwRange =  this.pwCcwMax - this.pwCwMin ;
    }

    /**
     * Sets the servo rotation command: 0 is maximum clockwise, 90 is neutral,
     * and 180 is maximum counterclockwise.
     * @param degree rotation command from 0 through 180
     */
    public void setServoRotationByDegree(double degree){

        if (!Double.isFinite(degree) || degree < 0.0 || degree > 180.0) {
            throw new IllegalArgumentException("degree must be finite and between 0.0 and 180.0");
        }
        pwm.on(degreeToDutyCycle(degree), PWM_FREQUENCY);
    }

    /**
     * Sets the servo rotation CCW percentage of max: 0% to 100% ,
     * and 180 is maximum counterclockwise.
     * Degree 90-- 180
     * @param percent rotation command from 0 through 100
     */
    public void setServoRotationCCwByPercent(double percent){

        if (!Double.isFinite(percent) || percent < 0.0 || percent > 100.0) {
            throw new IllegalArgumentException("percentage must be finite and between 0.0% and 100.0%");
        }
        double degree = 90.0 + (90.0 * percent/100) ;
        pwm.on(degreeToDutyCycle(degree), PWM_FREQUENCY);
    }

    /**
     * Sets the servo rotation CW percentage of max: 0% to 100% ,
     * and 180 is maximum counterclockwise.
     * Degree 0 -- 90
     * @param percent rotation command from 0 through 100
     */
    public void setServoRotationCwByPercent(double percent){

        if (!Double.isFinite(percent) || percent < 0.0 || percent > 100.0) {
            throw new IllegalArgumentException("percentage must be finite and between 0.0% and 100.0%");
        }
        double degree = 90.0 - (90.0 * percent/100) ;
        pwm.on(degreeToDutyCycle(degree), PWM_FREQUENCY);
    }


    /**
     *
     * @param degree   Desired location of the servo output shaft solved for required Duty Cycle.
     *                 See table above.
     * @return duty cycle
     */
    double degreeToDutyCycle(double degree){
        double targetPulseWidth  = this.pwCwMin + (degree * (totalPwRange / 180.0));
        return  (targetPulseWidth/ 20000.0)  * 100.0;
    }
}
