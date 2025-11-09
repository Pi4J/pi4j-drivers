package com.pi4j.drivers.sound;

import com.pi4j.io.pwm.Pwm;

import java.util.Timer;
import java.util.TimerTask;

/** PWM-Based tone generator */
public class PwmSoundDriver implements SoundDriver {
    private final Pwm pwm;
    private final Timer timer = new Timer();
    private final Object lock = new Object();

    public PwmSoundDriver(Pwm pwm) {
        this.pwm = pwm;
    }

    @Override
    public Sequence playNotes(int lengthMultiplierMillis, Runnable callback, float... notes) {
        SequenceImpl result = new SequenceImpl(lengthMultiplierMillis, callback, notes);
        result.playNext();
        return result;
    }

    @Override
    public void close() {
        pwm.close();
    }

    private class SequenceImpl implements Sequence {
        private final int lengthMultiplierMillis;
        private final Runnable callback;
        private final float[] notes;
        private int index = 0;

        private SequenceImpl(int lengthMultiplierMillis, Runnable callback, float... notes) {
            this.lengthMultiplierMillis = lengthMultiplierMillis;
            this.callback = callback;
            this.notes = notes;
        }

        @Override
        public void stop() {
            synchronized (lock) {
                pwm.off();
                index = Integer.MAX_VALUE;
            }
        }

        private void playNext() {
            boolean triggerCallback = false;
            synchronized (lock) {
                if (index >= notes.length) {
                    pwm.off();
                    triggerCallback = callback != null && index != Integer.MAX_VALUE;
                } else {
                    int frequency = Math.round(notes[index++]);
                    if (frequency == 0) {
                        pwm.off();
                    } else {
                        pwm.on(50, frequency);
                    }
                    int length = Math.round(notes[index++] * lengthMultiplierMillis);
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            synchronized (lock) {
                                pwm.off();
                            }
                            playNext();
                        }
                    }, length);
                }
            }
            if (triggerCallback) {
                callback.run();
            }
        }
    }
}
