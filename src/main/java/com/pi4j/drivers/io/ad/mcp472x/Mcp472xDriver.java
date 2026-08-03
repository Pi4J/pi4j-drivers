package com.pi4j.drivers.io.ad.mcp472x;


import com.pi4j.util.Delay;


public abstract class Mcp472xDriver {

    protected double vref ;
    final Delay delay = new Delay();

    public Mcp472xDriver( double vref) {
        this.vref = vref ;
    }


    public boolean setOutputVoltEEPROM(float twelveBitData) {
        boolean rval = false;
        int twelveBit = (int) Math.round((twelveBitData * 4096) / this.vref) - 1;
        rval = this.setOutputEEPROM(twelveBit);
        return (rval);
    }

    public boolean setOutputEEPROM(int twelveBitData) {
        boolean rval = false;
        int registerData = twelveBitData;
        if (this.chipIdle()) {
            byte[] data = new byte[Mcp472xConstants._MCP4725_SET_EEPROM_SIZE];
            data[0] = (byte) (data[0] | Mcp472xConstants._MCP4725_WRITE_CMD_DAC_EEPROM | Mcp472xConstants._MCP4725_PD_MODE_NORMAL);
            data[1] = (byte) ((registerData & 0x0ff0) >> 4);
            data[2] = (byte) ((registerData & 0x000f) << 4);
            data[3] = data[0];
            data[4] = data[1];
            data[5] = data[2];
            write_to_chip(data);
            rval = true;
        }
        return (rval);
    }

    public boolean setOutputVoltFast(float twelveBitData) {
        boolean rval = false;
        int twelveBit = (int) Math.round((twelveBitData * 4096) / this.vref) - 1;
        rval = this.setOutputFast(twelveBit);
        return (rval);
    }

    public boolean setOutputFast(int twelveBitData) {
        boolean rval = false;
        int registerData = twelveBitData;
        String binaryString = Integer.toBinaryString(registerData & 0xff);
        String withLeadingZeros = String.format("0b%12s", binaryString).replace(' ', '0');
        if (this.chipIdle()) {
            byte[] data = new byte[Mcp472xConstants._MCP4725_SET_FAST_SIZE];
            data[0] = (byte) (data[0] | Mcp472xConstants._MCP4725_WRITE_CMD_FAST | Mcp472xConstants._MCP4725_PD_MODE_NORMAL);
            data[0] = (byte) (data[0] | (byte) ((registerData & 0x0f00) >> 8));
            data[1] = (byte) (registerData & 0x00ff);
            data[2] = data[0];
            data[3] = data[1];
            write_to_chip(data);
            rval = true;
        }
        return (rval);
    }


    boolean chipIdle() {
        boolean rval = false;
        int[] data;
        data = this.readBuffer(Mcp472xConstants._MCP4725_CHIP_READ_SIZE);
        if (Mcp472xConstants._MCP4725_CHIP_READ_SIZE == data.length) {
            if ((data[0] & Mcp472xConstants._MCP4725_READ_CMD_IS_COMPLT) > 0) {
                rval = true;
            }
        }
        return (rval);
    }


    /**
     * @param mSecs Time to sleep this thread
     */ // todo use materialize
    void sleepMS(long mSecs) {
        delay.setMillis(mSecs).materialize();

    }


    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    int[] readBuffer(int readLen) {
       int[] data = new int[readLen];
        byte[] readData = new byte[readLen];
        int rc = read_chip(readData);
        for (int i = 0; i < readLen; i++) {
            data[i] = (readData[i] & 0xff);
        }
        return (data);
    }



    abstract int read_chip(byte[] readData);

    abstract int write_to_chip(byte[] chipData);


}
