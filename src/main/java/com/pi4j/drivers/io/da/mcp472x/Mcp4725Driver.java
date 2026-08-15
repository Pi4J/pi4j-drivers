package com.pi4j.drivers.io.da.mcp472x;

import com.pi4j.drivers.io.da.DigitalAnalogConverter;
import com.pi4j.io.i2c.I2C;
import com.pi4j.util.Delay;

/**
 *  Driver for MCP4725 DAC.
 *  <p>
 *  This chip can preserve written values in an internal EEPROM. This feature is controlled via setEepromEnabled()
 *  and off by default. Note that writes through the EEPROM are slower.
 *
 *  @see <a href="https://https://www.alldatasheet.com/datasheet-pdf/pdf/1692417/MICROCHIP/MCP4725.html">MCP4725</a>
 */

public class Mcp4725Driver implements DigitalAnalogConverter {

    private final I2C i2c ;
    private final double vref;
    private final Delay delay = new Delay();
    private final byte[] ioBuffer = new byte[Math.max(Constants.MCP4725_SET_FAST_SIZE,
            Math.max(Constants.MCP4725_SET_EEPROM_SIZE, Constants.MCP4725_CHIP_READ_SIZE))];

    private boolean eepromEnabled;
    private boolean lastWriteWasToEeprom;

    public Mcp4725Driver(I2C i2cHw, double vref) {
        this.i2c = i2cHw;
        this.vref = vref;
    }

    /**
     * Write reset command into chip
     * Result: chip POR and EEPROM loaded
     */
     public void resetChip(I2C resetI2c) {
         resetI2c.write(Constants.MCP4725_GEN_CALL_RESET_CMD);
         delay.setMillis(4).materialize();
    }

    /** If enabled is true, values will be written to the eeprom. */
    public void setEepromEnabled(boolean enable) {
         this.eepromEnabled = enable;
    }

    /**
     * Note that the simple call (setVoltage(double value)) is available via the interface default method.
     * This will block if eeprom writing is enabled and the last write is not completed. Use the chipIdle()
     * call to query the state and avoid blocking.
     */
    @Override
    public void setVoltage(int outputChannel, double value) {
        if (outputChannel != 0) {
            throw new IllegalArgumentException("This chip has only channel 0");
        }
        setDigitalValue((int) ((value / this.vref) * 4095));
    }

    @Override
    public int getChannelCount() {
        return 1;
    }

    /**
     * Writes a 12 bit value to the chip. Blocks if the chip is busy.
     */
    public void setDigitalValue(int digitalValue) {
        if (digitalValue < 0 || digitalValue > 4095) {
            throw new IllegalArgumentException("Value out of range (0...4095): " + digitalValue);
        }
        if (lastWriteWasToEeprom) {
            while (!chipIdle()) {
                delay.setMillis(1).materialize();
            }
        }

        if (eepromEnabled) {
            ioBuffer[0] = (byte) (Constants.MCP4725_WRITE_CMD_DAC_EEPROM | Constants.MCP4725_PD_MODE_NORMAL);
            ioBuffer[1] = (byte) ((digitalValue & 0x0ff0) >> 4);
            ioBuffer[2] = (byte) ((digitalValue & 0x000f) << 4);
            ioBuffer[3] = ioBuffer[0];
            ioBuffer[4] = ioBuffer[1];
            ioBuffer[5] = ioBuffer[2];
            this.i2c.write(ioBuffer, 0, Constants.MCP4725_SET_EEPROM_SIZE);
        } else {
            ioBuffer[0] = (byte) (Constants.MCP4725_WRITE_CMD_FAST | Constants.MCP4725_PD_MODE_NORMAL
                    | ((digitalValue & 0x0f00) >> 8));
            ioBuffer[1] = (byte) digitalValue;
            ioBuffer[2] = ioBuffer[0];
            ioBuffer[3] = ioBuffer[1];
            this.i2c.write(ioBuffer, 0, Constants.MCP4725_SET_FAST_SIZE);
        }
        lastWriteWasToEeprom = eepromEnabled;
    }


    /**
     * Returns true if the chip is ready (idle), else false.
     */
    public boolean chipIdle() {
        i2c.read(ioBuffer, 0, Constants.MCP4725_CHIP_READ_SIZE);
        return (ioBuffer[0] & Constants.MCP4725_READ_CMD_IS_COMPLT) != 0;
    }


}
