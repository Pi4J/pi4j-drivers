package com.pi4j.drivers.display.graphics.awt;

import com.pi4j.drivers.display.graphics.GraphicsDisplayDescriptor;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.PixelFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.atomic.AtomicBoolean;

public class JFrameGraphicsDriver implements GraphicsDisplayDriver {

    private static final Logger log = LoggerFactory.getLogger(JFrameGraphicsDriver.class);

    private final GraphicsDisplayDescriptor displayInfo;
    private final BufferedImage imageBuffer;
    private final int[] pixelBuffer; // direct backing int[] (TYPE_INT_RGB)
    private final Graphics2D g2d;
    private final CustomPanel customPanel;
    private final JFrame frame;

    // Coalesce repaints under heavy write load
    private final AtomicBoolean repaintQueued = new AtomicBoolean(false);

    public JFrameGraphicsDriver(int width, int height) {
        this.displayInfo = new GraphicsDisplayDescriptor(width, height, PixelFormat.RGB_888);

        this.imageBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.pixelBuffer = ((DataBufferInt) imageBuffer.getRaster().getDataBuffer()).getData();
        this.g2d = imageBuffer.createGraphics();

        this.customPanel = new CustomPanel(width, height);
        this.frame = new JFrame("Java Graphics Driver");

        Runnable initUi = () -> {
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(customPanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        };

        if (SwingUtilities.isEventDispatchThread()) {
            initUi.run();
        } else {
            SwingUtilities.invokeLater(initUi);
        }
    }

    private class CustomPanel extends JPanel {
        private final int width;
        private final int height;

        public CustomPanel(int width, int height) {
            this.width = width;
            this.height = height;
            setOpaque(true);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(width, height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(imageBuffer, 0, 0, this);
        }
    }

    @Override
    public void close() {
        log.info("Closing JFrameGraphicsDriver");
        g2d.dispose();

        Runnable disposeUi = () -> {
            if (frame.isDisplayable()) {
                frame.dispose();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            disposeUi.run();
        } else {
            SwingUtilities.invokeLater(disposeUi);
        }
    }

    @Override
    public GraphicsDisplayDescriptor getDisplayInfo() {
        return displayInfo;
    }

    @Override
    public void setPixels(int x, int y, int width, int height, byte[] data) {
        if (data == null)
            throw new IllegalArgumentException("data must not be null");
        if (width <= 0 || height <= 0)
            return;

        final int bpp = 3; // RGB_888
        final int required = width * height * bpp;
        if (data.length < required) {
            throw new IllegalArgumentException(
                    "Insufficient pixel data: expected at least " + required + " bytes, got " + data.length);
        }

        final int dispW = displayInfo.getWidth();
        final int dispH = displayInfo.getHeight();

        // Clip destination rectangle to display bounds
        final int dstX0 = Math.max(0, x);
        final int dstY0 = Math.max(0, y);
        final int dstX1 = Math.min(dispW, x + width);
        final int dstY1 = Math.min(dispH, y + height);

        if (dstX0 >= dstX1 || dstY0 >= dstY1)
            return;

        final int copyW = dstX1 - dstX0;
        final int copyH = dstY1 - dstY0;

        // Source start adjusted by clipping
        final int srcX0 = dstX0 - x;
        final int srcY0 = dstY0 - y;

        // Fast path: direct array writes, row by row
        // TYPE_INT_RGB layout expects 0x00RRGGBB
        synchronized (pixelBuffer) {
            for (int row = 0; row < copyH; row++) {
                final int srcRow = srcY0 + row;
                final int dstRow = dstY0 + row;

                final int srcBase = (srcRow * width + srcX0) * bpp;
                int dstIndex = dstRow * dispW + dstX0;
                int srcIndex = srcBase;

                for (int col = 0; col < copyW; col++) {
                    int r = data[srcIndex] & 0xFF;
                    int g = data[srcIndex + 1] & 0xFF;
                    int b = data[srcIndex + 2] & 0xFF;
                    pixelBuffer[dstIndex++] = (r << 16) | (g << 8) | b;
                    srcIndex += bpp;
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("setPixels: x={}, y={}, w={}, h={}", x, y, width, height);
        }

        requestRepaint();
    }

    private void requestRepaint() {
        if (repaintQueued.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(() -> {
                try {
                    customPanel.repaint();
                } finally {
                    repaintQueued.set(false);
                }
            });
        }
    }
}
