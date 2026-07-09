package com.pi4j.drivers.display.graphics;

import com.pi4j.drivers.display.BitmapFont;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GraphicsTextAnimator {

    public enum ScrollDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT }

    private final GraphicsDisplay display;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final int frameX;
    private final int frameY;
    private final int frameWidth;

    private BitmapFont font = BitmapFont.get5x8Font(BitmapFont.Option.PROPORTIONAL);
    private int foreground = Argb32.WHITE;
    private int background = Argb32.BLACK;
    private Duration delay = Duration.ofMillis(100);
    private boolean clearOnStop = false;
    private int stepPixels = 1;
    private String text;
    private ScrollDirection scrollDirection = ScrollDirection.RIGHT_TO_LEFT;

    private volatile Thread worker;

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

    public void setScrollDirection(ScrollDirection scrollDirection) {
        this.scrollDirection = Objects.requireNonNull(scrollDirection, "scrollDirection must not be null");
    }

    public ScrollDirection getScrollDirection() {
        return scrollDirection;
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

    public void scrollText() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("GraphicsTextAnimator is already running");
        }
        try {
            scrollOnce();
        } finally {
            running.set(false);
            if (clearOnStop) {
                clear();
            }
        }
    }

    public void scroll() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("GraphicsTextAnimator is already running");
        }
        try {
            runLoop();
        } finally {
            running.set(false);
            if (clearOnStop) {
                clear();
            }
        }
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("GraphicsTextAnimator is already running");
        }

        worker = new Thread(() -> {
            try {
                runLoop();
            } finally {
                running.set(false);
                worker = null;
                if (clearOnStop) {
                    clear();
                }
            }
        }, "pi4j-graphics-text-animator");

        worker.setDaemon(true);
        worker.start();
    }

    private void runLoop() {
        do {
            scrollOnce();
        } while (running.get());
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

    private void scrollOnce() {
        Graphics graphics = display.getGraphics();
        graphics.setFont(font);

        int textWidth = measureTextWidth(graphics);
        int textBaseline = frameY + font.getCellHeight();

        if (scrollDirection == ScrollDirection.RIGHT_TO_LEFT) {
            int startX = frameX + frameWidth;
            int endX = frameX - textWidth;
            for (int x = startX; running.get() && x >= endX; x -= stepPixels) {
                clear(graphics);
                graphics.setColor(foreground);
                graphics.renderText(x, textBaseline, text);
                sleep(delay);
            }
        } else {
            int startX = frameX - textWidth;
            int endX = frameX + frameWidth;
            for (int x = startX; running.get() && x <= endX; x += stepPixels) {
                clear(graphics);
                graphics.setColor(foreground);
                graphics.renderText(x, textBaseline, text);
                sleep(delay);
            }
        }
    }

    private int measureTextWidth(Graphics graphics) {
        int textBaseline = frameY + font.getCellHeight();
        graphics.setColor(foreground);
        return graphics.renderText(frameX + frameWidth, textBaseline, text);
    }

    private void clear() {
        clear(display.getGraphics());
    }

    private void clear(Graphics graphics) {
        graphics.setColor(background);
        graphics.fillRect(frameX, frameY, frameWidth, font.getCellHeight());
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