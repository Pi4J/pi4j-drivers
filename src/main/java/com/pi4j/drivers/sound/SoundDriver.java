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
    @Deprecated
    default Sequence playNotes(int lengthMultiplierMillis, Runnable callback, double... notes) {
        double[] multiplied;
        if (lengthMultiplierMillis == 1) {
            multiplied = notes;
        } else {
            multiplied = new double[lengthMultiplierMillis];
            for (int i = 1; i < notes.length; i += 2) {
                multiplied[i] = notes[i] * lengthMultiplierMillis;
            }
        }
        return playNotes(multiplied).setCallback(callback);
    }

    /**
     * Play the given interleaved sequence of tone frequencies (Hz) and durations (ms).
     * The returned sequence object can be used to request a notification when playing has ended or
     * to stop playback.
     */
    Sequence playNotes(double... notes);

    /** Convenience method for playing mml */
    default Sequence playNotes(String mml) {
        return playNotes(MmlParser.parse(mml));
    }

    @Override
    void close();

    interface Sequence {
        /** Stop playing */
        void stop();

        /** Called after playing the notes. Not called when playing was stopped. */
        Sequence setCallback(Runnable callback);
    }
}
