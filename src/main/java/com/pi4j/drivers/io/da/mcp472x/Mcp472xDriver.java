package com.pi4j.drivers.io.da.mcp472x;


import com.pi4j.util.Delay;


public abstract class Mcp472xDriver {

    protected double vref ;
    final Delay delay = new Delay();

    public Mcp472xDriver( double vref) {
        this.vref = vref ;
    }


    public boolean setOutputVoltEEPROM(float voltage) {
        int twelveBit = (int) ((voltage/this.vref) * 4096);
        return  this.setOutputEEPROM(twelveBit);
    }

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

    public boolean setOutputVoltFast(float voltage) {
        int twelveBit = (int) ((voltage/this.vref) * 4096);
        return  this.setOutputFast(twelveBit);
    }

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


    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    int[] readBuffer(int readLen) {
        byte[] readData = new byte[readLen];
        int rc = readChip(readData);
        int[] data = new int[rc];
        for (int i = 0; i < readLen; i++) {
            data[i] = (readData[i] & 0xff);
        }
        return (data);
    }



    abstract int readChip(byte[] readData);

    abstract int writeToChip(byte[] chipData);


}
