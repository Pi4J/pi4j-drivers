package com.pi4j.drivers.input;

public interface KeyPad {
    char NONE = '\0';

    /** Returns the character code the key currently pressed; 0 if none. */
    char getKey();
}
