package com.pi4j.drivers.display.graphics;

import com.pi4j.drivers.display.BitmapFont;

import java.time.Duration;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A class for rendering a scrolling message. Useful for showing longer texts on small displays.
 */
public final class GraphicsTextAnimator {

    private final int frameX;
    private final int frameY;
    private final int frameWidth;
    private final Object lock = new Object();
    private final Timer timer = new Timer(/* daemon= */ true);

    private BitmapFont font = BitmapFont.get5x8Font(BitmapFont.Option.PROPORTIONAL);
    private int foreground = Argb32.WHITE;
    private int background = Argb32.BLACK;
    private Duration delay = Duration.ofMillis(100);
    private boolean clearOnStop = false;
    private int stepPixels = 1;
    private String text;
    private TimerTask scrollTask;
    private Graphics graphics;
    private int offset;

    /**
     * Creates a scrolling message at the top of the display, spanning the whole display width. Note that the
     * constructor doesn't display anything (enabling detailed configuration); use start() or the methods for manual
     * scrolling to actually display text.
     */
    public GraphicsTextAnimator(GraphicsDisplay display, String text) {
        this(display, text, 0, 0, display.getWidth());
    }

    /**
     * Creates a scrolling message in the given frame coordinates. The frame height will be determined by
     * the font height. Note that the constructor doesn't display anything (enabling detailed configuration);
     * use start() or the methods for manual scrolling to actually display text.
     */
    public GraphicsTextAnimator(
        GraphicsDisplay display,
        String text,
        int frameX,
        int frameY,
        int frameWidth
    ) {
        this.graphics = Objects.requireNonNull(display, "display must not be null").getGraphics();
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

    /** Sets the delay between scrolling steps. */
    public void setDelay(Duration delay) {
        synchronized (lock) {
            Objects.requireNonNull(delay, "delay must not be null");

            if (delay.isNegative()) {
                throw new IllegalArgumentException("delay must not be negative");
            }

            this.delay = delay;

            if (isRunning()) {
                stop();
                start();
            }
        }
    }

    /** Sets the delay between scrolling steps in milliseconds. */
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

    /** Clears the frame and renders the text just once. */
    public int render() {
        synchronized (lock) {
            clear();

            // This uses clipping and the color from the last call
            graphics.setClip(frameX, frameY, frameWidth, font.getCellHeight());
            graphics.setColor(foreground);
            return graphics.renderText(frameX + offset, frameY + font.getCellHeight(), text);
        }
    }

    /** Clears the frame. */
    public void clear() {
        synchronized (lock) {
            graphics.setColor(background);
            graphics.fillRect(frameX, frameY, frameWidth, font.getCellHeight());
        }
    }

    /** Clears the frame, renders the text and advances the scroll position by the configured amount of pixels. */
    public void scroll() {
        synchronized (lock) {
            int textWidth = render();
            offset -= stepPixels;
            if (offset + textWidth < 0) {
                offset = frameWidth;
            }
        }
    }

    /**
     * Starts rendering the text asynchronously in the background. Use the stop() method to stop scrolling.
     * Will throw an IllegalStateException when scrolling was started already.
     */
    public void start() {
        synchronized (lock) {
            if (scrollTask != null) {
                throw new IllegalStateException("Already started.");
            }
            scrollTask = new TimerTask() {
                @Override
                public void run() {
                        scroll();
                }
            };
            long delayMs = delay.toMillis();
            timer.schedule(scrollTask, delayMs, delayMs);
        }
    }

    /** Stops background scrolling. This is safe to call multiple times. */
    public void stop() {
        synchronized (lock) {
            if (scrollTask != null) {
                scrollTask.cancel();
                scrollTask = null;
            }
            if (clearOnStop) {
                clear();
            }
        }
    }

    /** Returns true if scrolling is currently active; false otherwise. */
    public boolean isRunning() {
        synchronized (lock) {
            return scrollTask != null;
        }
    }
}
