package com.pi4j.drivers.display.character.hd44780;

import com.pi4j.io.OnOffWrite;

/**
 * IO implementation for connecting a HD44780 display via a parallel 4-bit connections using 7 pins:
 * 4 data pins and 3 control pins.
 */
public class Parallel4BitConnection extends AbstractConnection {

    private final OnOffWrite registerSelect;
    private final OnOffWrite enable;
    private final OnOffWrite backLight;
    private final OnOffWrite d4;
    private final OnOffWrite d5;
    private final OnOffWrite d6;
    private final OnOffWrite d7;

    private static void setState(OnOffWrite pin, boolean state) {
        if (state) {
            pin.on();
        } else {
            pin.off();
        }
    }

    public Parallel4BitConnection(
            OnOffWrite registerSelect,
            OnOffWrite enable,
            OnOffWrite backLight,
            OnOffWrite d4,
            OnOffWrite d5,
            OnOffWrite d6,
            OnOffWrite d7) {
        this.registerSelect = registerSelect;
        this.enable = enable;
        this.backLight = backLight;
        this.d4 = d4;
        this.d5 = d5;
        this.d6 = d6;
        this.d7 = d7;
    }

    @Override
    protected boolean is8Bit() {
        return false;
    }

    @Override
    protected void setBacklight(boolean on) {
        setState(backLight, on);
    }

    @Override
    public void sendValue(Mode mode, int value) {
        setState(this.registerSelect, mode == Mode.DATA);
        if (mode != Mode.INIT) {
            sendNibble(value >> 4);
        }
        sendNibble(value);
    }

    void sendNibble(int value) {
        setState(d4, (value & 0b0001) != 0);
        setState(d5, (value & 0b0010) != 0);
        setState(d6, (value & 0b0100) != 0);
        setState(d7, (value & 0b1000) != 0);
        setState(enable, true);
        setDelayMicros(1);  // Enable cycle time is 1000 ns
        materializeDelay();
        setState(enable, false);
        setDelayMicros(1);
    }
}
