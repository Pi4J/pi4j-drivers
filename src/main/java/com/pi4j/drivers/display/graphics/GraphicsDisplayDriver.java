package com.pi4j.drivers.display.graphics;

import java.io.Closeable;

public interface GraphicsDisplayDriver extends Closeable {

    GraphicsDisplayInfo getDisplayInfo();

    void setPixels(int x, int y, int width, int height, byte[] data);

    @Override
    void close();
}
