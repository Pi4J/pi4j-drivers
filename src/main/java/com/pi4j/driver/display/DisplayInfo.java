package com.pi4j.driver.display;

public class DisplayInfo {
  private final int width;
  private final int height;
  private PixelFormat pixelFormat;

  public DisplayInfo(int width, int height, PixelFormat pixelFormat) {
    this.width = width;
    this.height = height;
    this.pixelFormat = pixelFormat;
  }

  int getWidth() { return width; } // In pixel
  int getHeight() { return height; }  
  PixelFormat getPixelFormat() { return pixelFormat; }
}
