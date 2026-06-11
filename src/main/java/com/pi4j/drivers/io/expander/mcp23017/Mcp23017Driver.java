package com.pi4j.drivers.io.expander.mcp23017;

import com.pi4j.drivers.io.expander.mcp23008.Mcp23008Driver;
import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.i2c.I2C;

/** A 16-bit variant of the MCP23008. For documentation, please refer to the Mcp23008Driver class. */
public class Mcp23017Driver extends Mcp23008Driver {
    private final static int BANK_B_OFFSET = 0x10;

    public Mcp23017Driver(I2C i2c, ListenableOnOffRead<?> interruptPin) {
        super(i2c, 16, interruptPin);
    }

    protected void writeRegister(int register, int value) {
        super.writeRegister(register, value);
        super.writeRegister(register | BANK_B_OFFSET, value >>> 8);
    }

    protected int readRegister(int register) {
        return super.readRegister(register) | (super.readRegister(register | BANK_B_OFFSET) << 8);
    }
}
