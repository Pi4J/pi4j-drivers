package com.pi4j.drivers.display.character;

public interface CharacterDisplay {
    int getWidth();
    int getHeight();

    void writeAt(int x, int y, String text);
}
