package com.pi4j.drivers.plc.pixtend;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiChipSelect;
import com.pi4j.io.spi.SpiConfig;
import com.pi4j.io.spi.SpiMode;

import java.io.Closeable;

/**
 * Driver for the PiXtend V2 (S/L) PLCs.
 * <p>
 * For communication and PWM mode details, please refer to chapter 12 of the official documentation:
 * https://www.kontron-electronics.com/fileadmin/media/produkte/board/Pixtend_V2_Software_Manual_EN.pdf
 */
public class PiXtendDriver implements Closeable {
    public static final int ANALOG_OUTPUT_COUNT = 2;

    // Constants for the PWM data block structure.
    private static final int PWM_BLOCK_SIZE = 7;
    private static final int PWM_X_CTRL_0 = 0;
    private static final int PWM_X_CTRL_1L = 1;
    private static final int PWM_X_CTRL_1H = 2;
    private static final int PWM_X_AL = 3;
    private static final int PWM_X_AH = 4;
    private static final int PWM_X_BL = 5;
    private static final int PWM_X_BH = 6;

    public final Model model;
    private final Context pi4J;

    private final Spi spi;
    private final Spi dacSpi;
    
    /**
     * Enables PiXtend SPI communication
     */
    private final DigitalOutput pin24DOut;

    /**
     * This array is read from the device on syncState(). All getters for input values read from this directly.
     */
    private final byte[] spiIn;

    /**
     * This array is sent to the device on syncState(). All setters for output values write to this array.
     * The only exceptions are:
     * <ul>
     *     <li>The GPIO mode, as the DHT11/DHT22 distinction is relevant for the API but not for the device.</li>
     *     <lu>The DAC output values, which are handled by a separate SPI device.</lu>
     * </ul>
     */
    private final byte[] spiOut;
    private final byte[] dacOut = new byte[]{0, 0, (byte) 0x80, 0};

    private final GpioMode[] gpioModes = {
            GpioMode.DIGITAL_INPUT, GpioMode.DIGITAL_INPUT,
            GpioMode.DIGITAL_INPUT, GpioMode.DIGITAL_INPUT};

    private long cycleTime = 30;
    private long timestamp;

    public PiXtendDriver(Context pi4J, Model model) {
        this.pi4J = pi4J;
        this.model = model;

        this.spiIn = new byte[model.bufferSize];
        this.spiOut = new byte[model.bufferSize];

        spiOut[0] = (byte) model.modelOut;

        for (int i = 0; i < model.pwmCount; i++) {
            setPwmXPrescaler(i, 1);
        }

        // Enable Pixtend SPI communication by enabling bwm pin 24
        pin24DOut = pi4J.create(DigitalOutputConfig.newBuilder(pi4J).bcm(24).build());
        pin24DOut.setState(true);

        SpiConfig spiConfig = Spi.newConfigBuilder(pi4J)
                .mode(SpiMode.MODE_0)
                .chipSelect(SpiChipSelect.CS_0)
                .baud(700_000)
                .build();

        spi = pi4J.create(spiConfig);
        spi.open();

        SpiConfig dacSpiConfig = Spi.newConfigBuilder(pi4J)
                .mode(SpiMode.MODE_0)
                .chipSelect(SpiChipSelect.CS_1)
                .baud(700_000)
                .build();
        dacSpi = pi4J.create(dacSpiConfig);
        dacSpi.open();

        timestamp = System.currentTimeMillis();
    }

    /**
     * Closes SPI and shuts down pin 24
     */
    @Override
    public void close() {
        spi.close();
        dacSpi.close();
        pi4J.shutdown(pin24DOut.id());
    }

    /**
     * Sends the current state to the device and receives an update.
     */
    public void syncState() {

        // Header checksum
        int headerChecksum = crc16(spiOut, 0, 7);
        // System.out.println("Header crc: " + headerChecksum);
        setWord(7, headerChecksum);

        //Calculate CRC16 Data Transmit Checksum
        int dataChecksum = crc16(spiOut, 9, spiOut.length - 2);
        setWord(spiOut.length - 2, dataChecksum);

        long currentCycle = System.currentTimeMillis() - timestamp;
        if (currentCycle < cycleTime) {
            try {
                Thread.sleep(cycleTime - currentCycle);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        int result = spi.transfer(spiOut, spiIn, spiOut.length);

        timestamp = System.currentTimeMillis();

        if (result != spiOut.length) {
            throw new IllegalStateException("Expected " + spiOut.length + " bytes; got: " + result);
        }

        int receivedHeaderChecksum = getWord(7);
        int calculatedHeaderChecksum = crc16(spiIn, 0, 7);
        if (receivedHeaderChecksum != calculatedHeaderChecksum) {
            throw new IllegalStateException("Received header checksum " + receivedHeaderChecksum + " != calculated checksum " + calculatedHeaderChecksum);
        }

        int errorCode = getNibble(3, 1);
        switch (errorCode) {
            case 0b0010:
                throw new IllegalStateException("Data CRC Error");
            case 0b0011:
                throw new IllegalStateException("Data block too short");
            case 0b0100:
                throw new IllegalStateException("PiXtend Model mismatch");
            case 0b0101:
                throw new IllegalStateException("Header CRC Error");
            case 0b0110:
                throw new IllegalStateException("SPI frequency too high");
        }

        int receivedDataChecksum = getWord(spiIn.length - 2);
        int calculatedDataChecksum = crc16(spiIn, 9, spiIn.length - 2);
        if (receivedDataChecksum != calculatedDataChecksum) {
            throw new IllegalStateException("Received data checksum " + receivedDataChecksum + " != calculated checksum " + calculatedDataChecksum);
        }

        dacSpi.write(dacOut);
    }

    /**
     * Returns the 10V-range voltage for analog in 0..3 and a mA value for analog in 4 and 5
     */
    public double getAnalogIn(int index) {
        return (index > 4)
                ? getRawAnalogIn(index) * 0.020158400229358
                : getAnalogIn(index, false);
    }

    /**
     * Returns the 10/5V range voltage for analog in 0..3. Analog 4/5 are not supported in this call.
     */
    public double getAnalogIn(int index, boolean limitedTo5v) {
        if (index > 4) {
            throw new IllegalArgumentException("limitedTo5v argument not supported for analog in 4/5");
        }
        return getRawAnalogIn(index) * (limitedTo5v ? 5.0 : 10.0) / 1024.0;
    }

    public boolean getDigitalIn(int index) {
        if (index < 0 || index >= model.digitalInCount) {
            throw new IllegalArgumentException("Digital input index " + index + " out of range 0.." + (model.digitalInCount - 1));
        }
        return getBit(model.digitalInOffset, index);
    }

    public boolean getGpioIn(int index) {
        checkRange(index, model.gpioCount, "GPIO input");
        if (gpioModes[index] != GpioMode.DIGITAL_INPUT) {
            throw new IllegalStateException("GpioMode " + index + " is set to " + gpioModes[index]);
        }
        return getBit(model.gpioInOffset, index);
    }

    public GpioMode getGpioMode(int index) {
        checkRange(index, model.gpioCount, "GPIO");
        return gpioModes[index];
    }

    public double getTemperature(int index) {
        checkRange(index, model.tempHumidCount, "Temperature");
        int rawValue = getWord(model.tempHumidOffset + index * 4);
        return switch (gpioModes[index]) {
            case DHT11 -> rawValue / 256.0;
            case DHT22 -> rawValue / 10.0;
            default -> throw new IllegalStateException("GPIO " + index + " not configured for DHT11/22");
        };
    }

    public double getHumidity(int index) {
        checkRange(index, model.tempHumidCount, "Humidity");
        int rawValue = getWord(model.tempHumidOffset + index * 4 + 2);
        return switch (gpioModes[index]) {
            case DHT11 -> rawValue / 256.0;
            case DHT22 -> rawValue / 10.0;
            default -> throw new IllegalStateException("GPIO " + index + " not configured for DHT11/22");
        };
    }

    /**
     * Returns the "raw" analog input value in a 10 bit range [0..1023].
     */
    public int getRawAnalogIn(int index) {
        checkRange(index, model.analogInCount, "Analog input");
        return getWord(model.analogInOffset + 2 * index);
    }

    /**
     * Sets the debounce value for the given digital input. Note that debounce values always cover two inputs,
     * i.e. setting the value for din 0 or 1 will always set the value for both inputs.
     */
    public void setDigitalInDebounce(int index, int value) {
        checkRange(index, model.digitalInCount, "Digital input");
        spiOut[model.digitalInDebounceOffset + index / 2] = (byte) Math.max(0, Math.min(value, 255));
    }

    public void setDigitalOut(int index, boolean value) {
        checkRange(index, model.digitalOutCount, "Digital output");
        setBit(model.digitalOutOffset, index, value);
    }

    public void setGpioMode(int index, GpioMode mode) {
        checkRange(index, model.gpioCount, "GPIO mode");
        switch (mode) {
            case DHT11:
            case DHT22:
                setBit(model.gpioCtrlOffset, index, false);
                setBit(model.gpioCtrlOffset, index + 4, true);
                break;
            case DIGITAL_INPUT:
                setBit(model.gpioCtrlOffset, index, false);
                setBit(model.gpioCtrlOffset, index + 4, false);
                break;
            case DIGITAL_OUTPUT:
                setBit(model.gpioCtrlOffset, index, true);
                setBit(model.gpioCtrlOffset, index + 4, false);
        }
        gpioModes[index] = mode;
    }

    public void setPwmXa(int x, int value) {
        setWord(pwmAddress(x, PWM_X_AL), value);
    }

    public void setPwmXb(int x, int value) {
        setWord(pwmAddress(x, PWM_X_BL), value);
    }

    public void setPwmXaEnabled(int x, boolean enabled) {
        setBit(pwmAddress(x, PWM_X_CTRL_0), 3, enabled);
    }

    public void setPwmXbEnabled(int x, boolean enabled) {
        setBit(pwmAddress(x, PWM_X_CTRL_0), 3, enabled);
    }

    public void setPwmXPrescaler(int x, int value) {
        int address = pwmAddress(x, PWM_X_CTRL_0);
        spiOut[address] = (byte) ((spiOut[address] & 0b00011011) | (value << 5));
    }

    public void setPwmXMode(int x, PwmMode mode) {
        int address = pwmAddress(x, PWM_X_CTRL_0);
        spiOut[address] = (byte) ((spiOut[address] & 0b11111000) | mode.ordinal());
    }

    public void setPwmXCtrl1(int x, int value) {
        setWord(pwmAddress(x, PWM_X_CTRL_1L), value);
    }

    public void setGpioOut(int index, boolean value) {
        checkRange(index, model.gpioCount, "GPIO output");
        if (gpioModes[index] != GpioMode.DIGITAL_OUTPUT) {
            throw new IllegalStateException("GpioMode " + index + " is set to " + gpioModes[index]);
        }
        setBit(model.gpioOutOffset, index, value);
    }

    public void setRelay(int index, boolean value) {
        checkRange(index, model.relayOutCount, "Relay");
        setBit(model.relayOutOffset, index, value);
    }

    public void setAnalogOutEnabled(int index, boolean value) {
        checkRange(index, ANALOG_OUTPUT_COUNT, "Analog output");
        dacOut[index * 2] = (byte) (dacOut[index * 2 + 1] & ~0x10 | (value ? 0x10 : 0));
    }

    /**
     * Sets the analog output voltage as a double value between 10.0 and 0
     */
    public void setAnalogOut(int index, double value) {
        setRawAnalogOut(index, (int) Math.round(1023.0 * value / 10.0));
    }

    /**
     * Sets the analog output voltage as an integer between 0 and 1023.
     * Larger or smaller values will be clamped accordingly.
     */
    public void setRawAnalogOut(int index, int value) {
        checkRange(index, ANALOG_OUTPUT_COUNT, "Analog output");
        if (value > 1023) {
            value = 1023;
        } else if (value < 0) {
            value = 0;
        }

        // High 4 bits are in the bottom nibble of the high byte
        dacOut[index * 2] = (byte) (dacOut[index * 2] & 0xf0 | (value >>> 6));
        // Low 6 bits are in the upper 6 bits of the low byte.
        dacOut[index * 2 + 1] = (byte) (value << 2);
    }

    // Private helpers

    private static void checkRange(int index, int count, String name) {
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException(name + " index " + index + " out of range 0.." + (count - 1));
        }
    }

    private static int crc16(byte[] data, int from, int toExclusive) {
        int crc = 0xFFFF;
        for (int i = from; i < toExclusive; i++) {
            crc = crc16(crc, data[i]);
        }
        return crc;
    }

    private static int crc16(int crc, byte data) {
        crc ^= (data & 0xFF);
        for (int i = 0; i < 8; i++) {
            crc = (crc & 1) != 0 ? (crc >>> 1) ^ 0xa001 : (crc >>> 1);
        }
        return crc;
    }

    private int pwmAddress(int index, int offset) {
        checkRange(index, model.pwmCount, "PWM address");
        return model.pwmOffset + index * PWM_BLOCK_SIZE + offset;
    }

    private boolean getBit(int baseAddress, int bitIndex) {
        int address = baseAddress + bitIndex / 8;
        return (spiIn[address] & (1 << (bitIndex % 8))) != 0;
    }

    private int getNibble(int baseAddress, int index) {
        int address = baseAddress + index / 2;
        return (spiIn[address] >>> ((index % 2) * 4)) & 0x0f;
    }

    private int getWord(int address) {
        return (spiIn[address] & 0xff) | ((spiIn[address + 1] & 0xff) << 8);
    }

    private void setBit(int baseAddress, int bitIndex, boolean value) {
        int address = baseAddress + bitIndex / 8;
        int mask = ~(1 << (bitIndex % 8));
        int bitValue = value ? 1 << bitIndex : 0;
        spiOut[address] = (byte) ((spiOut[address] & mask) | bitValue);
    }

    private void setNibble(int baseAddress, int index, int value) {
        int address = baseAddress + index / 2;
        int oldValue = spiIn[address] & 255;
        spiOut[address] = (byte) ((index & 1) == 0
                ? ((oldValue & 0xf0) | (value & 0x0f))
                : ((oldValue & 0x0f) | ((value & 0x0f) << 4)));
    }

    private void setWord(int address, int word) {
        spiOut[address] = (byte) word;
        spiOut[address + 1] = (byte) (word >>> 8);
    }

    /**
     * Enumeration of supported Pixtend models, containing their capabilities and IO record structure / offsets.
     */
    public enum Model {
        V2S(    /* modelOut */ 'S',
                /* bufferSize */ 67,
                /* digitalInOffset */ 9,
                /* digitalInCount */ 8,
                /* analogInOffset */ 10,
                /* analogInCount */ 2,
                /* gpioInOffset */ 14,
                /* gpioCount */ 4,
                /* tempHumidOffset */ 15,
                /* tempHumidCount */ 4,
                /* retainDataOffset */ 33,
                /* retainDataCount */ 32,
                /* digitalInDebounceOffset */ 9,
                /* digitalOutOffset */ 13,
                /* digitalOutCount */ 4,
                /* relayOutOffset */ 14,
                /* relayOutCount */ 4,
                /* gpioCtrlOffset */ 15,
                /* gpioOutOffset */ 16,
                /* gpioDebounceOffset */ 17,
                /* pwmOffset */ 19,
                /* pwmCount */ 2),

        V2L(    /* modelOut */ 'L',
                /* bufferSized */ 111,
                /* digitalInOffset */ 9,
                /* digitalInCount */ 16,
                /* analogInOffset */ 11,
                /* analogInCount */ 6,
                /* gpioInOffset */ 23,
                /* gpioCount */ 4,
                /* tempHumidOffset */ 24,
                /* tempHumidCount */ 4,
                /* retainDataOffset */ 45,
                /* retainDataCount */ 64,
                /* digitalInDebounceOffset */ 8,
                /* digitalOutOffset */ 17,
                /* digitalOutCount */ 12,
                /* relayOutOffset */ 19,
                /* relayOutCount */ 4,
                /* gpioCtrlOffset */ 20,
                /* gpioOutOffset */ 21,
                /* gpioDebounceOffset */ 22,
                /* pwmOffset */ 24,
                /* pwmCount */ 3);

        public final char modelOut;
        public final int bufferSize;
        public final int digitalInOffset;
        public final int digitalInCount;
        public final int analogInOffset;
        public final int analogInCount;
        public final int gpioInOffset;
        public final int gpioCount;
        public final int tempHumidOffset;
        public final int tempHumidCount;
        public final int retainDataOffset;
        public final int retainDataCount;
        public final int digitalInDebounceOffset;
        public final int digitalOutOffset;
        public final int digitalOutCount;
        public final int relayOutOffset;
        public final int relayOutCount;
        public final int gpioCtrlOffset;
        public final int gpioOutOffset;
        public final int gpioDebounceOffset;
        public final int pwmOffset;
        public final int pwmCount;

        Model(char modelOut,
              int bufferSize,
              int digitalInOffset,
              int digitalInCount,
              int analogInOffset,
              int analogInCount,
              int gpioInOffset,
              int gpioCount,
              int tempHumidOffset,
              int tempHumidCount,
              int retainDataOffset,
              int retainDataCount,
              int digitalInDebounceOffset,
              int digitalOutOffset,
              int digitalOutCount,
              int relayOutOffset,
              int relayOutCount,
              int gpioCtrlOffset,
              int gpioOutOffset,
              int gpioDebounceOffset,
              int pwmOffset,
              int pwmCount) {
            this.modelOut = modelOut;
            this.bufferSize = bufferSize;
            this.digitalInOffset = digitalInOffset;
            this.digitalInCount = digitalInCount;
            this.analogInOffset = analogInOffset;
            this.analogInCount = analogInCount;
            this.gpioInOffset = gpioInOffset;
            this.gpioCount = gpioCount;
            this.tempHumidOffset = tempHumidOffset;
            this.tempHumidCount = tempHumidCount;
            this.retainDataOffset = retainDataOffset;
            this.retainDataCount = retainDataCount;
            this.digitalInDebounceOffset = digitalInDebounceOffset;
            this.digitalOutOffset = digitalOutOffset;
            this.digitalOutCount = digitalOutCount;
            this.relayOutOffset = relayOutOffset;
            this.relayOutCount = relayOutCount;
            this.gpioCtrlOffset = gpioCtrlOffset;
            this.gpioOutOffset = gpioOutOffset;
            this.gpioDebounceOffset = gpioDebounceOffset;
            this.pwmOffset = pwmOffset;
            this.pwmCount = pwmCount;
        }
    }

    public enum GpioMode {
        DIGITAL_INPUT,
        DIGITAL_OUTPUT,
        DHT11,
        DHT22
    }

    public enum PwmMode {
        SERVO, DUTY_CYCLE, UNIVERSAL, FREQUENCY
    }
}
