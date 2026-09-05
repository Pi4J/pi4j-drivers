package com.pi4j.drivers.io.da.mcp472x;

/*
 * package-private
 */
class Constants {
    static final int MCP4725_DEFAULT_ADDRESS = 0x62;


    static final int MCP4725_SET_EEPROM_SIZE = 0x06;
    static final int MCP4725_SET_FAST_SIZE = 0x04;
    static final int MCP4725_CHIP_READ_SIZE = 0x05;

    static final byte MCP4725_WRITE_CMD_FAST = 0b00000000;
    static final byte MCP4725_WRITE_CMD_DAC = 0b01000000;
    static final byte MCP4725_WRITE_CMD_DAC_EEPROM = 0b01100000;

    static final byte MCP4725_PD_MODE_NORMAL = 0b00000000;
    static final byte MCP4725_PD_MODE_INOPT = 0b00000110;

    static final byte MCP4725_DAC_PD0_MODE_MASK = 0b00000010;
    static final byte MCP4725_DAC_PD1_MODE_MASK = 0b00000100;

    static final int MCP4725_EEPROM_PD1_MODE_MASK = 0b10000000;
    static final byte MCP4725_EEPROM_PD0_MODE_MASK = 0b01000000;

    // Indicate EEPROM write/update status
    static final byte MCP4725_READ_CMD_RDY_BSY_MSK = (byte) 0b10000000;
    static final byte MCP4725_READ_CMD_IS_COMPLT = (byte) 0b10000000;
    static final byte MCP4725_READ_CMD_IS_NOT_COMPLT = (byte) 0b00000000;

    // POR device load EEPROM
    static final byte MCP4725_GEN_CALL_RESET_CMD = (byte) 0b00000110;

    // POR device   PD1 and PD0 set to 0 for normal operation
    static final byte MCP4725_GEN_CALL_WAKEUP_CMD = (byte) 0b00001001;


    // MCP4728
    static final int MCP4728_DEFAULT_ADDRESS = 0x60;

    static final int MCP4728_DAC_EEPROM_DATA_SZ   = 24;


    static final int MCP4728_SET_EEPROM_SIZE    = 0x03;
    static final int MCP4728_SET_FAST_SIZE      = 0x06;
    static final int MCP4728_CHIP_READ_SIZE     = 0x05;
    static final int MCP4728_SET_VREF_SIZE      = 0x01;
    static final int MCP4728_SET_GAIN_SIZE      = 0x01;
    static final int MCP4728_SYNC_EEPROM_SIZE   = 0x03;

    // Only the  Power-Down mode selection bits (PD1 and PD0) and12 bits of DAC
    static final byte MCP4728_WRITE_CMD_FAST = 0b00000000;

    //  multi select channel  write DAC, not eeprom   39
    static final byte MCP4728_WRITE_CMD_DAC = 0b01000000;

    // !!!  write DAC and eeprom    3 bytes    P41
    static final byte MCP4728_SINGL_WRITE_CMD_DAC_EEPROM = 0b01011000;

    //  !!!   write address   p 42
    static final byte MCP4728_WRITE_ADDR_DAC_EEPROM = 0b01100000;
    static final byte MCP4728_ADDR_MASK_DAC_EEPROM  = 0b00000111;

    static final byte MCP4728_CHANL_MASK_DAC_EEPROM  = 0b00110000;

    //  !!! vref    p43
    static final byte MCP4728_WRITE_VREF_CMD_DAC = (byte) 0b10000000;
    // !!! gain   p44
    static final byte MCP4728_WRITE_GAIN_CMD_DAC = (byte) 0b11000000;
    // !!! PD   p 43
    static final byte MCP4728_WRITE_PWR_DWN_CMD_DAC = (byte) 0b10100000;
    //


    //   intricate operation of LDAC pin  will not support
    static final byte MCP4728_READ_CMD_DAC_EEPROM_FIRST  = 0b01100001;
    static final byte MCP4728_READ_CMD_DAC_EEPROM_SECOND = 0b01100010;
    static final byte MCP4728_READ_CMD_DAC_EEPROM_THIRD  = 0b01100011;


    static final byte MCP4728_PD_MODE_NORMAL = 0b00000000;
    static final byte MCP4728_PD_MODE_INOPT = 0b00000110;

    static final byte MCP4728_DAC_PD0_MODE_MASK = 0b00000010;
    static final byte MCP4728_DAC_PD1_MODE_MASK = 0b00000100;

    static final int MCP4728_EEPROM_PD1_MODE_MASK = 0b10000000;
    static final byte MCP4728_EEPROM_PD0_MODE_MASK = 0b01000000;

    // Indicate EEPROM write/update status
    static final byte MCP4728_READ_CMD_RDY_BSY_MSK = (byte) 0b10000000;
    static final byte MCP4728_READ_CMD_IS_COMPLT = (byte) 0b10000000;
    static final byte MCP4728_READ_CMD_IS_NOT_COMPLT = (byte) 0b00000000;

    // POR device load EEPROM
    static final byte MCP4728_GEN_CALL_RESET_CMD = (byte) 0b00000110;

    // POR device   PD1 and PD0 set to 0 for normal operation
    static final byte MCP4728_GEN_CALL_WAKEUP_CMD = (byte) 0b00001001;


    // Write
    //  C2=0  C1=1 C0=0 DAC only
    //  C2=0  C1=1 C0=1 DAC and EEPROM
    //  byte1                  byte2                            byte3                       byte4
    // address r/w       C2 C1 C0 x x PD1 PD0 X           D11 D10 D9 D8 D7 D6 D5 D4      D3 D2 D1 D0 X X X X

    // Read   DAC
    //  byte1                  byte2                            byte3                         byte4
    // address r/w       RDY/BSY C1 C0 x x PD1 PD0 X      D11 D10 D9 D8 D7 D6 D5 D4      D3 D2 D1 D0 X X X X

    //   EEPROM
    //    byte5                                    byte6
    //  X PD1 PD0 X D11 D10 D9 D8      D7 D6 D5 D4 D3 D2 D1 D0
}
