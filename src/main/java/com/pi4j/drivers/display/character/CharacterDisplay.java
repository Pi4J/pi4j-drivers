package com.pi4j.drivers.display.character;

import java.util.EnumSet;

public interface CharacterDisplay {
    int getWidth();
    int getHeight();

    void clear();

    default EnumSet<Attribute> getSupportedAttributes() {
        return EnumSet.noneOf(Attribute.class);
    }

    default void writeAt(int x, int y, String text) {
        writeAt(x, y, text, EnumSet.noneOf(Attribute.class));
    }
    
    /**
     * Writes at text at the given position with the given attributes. Fractional values for the x position
     * can be used for soft scrolling on displays that support this.
     */
    void writeAt(float x, int y, String text, EnumSet<Attribute> attributes);

    enum Attribute {
        INVERSE
    }

}
