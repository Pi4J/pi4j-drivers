package com.pi4j.drivers.display.graphics;

import com.pi4j.drivers.display.BitmapFont;
import com.pi4j.drivers.display.character.CharacterDisplay;

import java.util.EnumSet;

/**
 * Implements the CharacterDisplay interface on top of a graphics display. Useful for running code that's designed
 * for text display with a graphics display.
 */
public class GraphicsCharacterDisplay implements CharacterDisplay {
    private final GraphicsDisplay display;
    private final BitmapFont font;
    private final int foregroundColor;
    private final int backgroundColor;
    private final int scale;
    private final Graphics graphics;

    public GraphicsCharacterDisplay(GraphicsDisplay display) {
        this (display,
                display.getHeight() > 128 ? BitmapFont.get5x10Font() : BitmapFont.get5x8Font(),
                0xffffffff,
                0xff000000,
                Math.max(display.getHeight() / 80, 1));
    }

    public GraphicsCharacterDisplay(GraphicsDisplay display, BitmapFont font, int foregroundColor, int backgroundColor, int scale) {
        this.display = display;
        this.font = font;
        this.foregroundColor = foregroundColor;
        this.backgroundColor = backgroundColor;
        this.scale = scale;
        this.graphics = display.getGraphics();
        graphics.setTextScale(scale);
        graphics.setFont(font);
    }

    @Override
    public EnumSet<Attribute> getSupportedAttributes() {
        return EnumSet.of(Attribute.INVERSE);
    }

    @Override
    public int getWidth() {
        return display.getWidth() / (scale * font.getCellWidth());
    }

    @Override
    public int getHeight() {
        return display.getHeight() / (scale * font.getCellHeight());
    }

    @Override
    public void clear() {
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, display.getWidth(), display.getHeight());
    }

    @Override
    public void writeAt(float x, int y, String text, EnumSet<Attribute> attributes) {
        boolean invert = attributes.contains(Attribute.INVERSE);
        int fg = !invert ? foregroundColor : backgroundColor;
        int bg = invert ? foregroundColor : backgroundColor;
        int px = (int) (x * scale * font.getCellWidth());
        int py = y * scale * font.getCellHeight();
        int width = scale * font.getCellWidth() * text.length();
        graphics.setColor(bg);
        graphics.fillRect(px, py, width, scale * font.getCellHeight());
        graphics.setColor(fg);
        graphics.renderText(px, py + scale * font.getCellHeight(), text);
    }
}
