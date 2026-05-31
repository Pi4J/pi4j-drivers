package com.pi4j.drivers.sound;

import java.io.Closeable;

public interface SoundDriver extends Closeable {

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
