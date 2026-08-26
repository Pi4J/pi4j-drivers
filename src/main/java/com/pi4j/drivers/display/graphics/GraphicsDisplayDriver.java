package com.pi4j.drivers.display.graphics;

import java.io.Closeable;

public interface GraphicsDisplayDriver extends Closeable {

    GraphicsDisplayDescriptor getDisplayInfo();

    void setPixels(int x, int y, int width, int height, byte[] data);

    /**
     * Displays that need time to update beyond the data transfer (typically e-Ink displays)
     * can use this method to signal their busy state. Display updates will be aggregated until
     * the display is ready to process a new update.
     */
    default boolean isBusy() {
        return false;
    }

    /**
     * The limit of how much pixel data this driver can digest at once; defaults to the linux
     * SPI transfer limit. E-ink displays should set this to Integer.MAX_VALUE and handle splitting
     * transfers internally in order to avoid unnecessary screen updates.
     */
    default int getTransferLimit() {
        return GraphicsDisplay.DEFAULT_MAX_TRANSFER_SIZE;
    }

    @Override
    void close();
}
