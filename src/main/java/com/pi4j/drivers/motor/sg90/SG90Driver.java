package com.pi4j.drivers.motor.sg90;


import com.pi4j.io.pwm.Pwm;


/*
* https://www.scribd.com/document/420327552/Servo
* https://www.friendlywire.com/projects/ne555-servo-safe/SG90-datasheet.pdf
*
* These devices do not have a customary DataSheet.  Most servo devices provide limited specifications
* for required pulse width, they just say 1 - 2 millisecond.
*
* The SG90 defines a specific PWM interface and exactly how the PWN signal
* controls the servo. There are many different manufacturers of the SG90 servo.
* The pulse width required for setting the servo at 0 degrees or 180 degrees varies by
* manufacturer. The driver by default uses the pulse width timing as
* (low)1000 - (high)2000 microseconds. The user of this example application can change these defaults
* when first invoking the program.
*
* If the servo does not rotate to the desired position, Example: to rotate closer to the 0 degree
* point reduce the -low argument, to rotate closer to the 180 degree point increase the
* -high argument. The -low -high change should be balanced or 90 degree will not properly align.
* Meaning if you reduce the -low by 200, you should increase the -high by 200, or visa-versa.
*
*
*/


public class SG90Driver {

    private final Pwm pwm;
    private float pwZero = 1000;
    private float pwOneEighty = 2000;
    private float totalPwRange = pwOneEighty - pwZero ;

    private static final int PWM_FREQUENCY = 50;

    /**
     *
     * @param pwm Hardware PWM
     */
    public SG90Driver(Pwm pwm) {
        this.pwm = pwm;
        if(pwm == null){
            throw new IllegalArgumentException("PWM is null");
        }
    }

    /**
     *
     * @param pwm           pwm device
     * @param pulseWidth0   value in microseconds for 0 degree position
     * @param pulseWidth180 value in microseconds for 180 degree position
     */
    public SG90Driver(Pwm pwm, float pulseWidth0, float pulseWidth180 ) {
        this.pwm = pwm;
        this.pwZero = pulseWidth0;
        this.pwOneEighty = pulseWidth180;
        this.totalPwRange =  this.pwOneEighty - this.pwZero ;

        if(pwm == null){
            throw new IllegalArgumentException("PWM is null");
        }
        if (pulseWidth0 < 500 || pulseWidth0 > 1500){
            throw new IllegalArgumentException("pulseWidth0 range 500 ... 1500 ");
        }
        if (pulseWidth180 < 1500 || pulseWidth180 > 2500){
            throw new IllegalArgumentException("pulseWidth180 range 1500 ... 2500 ");
        }
    }



    /**
     * Move the servo output shaft to the position
     * expressed in degrees
     *
     * @param degree 0 ... 180
     */
    public void setServoAngle(float degree) {
        pwm.on(degreeToDutyCycle(degree), PWM_FREQUENCY);
    }

    /**
     *
     * @param degree Desired location of the servo output shaft solved for required Duty Cycle.
     *               See table above.
     * @return duty cycle
     */
    float degreeToDutyCycle(float degree) {
        float targetPulseWidth  = this.pwZero + (degree * (totalPwRange / 180));
        return  (targetPulseWidth/ 20000)  * 100;
    }

}
