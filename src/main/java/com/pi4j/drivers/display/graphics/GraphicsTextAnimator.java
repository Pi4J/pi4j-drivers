package com.pi4j.drivers.display.graphics;

import com.pi4j.drivers.display.BitmapFont;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GraphicsTextAnimator {

    private final GraphicsDisplay display;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final int frameX;
    private final int frameY;
    private final int frameWidth;
    private final Object lock;

    private BitmapFont font = BitmapFont.get5x8Font(BitmapFont.Option.PROPORTIONAL);
    private int foreground = Argb32.WHITE;
    private int background = Argb32.BLACK;
    private Duration delay = Duration.ofMillis(100);
    private boolean clearOnStop = false;
    private int stepPixels = 1;
    private String text;
    private timerTask scrollTask;
    private Graphics graphics;
    private int offset;

    public GraphicsTextAnimator(GraphicsDisplay display, String text) {
        this(display, text, 0, 0, display.getWidth());
    }

    public GraphicsTextAnimator(
        GraphicsDisplay display,
        String text,
        int frameX,
        int frameY,
        int frameWidth
    ) {
        this.display = Objects.requireNonNull(display, "display must not be null");
        this.text = Objects.requireNonNull(text, "text must not be null");

        if (frameWidth <= 0) {
            throw new IllegalArgumentException("frameWidth must be > 0");
        }

        this.frameX = frameX;
        this.frameY = frameY;
        this.frameWidth = frameWidth;
        this.graphics = display.getGraphics();
    }

    public void setText(String text) {
        this.text = Objects.requireNonNull(text, "text must not be null");
    }

    public String getText() {
        return text;
    }

    public void setFont(BitmapFont font) {
        this.font = Objects.requireNonNull(font, "font must not be null");
    }

    public BitmapFont getFont() {
        return font;
    }

    public void setForeground(int foreground) {
        this.foreground = foreground;
    }

    public void setForeground(int r, int g, int b) {
        this.foreground = Argb32.fromRgb(r, g, b);
    }

    public int getForeground() {
        return foreground;
    }

    public void setBackground(int background) {
        this.background = background;
    }

    public void setBackground(int r, int g, int b) {
        this.background = Argb32.fromRgb(r, g, b);
    }

    public int getBackground() {
        return background;
    }

    public void setDelay(Duration delay) {
        Objects.requireNonNull(delay, "delay must not be null");

        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be negative");
        }

        this.delay = delay;
    }

    public void setDelayMillis(long delayMillis) {
        setDelay(Duration.ofMillis(delayMillis));
    }

    public Duration getDelay() {
        return delay;
    }

    public void setClearOnStop(boolean clearOnStop) {
        this.clearOnStop = clearOnStop;
    }

    public boolean isClearOnStop() {
        return clearOnStop;
    }

    public void setStepPixels(int stepPixels) {
        if (stepPixels <= 0) {
            throw new IllegalArgumentException("stepPixels must be > 0");
        }

        this.stepPixels = stepPixels;
    }

    public int getStepPixels() {
        return stepPixels;
    }

    public int getFrameX() {
        return frameX;
    }

    public int getFrameY() {
        return frameY;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    /** Clears the frame and renders the text once */
    public void render() {
       
    }

    public void start() {

    }

    public void stop() {
        running.set(false);

        Thread currentWorker = worker;
        if (currentWorker != null) {
            currentWorker.interrupt();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

}
