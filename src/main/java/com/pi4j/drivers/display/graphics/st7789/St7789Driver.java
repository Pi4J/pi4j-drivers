package com.pi4j.drivers.display.graphics.st7789;

import com.pi4j.drivers.display.graphics.PixelFormat;

import com.pi4j.drivers.display.graphics.st77xx.St77xxDriver;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;

import java.util.EnumSet;

/*
 * Tested on Adafruit 1.54" 240x240 Wide Angle TFT LCD Display with MicroSD - ST7789 with EYESPI Connector
 * https://www.adafruit.com/product/3787
 */
public class St7789Driver extends St77xxDriver {

    /** The SPI baud rate supported by this chip. */
    public static final int SPI_BAUDRATE = 62_500_000;
    public static final EnumSet<PixelFormat> SUPPORTED_PIXEL_FORMATS = EnumSet.of(PixelFormat.RGB_444, PixelFormat.RGB_565);

    public St7789Driver(Spi spi, DigitalOutput dc, int displayHeight, PixelFormat pixelFormat) {
        super(spi, dc, null, pixelFormat, true, 240, displayHeight, 0, 320 - displayHeight);
    }
}
