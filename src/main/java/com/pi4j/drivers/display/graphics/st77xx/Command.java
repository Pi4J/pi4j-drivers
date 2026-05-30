package com.pi4j.drivers.display.graphics.st77xx;

enum Command {
    SWRESET(0x01),
    SLPOUT(0x11),
    NORON(0x13),
    INVON(0x21),
    DISPON(0x29),
    CASET(0x2A),
    RASET(0x2B),
    RAMWR(0x2C),
    MADCTL(0x36),
    COLMOD(0x3A);
    
    final int code;
    
    Command(int code) {
        this.code = code;
    }
}
