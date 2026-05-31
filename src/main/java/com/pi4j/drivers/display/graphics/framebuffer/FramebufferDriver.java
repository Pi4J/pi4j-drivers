package com.pi4j.drivers.display.graphics.framebuffer;

import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDescriptor;
import com.pi4j.drivers.display.graphics.PixelFormat;

import java.io.*;

public class FramebufferDriver implements GraphicsDisplayDriver, Closeable {

    private static final String SENSE_HAT_FB_NAME = "RPi-Sense FB";
    private final GraphicsDisplayDescriptor displayInfo;
    private final RandomAccessFile file;

    public static String resolveFramebufferName(String name) {
        File dir = new File("/sys/class/graphics/");
        for (File candidate :  dir.listFiles()) {
            if (candidate.getName().startsWith("fb")) {
                File nameFile = new File(candidate, "name");
                if (nameFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(nameFile))) {
                        String line = reader.readLine().trim();
                        if (line.equals(name)) {
                           return "/dev/" + candidate.getName();
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        return null;
    }

    public static GraphicsDisplayDriver forSenseHat() {
        return new FramebufferDriver(resolveFramebufferName(SENSE_HAT_FB_NAME), 8, 8, PixelFormat.RGB_565_LE);
    }

    public FramebufferDriver(String filename, int width, int height, PixelFormat pixelFormat) {
        try {
            this.file = new RandomAccessFile(filename, "rw");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        this.displayInfo = new GraphicsDisplayDescriptor(width, height, pixelFormat);
    }

    @Override
    public void close() {
        try {
            file.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public GraphicsDisplayDescriptor getDisplayInfo() {
        return displayInfo;
    }


    @Override
    public void setPixels(int x, int y, int width, int height, byte[] data) {
        try {
            int srcOffset = 0;
            int bitsPerRow = (width * getDisplayInfo().getPixelFormat().getBitCount());
            if (bitsPerRow % 8 != 0) {
                throw new IllegalArgumentException("Row bit width " + bitsPerRow + " must be a multiple of 8");
            }
            int bytesPerRow = bitsPerRow / 8;
            for (int i = 0; i < height; i++) {
                file.seek(getPixelAddress(x, y + i));
                file.write(data, srcOffset, bytesPerRow);
                srcOffset += bytesPerRow;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private int getPixelAddress(int x, int y) {
        int bitAddress = (y * getDisplayInfo().getWidth() + x) *  getDisplayInfo().getPixelFormat().getBitCount();
        if (bitAddress % 8 != 0) {
            throw new IllegalArgumentException("Pixel bit address " + bitAddress + " for " + x + ", " + y + " is not on a byte boundary.");
        }
        return bitAddress / 8;
    }
}
