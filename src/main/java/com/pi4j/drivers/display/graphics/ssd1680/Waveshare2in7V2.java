package com.pi4j.drivers.display.graphics.ssd1680;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiMode;

public class Waveshare2in7V2 extends Ssd1680Driver {
    public Waveshare2in7V2(
            Spi spi,
            DigitalOutput dc,
            DigitalOutput rst,
            DigitalInput busy) {
        super(spi, dc, rst, busy, 176, 264);
        sendCommand(Command.DATA_ENTRY_MODE_SETTING, 0x03);
        sendCommand(Command.DISPLAY_UPDATE_CONTROL_2, 0xf7);
    }

    /** Convenience constructor using the "hat" pin configuration. */
    public Waveshare2in7V2(Context context) {
        this(context.create(Spi.newConfigBuilder(context).bus(0).channel(0).baud(2_000_000).mode(SpiMode.MODE_0)),
                context.create(DigitalOutput.newConfigBuilder(context).bcm(25)),
                context.create(DigitalOutput.newConfigBuilder(context).bcm(17)),
                context.create(DigitalInput.newConfigBuilder(context).bcm(24)));

    }
}
