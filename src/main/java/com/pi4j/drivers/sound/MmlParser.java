package com.pi4j.drivers.sound;

import java.util.Arrays;

/**
 * Parser for the Music Macro Language format, see https://en.wikipedia.org/wiki/Music_Macro_Language#Modern_MML
 * or https://mml-guide.readthedocs.io/
 */
public class MmlParser {

    /**
     * Converts the given mml string to an interleaved array of frequencies and durations in milliseconds.
     * Unrecognized codes are skipped.
     */
    public static float[] parse(String mml) {
        float[] result = new float[16];
        int rPos = 0;
        int pos = 0;
        int len = mml.length();
        int octave = 4;
        int defaultLength = 4;
        int tempoBpm = 120;

        while (pos < len) {
            char c = Character.toLowerCase(mml.charAt(pos++));
            if (c == '<') {
                octave--;
                continue;
            }
            if (c == '>') {
                octave++;
                continue;
            }
            if (pos < len) {
                char d = mml.charAt(pos);
                switch (d) {
                    case '#', '+' -> {
                        c = Character.toUpperCase(c);
                        pos++;
                    }
                    case '-' -> {
                        c = Character.toUpperCase((char) (c - 1));
                        pos++;
                    }
                }
            }
            int n = 1;
            boolean useDefault = true;
            while (pos < len) {
                char d = mml.charAt(pos);
                if (d < '0' || d > '9') {
                    break;
                }
                pos++;
                useDefault = false;
                n = n * 10 + (d - '0');
            }
            boolean dotted = pos < len && mml.charAt(pos) == '.';
            if (dotted) {
                pos++;
            }
            float frequency;
            switch (c) {
                case 'c' -> frequency = 261.63f;
                case 'C' -> frequency = 277.18f;
                case 'd' -> frequency = 293.66f;
                case 'D' -> frequency = 311.13f;
                case 'e' -> frequency = 329.63f;
                case 'f' -> frequency = 349.23f;
                case 'F' -> frequency = 369.99f;
                case 'g' -> frequency = 392;
                case 'G' -> frequency = 415.3f;
                case 'a' -> frequency = 440;
                case 'A' -> frequency = 466.16f;
                case 'b' -> frequency = 493.88f;
                case 'p', 'r' -> frequency = 0;
                case 'o' -> {
                    octave = n;
                    continue;
                }
                case 't' -> {
                    tempoBpm = n;
                    continue;
                }
                case 'l' -> {
                    defaultLength = n;
                    continue;
                }
                default -> {
                    continue;
                }
            }
            float length = ((dotted ? 1.5f : 1f) * 4 * 60_000f / tempoBpm) / (useDefault ? defaultLength : n);
            frequency *= Math.pow(2, octave - 4);

            if (rPos + 1 >= result.length) {
                result = Arrays.copyOf(result, result.length * 2);
            }

            result[rPos++] = frequency;
            result[rPos++] = length;
        }

        return Arrays.copyOf(result, rPos);
    }

}