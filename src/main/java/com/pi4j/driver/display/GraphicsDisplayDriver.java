package com.pi4j.driver.display;

public interface GraphicsDisplayDriver {
  DisplayInfo getDisplayInfo();

  /** 
   * Renders a horizontal line of “bulk” pixel data. 
   * If length < w, pixels will be repeated.
   * x, y, and x + w are expected to be within the bounds of the display.
   * Offset and length are in pixels to allow bitwise addressing for 
   * monochrome displays (for grayscale and color displays, these should
   * always be multiple of bytes.)
   *
   * Note that this can be implemented as a simple setPixel loop should 
   * bulk transfer not be available (yet).
   */

  void setPixels(int x, int y, int w, byte[] data, int offset, int length) throws Exception;
}
