package com.pi4j.drivers.display.graphics.text;

import com.pi4j.drivers.display.BitmapFont;
import com.pi4j.drivers.display.graphics.Argb32;
import com.pi4j.drivers.display.graphics.Graphics;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TextAnimator {

    private final GraphicsDisplay display;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private BitmapFont font = BitmapFont.get5x8Font(BitmapFont.Option.PROPORTIONAL);
    private int foreground = Argb32.WHITE;
    private int background = Argb32.BLACK;
    private Duration delay = Duration.ofMillis(100);
    private TextAnimationDirection direction = TextAnimationDirection.RIGHT_TO_LEFT;
    private boolean loop = false;
    private boolean clearOnStop = false;
    private int stepPixels = 1;

    private int frameX = 0;
    private int frameY = 0;
    private int frameWidth;
    private int frameHeight;

    private Thread worker;

    public TextAnimator(GraphicsDisplay display) {
        this.display = Objects.requireNonNull(display, "display must not be null");
        this.frameWidth = display.getWidth();
        this.frameHeight = display.getHeight();
    }

    public void setFont(BitmapFont font) {
        this.font = Objects.requireNonNull(font, "font must not be null");
    }

    public BitmapFont getFont() {
        return font;
    }

    public void setForeground(int color) {
        this.foreground = color;
    }

    public void setForeground(int r, int g, int b) {
        this.foreground = Argb32.fromRgb(r, g, b);
    }

    public int getForeground() {
        return foreground;
    }

    public void setBackground(int color) {
        this.background = color;
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

    public void setDirection(TextAnimationDirection direction) {
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
    }

    public TextAnimationDirection getDirection() {
        return direction;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean isLoop() {
        return loop;
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

    public void setFrame(int x, int y, int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be > 0");
        }

        if (height <= 0) {
            throw new IllegalArgumentException("height must be > 0");
        }

        this.frameX = x;
        this.frameY = y;
        this.frameWidth = width;
        this.frameHeight = height;
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

    public int getFrameHeight() {
        return frameHeight;
    }

    /**
     * Blocking call. Runs on the current thread.
     */
    public void scroll(String text) {
        Objects.requireNonNull(text, "text must not be null");

        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("TextAnimator is already running");
        }

        try {
            do {
                scrollOnce(text);
            } while (running.get() && loop);
        } finally {
            running.set(false);

            if (clearOnStop) {
                clear();
            }
        }
    }

    /**
     * Non-blocking call. Runs on a background thread.
     */
    public void start(String text) {
        Objects.requireNonNull(text, "text must not be null");

        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("TextAnimator is already running");
        }

        worker = new Thread(() -> {
            try {
                do {
                    scrollOnce(text);
                } while (running.get() && loop);
            } finally {
                running.set(false);
                worker = null;

                if (clearOnStop) {
                    clear();
                }
            }
        }, "pi4j-text-animator");

        worker.setDaemon(true);
        worker.start();
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

    private void scrollOnce(String text) {
        Graphics graphics = display.getGraphics();
        graphics.setFont(font);

        int textWidth = measureTextWidth(graphics, text);

        int startX;
        int endX;
        int step;

        if (direction == TextAnimationDirection.RIGHT_TO_LEFT) {
            startX = frameX + frameWidth;
            endX = frameX - textWidth;
            step = -stepPixels;
        } else {
            startX = frameX - textWidth;
            endX = frameX + frameWidth;
            step = stepPixels;
        }

        for (int x = startX; running.get() && shouldContinue(x, endX, step); x += step) {
            clear(graphics);

            graphics.setColor(foreground);
            graphics.renderText(x, frameY + frameHeight, text);

            sleep(delay);
        }
    }

    private int measureTextWidth(Graphics graphics, String text) {
        clear(graphics);

        graphics.setColor(foreground);
        int width = graphics.renderText(frameX + frameWidth, frameY + frameHeight, text);

        clear(graphics);

        return width;
    }

    private boolean shouldContinue(int x, int endX, int step) {
        if (step < 0) {
            return x >= endX;
        }

        return x <= endX;
    }

    private void clear() {
        clear(display.getGraphics());
    }

    private void clear(Graphics graphics) {
        graphics.setColor(background);
        graphics.fillRect(frameX, frameY, frameWidth, frameHeight);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }
}