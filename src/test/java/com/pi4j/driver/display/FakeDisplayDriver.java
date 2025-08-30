package com.pi4j.driver.display;

import java.io.IOException;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeDisplayDriver implements GraphicsDisplayDriver {

    private static Logger log = LoggerFactory.getLogger(FakeDisplayDriver.class);

    private byte[] data;
    private DisplayInfo displayInfo;

    public FakeDisplayDriver(DisplayInfo displayInfo) {
        this.displayInfo = displayInfo;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public DisplayInfo getDisplayInfo() {

        return displayInfo;
    }

    @Override
    public void setPixels(int x, int y, int width, int height, byte[] data) {
        log.trace("setPixels: {} {} {} {}", x, y, width, height);
        log.trace("\t" + HexFormat.of().formatHex(data));
        this.data = data;
    }
}
