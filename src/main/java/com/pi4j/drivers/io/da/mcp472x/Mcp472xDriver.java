package com.pi4j.drivers.io.da.mcp472x;


import com.pi4j.util.Delay;

/**
 *  Parent class to family of MCP472x DAC chips
 */

public abstract class Mcp472xDriver {

    protected double vref ;
    final Delay delay = new Delay();

    public Mcp472xDriver( double vref) {
        this.vref = vref ;
    }


    /**
     *   Set DAC eeprom to the 12 bit value
     *
     * @param voltage    float value of the desired VOUT. Method calculates the DAC
     *                   value and sets the DAC register
     * @return
     */
    public boolean setOutputVoltEEPROM(float voltage) {
        int twelveBit = (int) ((voltage/this.vref) * 4096);
        return  this.setOutputEEPROM(twelveBit);
    }


    /**
     *   Set DAC eeprom to the 12 bit value
     *
     *                            12 bits
     *   C2 C1 C0 RDY POR PD1 PD0 D11 D10 D9 D8 D7 D6 D5 D4 D3 D2 D1 D0
     *
     * @param twelveBitData
     * @return true with success else false
     */
    public boolean setOutputEEPROM(int twelveBitData) {
        boolean rval = false;
        int registerData = twelveBitData;
        if (this.chipIdle()) {
            byte[] data = new byte[Constants._MCP4725_SET_EEPROM_SIZE];
            data[0] = (byte) (data[0] | Constants._MCP4725_WRITE_CMD_DAC_EEPROM | Constants._MCP4725_PD_MODE_NORMAL);
            data[1] = (byte) ((registerData & 0x0ff0) >> 4);
            data[2] = (byte) ((registerData & 0x000f) << 4);
            data[3] = data[0];
            data[4] = data[1];
            data[5] = data[2];
            writeToChip(data);
            rval = true;
        }
        return (rval);
    }


    /**
     *
     * @param voltage    float value of the desired VOUT. Method calculates the DAC
     *                   value and sets the DAC register
     * @return
     */
    public boolean setOutputVoltFast(float voltage) {
        int twelveBit = (int) ((voltage/this.vref) * 4096);
        return  this.setOutputFast(twelveBit);
    }

    /**
     *   Set DAC register to the 12 bit value
     *
     *                            12 bits
     *   C2 C1 C0 RDY POR PD1 PD0 D11 D10 D9 D8 D7 D6 D5 D4 D3 D2 D1 D0
     *
     * @param twelveBitData
     * @return true with success else false
     */
    public boolean setOutputFast(int twelveBitData) {
        boolean rval = false;
        int registerData = twelveBitData;
        String binaryString = Integer.toBinaryString(registerData & 0xff);
        String withLeadingZeros = String.format("0b%12s", binaryString).replace(' ', '0');
        if (this.chipIdle()) {
            byte[] data = new byte[Constants._MCP4725_SET_FAST_SIZE];
            data[0] = (byte) (data[0] | Constants._MCP4725_WRITE_CMD_FAST | Constants._MCP4725_PD_MODE_NORMAL);
            data[0] = (byte) (data[0] | (byte) ((registerData & 0x0f00) >> 8));
            data[1] = (byte) (registerData & 0x00ff);
            data[2] = data[0];
            data[3] = data[1];
            writeToChip(data);
            rval = true;
        }
        return (rval);
    }


    /**
     *
     * @return  true if chip idle, else false
     */
    boolean chipIdle() {
        int[] data = this.readBuffer(Constants._MCP4725_CHIP_READ_SIZE);
        return (data[0] & Constants._MCP4725_READ_CMD_IS_COMPLT) != 0;
    }


    /**
     * @param mSecs Time to sleep this thread
     */
    void sleepMS(long mSecs) {
        delay.setMillis(mSecs).materialize();

    }


    int[] readBuffer(int readLen) {
        byte[] readData = new byte[readLen];
        int rc = readChip(readData);
        int[] data = new int[rc];
        for (int i = 0; i < readLen; i++) {
            data[i] = (readData[i] & 0xff);
        }
        return (data);
    }


    /**
     *   Subclass implements code capable to using the chip 'bus' for communication
     * @param readData
     * @return  number bytes read
     */
    abstract int readChip(byte[] readData);

    /**
     *   Subclass implements code capable to using the chip 'bus' for communication
     *
     * @param chipData
     * @return  number bytes written
     */
    abstract int writeToChip(byte[] chipData);


}
