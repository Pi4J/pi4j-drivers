package com.pi4j.drivers.io.ad.mcp472x;

import com.pi4j.io.i2c.I2C;

public class Mcp4725Driver extends Mcp472xDriver {

    protected final I2C i2c ;


    public Mcp4725Driver(I2C i2cHw, double vref) {
        super( vref);
        this.i2c = i2cHw;
    }



    @Override
    int read_chip(byte[] readData) {
        return  this.i2c.read(readData);
    }

    @Override
    int write_to_chip(byte[] chipData) {
        return this.i2c.write(chipData);
    }

    /**
     * Write reset command into chip
     * Result: chip POR and EEPROM loaded
     */
     public void resetChip(I2C resetI2c) {
        resetI2c.write(Mcp472xConstants._MCP4725_GEN_CALL_RESET_CMD);
        this.sleepMS(4);
    }

}
