package com.pi4j.drivers.display.character.hd44780;

/** Command codes used by the driver */
class CommandCodes {

    /** Display commands and their flags. */
    static final int CMD_CLEAR_DISPLAY   = 0x01;
    static final int CMD_RETURN_HOME     = 0x02;

    static final int CMD_ENTRY_MODE       = 0x04;
    static final int EM_INCREMENT         = 0x02;
    static final int EM_DECREMENT         = 0x00;
    static final int EM_DISPLAY_SHIFT_ON  = 0x01;
    static final int EM_DISPLAY_SHIFT_OFF = 0x00;

    static final int CMD_DISPLAY_CONTROL = 0x08;
    static final int DC_DISPLAY_ON       = 0x04;
    static final int DC_DISPLAY_OFF      = 0x00;
    static final int DC_CURSOR_ON        = 0x02;
    static final int DC_CURSOR_OFF       = 0x00;
    static final int DC_BLINK_ON         = 0x01;
    static final int DC_BLINK_OFF        = 0x00;

    static final int CMD_CURSOR_SHIFT_LEFT   = 0b0001_0000;
    static final int CMD_CURSOR_SHIFT_RIGHT  = 0b0001_0100;
    static final int CMD_DISPLAY_SHIFT_LEFT  = 0b0001_1000;
    static final int CMD_DISPLAY_SHIFT_RIGHT = 0b0001_1100;

    static final int CMD_FUNCTION_SET    = 0x20;
    static final int FS_8_BIT            = 0x10;
    static final int FS_4_BIT            = 0x00;
    static final int FS_2_LINES          = 0x08;
    static final int FS_1_LINE           = 0x00;
    static final int FS_CHARSET_5X10     = 0x04;
    static final int FS_CHARSET_5X8      = 0x00;

    static final int CMD_SET_CGRAM_ADDR  = 0x40;
    static final int CMD_SET_DDRAM_ADDR  = 0x80;

    // flags for display/cursor shift
    static final int LCD_DISPLAY_MOVE = 0x08;
    static final int LCD_CURSOR_MOVE  = 0x00;

    // flags for function set
    static final int LCD_8BIT_MODE = 0x10;
    static final int LCD_4BIT_MODE = 0x00;
    static final int LCD_2LINE     = 0x08;
    static final int LCD_1LINE     = 0x00;
    static final int LCD_5x10DOTS  = 0x04;
    static final int LCD_5x8DOTS   = 0x00;

}
