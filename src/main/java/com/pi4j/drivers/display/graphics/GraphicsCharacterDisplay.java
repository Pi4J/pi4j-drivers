package com.pi4j.drivers.display.graphics;

import com.pi4j.drivers.display.BitmapFont;
import com.pi4j.drivers.display.character.CharacterDisplay;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Implements the CharacterDisplay interface on top of a graphics display. Useful for running code that's designed
 * for text display with a graphics display.
 */
public class GraphicsCharacterDisplay implements CharacterDisplay {
    private final GraphicsDisplay display;
    private final Graphics graphics;

    private int foregroundColor;
    private int backgroundColor;
    private int originX;
    private int originY;
    private int widthPx;
    private int heightPx;
    private int leading;
    private int letterSpacing = 0;
    private List<List<CharacterCell>> screenBuffer = new ArrayList<>();
    private boolean cursorEnabled;
    private int cursorX;
    private int cursorY;

    /**
     * Creates a character display for the given graphics display, using the full display by default. This
     * can be refined as needed using the setters.
     */
    public GraphicsCharacterDisplay(GraphicsDisplay display) {
        this (display,
                display.getHeight() > 128 ? BitmapFont.get5x10Font() : BitmapFont.get5x8Font(),
                0xffffffff,
                0xff000000,
                Math.max(display.getHeight() / 80, 1));
    }

    /**
     * Creates a character display for the given graphics display, using the full display by default, using
     * the given font, colors and scale. Properties can be further refined using the setters on this class.
     */
    public GraphicsCharacterDisplay(
            GraphicsDisplay display,
            BitmapFont font,
            int foregroundColor,
            int backgroundColor,
            int scale) {
        this.display = display;
        this.foregroundColor = foregroundColor;
        this.backgroundColor = backgroundColor;
        this.graphics = display.getGraphics();
        this.graphics.setFont(font);
        this.graphics.setTextScale(scale);
        this.widthPx = display.getWidth();
        this.heightPx = display.getHeight();
    }

    // Additional public API, managing the properties of this character text display relative to the overall
    // graphics display.

    /** Sets the origin of this text display inside the graphics display it's rendered to */
    public void setOrigin(int originX, int originY) {
        this.originX = originX;
        this.originY = originY;
        graphics.setClip(originX, originY, widthPx, heightPx);
    }

    /**
     * Sets the size of this text display inside the graphics display it's rendered to.
     * Clipping will be applied to enforce this size.
     */
    public void setSizePx(int widthPx, int heightPx) {
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        graphics.setClip(originX, originY, widthPx, heightPx);
    }

    public void setFont(BitmapFont font) {
        this.graphics.setFont(font);
    }

    /** Sets the text scaling factors. */
    public void setTextScale(int textScaleX, int textScaleY) {
        this.graphics.setTextScale(textScaleX, textScaleY);
    }

    /** Sets the extra vertical distance between two lines of text */
    public void setLeading(int value) {
        this.leading = value;
    }

    /** Sets the additional horizontal space between two characters (excluding the space included by default). */
    public void setLetterSpacing(int value) {
        this.letterSpacing = value;
    }

    // Implementation of the CharacterDisplay API

    @Override
    public EnumSet<Attribute> getSupportedAttributes() {
        return EnumSet.of(Attribute.INVERSE);
    }

    /** Returns the number of fully visible columns that fit into this display */
    @Override
    public int getWidth() {
        return widthPx / (graphics.getTextScaleX() * graphics.getFont().getCellWidth() + letterSpacing);
    }

    /** Returns the number of fully visible rows that fit into this display */
    @Override
    public int getHeight() {
        return heightPx / (graphics.getTextScaleY() * graphics.getFont().getCellHeight() + leading);
    }

    @Override
    public void clear() {
        graphics.setColor(backgroundColor);
        graphics.fillRect(originX, originY, widthPx, heightPx);
        screenBuffer.clear();
    }

    @Override
    public void writeAt(float x, int y, String text, EnumSet<Attribute> attributes) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            getCell((int) x + i, y).set(c, attributes);
            renderCell(x, y, false);
        }
        setCursorPosition((int) x + text.length(), y);
    }

    public void renderCell(float cx, int cy, boolean withCursor) {
        CharacterCell cell = getCell((int) cx, cy);
        boolean invert = cell.attributes.contains(Attribute.INVERSE);
        int fg = !invert ? foregroundColor : backgroundColor;
        int bg = invert ? foregroundColor : backgroundColor;
        int cellWidth = graphics.getTextScaleX() * graphics.getFont().getCellWidth() + letterSpacing;
        int cellHeight = graphics.getTextScaleY() * graphics.getFont().getCellHeight() + leading;
        int scrX = originX + (int) (cx * cellWidth);
        int scrY = originY + cy * cellHeight;
        graphics.setColor(bg);
        graphics.fillRect(scrX, scrY, cellWidth, cellHeight);
        graphics.setColor(fg);
        if (withCursor) {
            for (int y = 0; y < cellHeight; y++) {
                for (int x = y & 1; x < cellWidth; x += 2) {
                    graphics.drawLine(scrX + x, scrY + y, scrX + x, scrY + y);
                }
            }
        }
        graphics.renderCharacter(scrX + letterSpacing / 2, scrY + cellHeight - leading / 2, cell.codePoint);
    }

    @Override
    public void setCursorEnabled(boolean value) {
        if (cursorEnabled != value) {
            renderCell(cursorX, cursorY, value);
            cursorEnabled = value;
        }
    }

    @Override
    public void setCursorPosition(int x, int y) {
        if (cursorEnabled && (x != cursorX || y != cursorY)) {
            renderCell(cursorX, cursorY, false);
            renderCell(x, y, true);
        }
        cursorX = x;
        cursorY = y;
    }

    // Private helpers

    CharacterCell getCell(int x, int y) {
        while(screenBuffer.size() <= y) {
            screenBuffer.add(new ArrayList<>());
        }
        List<CharacterCell> row = screenBuffer.get(y);
        while (row.size() <= x) {
            row.add(new CharacterCell());
        }
        return row.get(x);
    }

    class CharacterCell {
        int codePoint = ' ';
        EnumSet<Attribute> attributes = EnumSet.noneOf(Attribute.class);

        void set(int codePoint, EnumSet<Attribute> attributes) {
            this.codePoint = codePoint;
            this.attributes.clear();
            this.attributes.addAll(attributes);
        }
    }
}
