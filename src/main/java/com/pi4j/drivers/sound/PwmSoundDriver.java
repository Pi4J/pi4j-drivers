package com.pi4j.drivers.sound;

import com.pi4j.io.pwm.Pwm;

/** Deprecated; please use the super class instead. */
@Deprecated
public class PwmSoundDriver extends com.pi4j.drivers.sound.pwm.PwmSoundDriver {
    public PwmSoundDriver(Pwm pwm) {
        super(pwm);
    }
}
