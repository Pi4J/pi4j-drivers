package com.pi4j.drivers.sound;

import java.io.Closeable;

public interface SoundDriver extends Closeable {
    /**
     * Plays a sequence of tones.
     *
     * @param lengthMultiplierMillis The note lengths are multiplied wth this factor, resulting in the
     *                               note length in milliseconds.
     * @param callback Called when the end of the note sequence is reached.
     * @param notes An alternating sequence of frequencies and note lengths.
     *
     * @return A Sequence object that can be used to stop playing.
     */
    Sequence playNotes(int lengthMultiplierMillis, Runnable callback, float... notes);

    @Override
    void close();

    interface Sequence {
        void stop();
    }
}
