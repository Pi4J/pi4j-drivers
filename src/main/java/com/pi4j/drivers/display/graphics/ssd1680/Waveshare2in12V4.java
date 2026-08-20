package com.pi4j.drivers.display.graphics.ssd1680;

import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;

public class Waveshare2in12V4 extends Ssd1680Driver {
    public Waveshare2in12V4(
            Spi spi,
            DigitalOutput dc,
            DigitalOutput rst,
            DigitalInput busy) {
        super(spi, dc, rst, busy, 122, 250);
    }
}
