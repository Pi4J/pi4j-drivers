package com.pi4j.drivers.io.da.mcp472x;

import com.pi4j.drivers.io.da.DigitalAnalogConverter;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent;
import com.pi4j.io.gpio.digital.DigitalStateChangeListener;
import com.pi4j.io.i2c.I2C;
import com.pi4j.util.Delay;


/**
 * Driver for MCP4728 DAC.
 *
 * <p>
 *The MCP4728 ships with device address 0x60. To set other address the DAC config registers
 * must be updated. The update process requires the LDAC pin be toggled at a specific point
 * in the I2C command as it is clocked into the chip. This GPIO control within the I2C
 * traffic is not possible with Pi4j.  To accomplish this would require ordering the chip with
 * the address programmed, or use of an MCU.
 * </p>
 * @see <a href="https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/22187E.pdf">MCP4728</a>
 * 
 */

public class Mcp4728Driver implements DigitalAnalogConverter {

    private final I2C i2c;
    private final DigitalInput readyPin;
    private final double vdd;
    private final Delay delay = new Delay();
    private final byte[] ioBuffer = new byte[Math.max(Constants.MCP4728_SET_FAST_SIZE,
            Math.max(Constants.MCP4728_SET_EEPROM_SIZE, Constants.MCP4728_CHIP_READ_SIZE))];

    private  RdyBsyDataInGpioListener idlePin = null;
    private boolean eepromEnabled;

    public Mcp4728Driver(I2C i2cHw, DigitalInput readyPin, double vdd) {
        this.i2c = i2cHw;
        this.readyPin = readyPin;
        this.vdd = vdd;
        idlePin = new RdyBsyDataInGpioListener();
        readyPin.addListener(idlePin);
    }

    /**
     * Write reset command into chip
     * Result: chip POR and EEPROM loaded
     * @param resetI2c    I2C of implementation.SMB
     */
    public void resetChip(I2C resetI2c) {
        resetI2c.write(Constants.MCP4728_GEN_CALL_RESET_CMD);
        delay.setMillis(4).materialize();
    }


    /**
     *
     * @param channel
     * @param vrefValue   zero input sets Gx to 0, non zero sets vref to 1
     */
    public void setVref(int channel, int vrefValue){

        byte[] allRegs = materializeDacRegs();
        int vrefA =  (allRegs[ 1 ] & 0x80) >> 7;
        int vrefB =  (allRegs[ 1 + 6] & 0x80) >> 7;
        int vrefC =  (allRegs[ 1 + 12] & 0x80) >> 7;
        int vrefD =  (allRegs[ 1 + 18] & 0x80) >> 7;
        byte vrefBit = 1;
        ioBuffer[0] = (byte) (Constants.MCP4728_WRITE_VREF_CMD_DAC |  (vrefA << 3) | (vrefB << 2) | (vrefC << 1) | vrefD);
        ioBuffer[0] = (byte) ((vrefValue > 0) ? (ioBuffer[0] | (vrefBit << (3 - channel))) : (ioBuffer[0] & ~(vrefBit << (3 -channel)) ) );
        this.i2c.write(ioBuffer, 0, Constants.MCP4728_SET_VREF_SIZE);
    }

    /**
     *
     * @param channel
     * @param gainValue   zero input sets Gx to 0, non zero  sets Gx to 1
     */
    public void setGain(int channel, int gainValue){
        byte[] allRegs = materializeDacRegs();
        int gainA =  (allRegs[ 1 ] & 0x10) >> 4;
        int gainB =  (allRegs[ 1 + 6] & 0x10) >> 4;
        int gainC =  (allRegs[ 1 + 12] & 0x10) >> 4;
        int gainD =  (allRegs[ 1 + 18] & 0x10) >> 4;
        byte gainBit = 1;
        ioBuffer[0] = (byte) (Constants.MCP4728_WRITE_GAIN_CMD_DAC | (gainA << 3) | (gainB << 2) | (gainC << 1) | gainD);
        ioBuffer[0] = (byte) ((gainValue > 0) ? (ioBuffer[0] | (gainBit << (3 - channel))) :  (ioBuffer[0] & ~(gainBit << (3 -channel))) ) ;;
        this.i2c.write(ioBuffer, 0, Constants.MCP4728_SET_GAIN_SIZE);
    }


    /**
     * Contents of DAC config registers written to the EEPROM
     */
    public void syncDACToEEPROM(){
        byte[] allRegs = materializeDacRegs();
        for(int c = 0; c <4 ; c++) {
            ioBuffer[0] = (byte) (Constants.MCP4728_SINGL_WRITE_CMD_DAC_EEPROM | (c << 1 ) | (allRegs[c * 6] & 0x01));
            ioBuffer[1] =  (byte)((allRegs[1 + c * 6]) );
            ioBuffer[2] = (byte) ((allRegs[2 + c * 6]) );
            this.i2c.write(ioBuffer, 0, Constants.MCP4728_SYNC_EEPROM_SIZE);
            chipIdle(); // will return when the chip is again idle
        }
    }

    /**
     * Note that the simple call (setVoltage(double value)) is available via the interface default method.
     * This will block until the EEPROM write has completed
     */
    @Override
    public void setVoltage(int outputChannel, double voutValue) {
        if (outputChannel > 3) {
            throw new IllegalArgumentException("This chip has only 4 channels  A B C D ");
        }
        byte[] allRegs = materializeDacRegs();
        // Second byte returned contains vref and fx bits
        int configVrefBit =  ((allRegs[1 + outputChannel*6]  & 0x80) > 0 ? 1 : 0);
        int configGainfBit = ((allRegs[1 + outputChannel*6]  & 0x10) > 0 ? 1 : 0);;
        if (configVrefBit == 1) {
            int gain = (configGainfBit == 0) ? 1 : 2;
            setDigitalValueDACEEPROM(outputChannel, (int) ((voutValue * 4096) / (2.048 * gain)));
        } else {
            setDigitalValueDACEEPROM(outputChannel, (int) ((voutValue * 4096) / vdd));
        }
    }


    public byte[] materializeDacRegs() {
        byte[] dacRegs = new byte[24];
        i2c.read(dacRegs);
        return dacRegs;
    }


    public String materializeDacDescription() {
        delay.setMillis(8).materialize();   // allow time for EEPROM update to complete
        byte[] dacRegs = new byte[24];
        i2c.read(dacRegs);
        StringBuilder total = new StringBuilder("\n");
        for (int i = 0; i < 8; i++) {
            String firstByteReg = String.format("%8s",  Integer.toBinaryString(dacRegs[i +   (i*2)] & 0xFF)).replace(' ', '0');
            String secondByteReg = String.format("%8s", Integer.toBinaryString(dacRegs[i+1 + (i*2)] & 0xFF)).replace(' ', '0');
            String thirdByteReg = String.format("%8s",  Integer.toBinaryString(dacRegs[i+2 + (i*2)] & 0xFF)).replace(' ', '0');
            total.append(chanlData[i] + " " + firstByteReg + " " + secondByteReg + " " + thirdByteReg + "\n\n");
        }
        return total.toString();
    }

String[] chanlData = {"Chnl A REG   ", "Chnl A EEPROM","Chnl B REG   ","Chnl B EEPROM","Chnl C REG   ",
    "Chnl C EEPROM","Chnl D REG   ","Chnl D EEPROM"};




    @Override
    public int getChannelCount() {
        return 4;
    }

    /**
     * Writes a 12 bit value to the chip. Blocks if the chip DAC.
     */
    public void setDigitalValueDAC(int outputChannel, int digitalValue) {
        if (outputChannel > 3) {
            throw new IllegalArgumentException("This chip has only channel 4");
        }
        if (digitalValue < 0 || digitalValue > 4095) {
            throw new IllegalArgumentException("Value out of range (0...4095): " + digitalValue);
        }
       //  DAC1    DAC0     Channels
        //  0       0       Ch. A - Ch. D       0
        //  0       1       Ch. B - Ch. D       1
        //  1       0       Ch. C - Ch. D       2
        //  1       1       Ch. D               3


        //  c2 c1 c0 w1 w2 DAC1 DAC0 UDAC
        //   o  1  o  1  1
        //         P25    0 = External VDD, 1 = Internal VREF (2.048V)
        //            Gain Select: 0 = Gain of 1, 1 = Gain of 2.
        //  VREF PD1 PD0 Gx D11 D10 D9 D8
        //        D7 D6 D5 D4 D3 D2 D1 D0

        byte[] allRegs = new byte[Constants.MCP4728_DAC_EEPROM_DATA_SZ];
        allRegs = materializeDacRegs();

        ioBuffer[0] = (byte) (Constants.MCP4728_WRITE_CMD_DAC | (outputChannel << 1 ) | (allRegs[1 + outputChannel*6] & 0x01));
        ioBuffer[1] = (byte) (byte)((allRegs[ 1 + outputChannel*6] & 0xF0) | ((digitalValue & 0xf00) >> 8));
        ioBuffer[2] = (byte) (digitalValue);
        ioBuffer[3] =  ioBuffer[0];
        ioBuffer[4] =  ioBuffer[1];
        ioBuffer[5] =  ioBuffer[2];
        this.i2c.write(ioBuffer, 0, Constants.MCP4728_SET_FAST_SIZE);
    }

    /**
     * Writes a 12 bit value to the chip. Blocks if the chip DAC and EEPROM , blocks until EEPROM write completes .
     */
    public void setDigitalValueDACEEPROM(int outputChannel, int digitalValue) {
        if (outputChannel > 3) {
            throw new IllegalArgumentException("This chip has only channel 4");
        }
        if (digitalValue < 0 || digitalValue > 4095) {
            throw new IllegalArgumentException("Value out of range (0...4095): " + digitalValue);
        }
        //  DAC1    DAC0     Channels
        //  0       0       Ch. A - Ch. D       0
        //  0       1       Ch. B - Ch. D       1
        //  1       0       Ch. C - Ch. D       2
        //  1       1       Ch. D               3


        //  c2 c1 c0 w1 w2 DAC1 DAC0 UDAC
        //   o  1  o  1  1
        //         P25    0 = External VDD, 1 = Internal VREF (2.048V)
        //            Gain Select: 0 = Gain of 1, 1 = Gain of 2.
        //  VREF PD1 PD0 Gx D11 D10 D9 D8
        //        D7 D6 D5 D4 D3 D2 D1 D0

        byte[] allRegs = new byte[Constants.MCP4728_DAC_EEPROM_DATA_SZ];
        allRegs = materializeDacRegs();

        ioBuffer[0] = (byte) (Constants.MCP4728_SINGL_WRITE_CMD_DAC_EEPROM | (outputChannel << 1 ) | (allRegs[1 + outputChannel*6] & 0x01));
        ioBuffer[1] =  (byte)((allRegs[1 + outputChannel*6] & 0xF0) | ((digitalValue & 0xf00) >> 8));
        ioBuffer[2] = (byte) (digitalValue);
        this.i2c.write(ioBuffer, 0, Constants.MCP4728_SET_EEPROM_SIZE);
        chipIdle(); // will return when the chip is again idle
    }


    /**
     * Returns true if the chip is ready (idle), else false.
     */
    public boolean chipIdle() {
        do{
            delay.setMillis(100).materialize();
        } while (! idlePin.isChipIdle() );
        return true;
    }




    // The mcp4728 chip signals the EEPROM update has completed by driving the Read/Busy pin HIGH
       private static class RdyBsyDataInGpioListener implements DigitalStateChangeListener {
        boolean chipIsIdle = true;

        public RdyBsyDataInGpioListener() { }

        @Override
        public void onDigitalStateChange(DigitalStateChangeEvent event) {
            if (event.state() == DigitalState.LOW) {
                chipIsIdle = false;
            } else if (event.state() == DigitalState.HIGH) {
                chipIsIdle = true;
            }else {
                System.out.println("Unexpected EVEN !!!!");
            }
        }

        public boolean isChipIdle() { return chipIsIdle; }


    }



}
