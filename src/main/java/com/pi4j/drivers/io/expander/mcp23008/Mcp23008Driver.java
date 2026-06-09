package com.pi4j.drivers.io.expander.mcp23008;

import com.pi4j.drivers.io.expander.OutputExpander;
import com.pi4j.io.OnOffWrite;
import com.pi4j.io.exception.IOException;
import com.pi4j.io.i2c.I2C;

/**
 * Driver for the MCP 23008 io expander. Supports output only currently.
 *
 * Note that most configuration calls set the values for all pins simultaneously; please set the corresponding
 * bits in the integer value accordingly; Java supports binary number representation usin a leading 0b. Alternatively,
 * (1 << pin) can be used (where pin ranges from 0 to 7).
 *
 * Datasheet:
 * https://ww1.microchip.com/downloads/aemDocuments/documents/APID/ProductDocuments/DataSheets/MCP23008-MCP23S08-Data-Sheet-DS20001919.pdf
 */
public class Mcp23008Driver implements OutputExpander {

    /** Contains the bit mask for SEQOP (1<<5) used in the setIoConfiguration() call. */
    public final static int SEQOP = 1 << 5;
    /** Contains the bit mask for DISSLW (1<<4) used in the setIoConfiguration() call. */
    public final static int DISSLW = 1 << 4;
    /** Contains the bit mask for HAEN (1<<3) used in the setIoConfiguration() call. */
    public final static int HAEN = 1 << 3;
    /** Contains the bit mask for ODR (1<<2) used in the setIoConfiguration() call. */
    public final static int ODR = 1 << 2;
    /** Contains the bit mask for INTPOL (1<<1) used in the setIoConfiguration() call. */
    public final static int INTPOL = 1 << 1;

    private final I2C i2c;
    private final OnOffWrite<?>[] onOffWriteArray = new OnOffWrite[8];

    private int outputBits = 0x0;
    private int triggerMask = -1;
    private int ioDir = -1;

    public Mcp23008Driver(I2C i2c) {
        this.i2c = i2c;
        for (int i = 0; i < 8; i++) {
            onOffWriteArray[i] = new OnOffWriteImpl(i);
        }
    }

    /**
     * The INTF register reflects the interrupt condition on the PORT pins of any pin that is enabled for interrupts
     * via the GPINTEN register. A ‘set’ bit indicates that the associated pin caused the interrupt.
     */
    public int getInterruptFlags() {
        return i2c.readRegister(Register.INTF);
    }

    /**
     * The INTCAP register captures the GPIO port value at the time the interrupt occurred. The register is
     * ‘read-only’ and is updated only when an interrupt occurs. The register will remain unchanged until the
     * interrupt is cleared via a read of the 'INTCAP' or GPIO registers..
     */
    public int getInterruptCapture() {
        return i2c.readRegister(Register.INTCAP);
    }

    /**
     * The OLAT register provides access to the output latches. A read from this register results in a read of the
     * OLAT and not the port itself. A write to this register modifies the output latches that modify the pins
     * configured as outputs.
     */
    public int getOutputLatches() {
        return i2c.readRegister(Register.OLAT);
    }

    /**
     * Sets the pin polarity for all pins by writing to the "IPOL" register. If a bit is set, the corresponding GPIO
     * register will reflect the inverted value on the pin.
     */
    public void setInputPolarity(int pins) {
        i2c.writeRegister(Register.IPOL, pins);
    }

    /**
     * If a bit is set, the corresponding pin is enabled for interrupt-on-change by writing to the "GPINTEN" chip
     * register. The "DEFVAL" and "INTCON" registers must also be configured if any pins are enabled for
     * interrupt-on-change.
     */
    public void setInterruptOnChange(int pins) {
        i2c.writeRegister(Register.GPINTEN, pins);
    }

    /**
     * Sets the default comparison value for the associated pin by writing to the "DEFVAL" register.
     * If enabled via the "GPINTEN" and "INTCON" registers to compare against the "DEFVAL" register, only an
     * opposite value on the associated pin will cause an interrupt to occur.
     */
    public void setDefaultValues(int pins) {
       i2c.writeRegister(Register.DEFVAL, pins);
    }

    /**
     * Writes to the "IOCON" register, managing several bits for configuring the device:
     *
     * <ul>
     * <li>Bit 5 (SEQOP) controls the incrementing function of the Address Pointer. If
     *     the Address Pointer is disabled, the Address Pointer does not automatically increment after each byte is
     *     clocked during a serial transfer. This feature is useful when it is desired to continuously
     *     poll (read) or modify (write) a register. Note that this will requiring manual access to this chip,
     *     bypassing this driver.
     * <li>Bit 4 (DISSLW) Controls the slew rate function on the SDA pin. If enabled, the SDA
     *     slew rate will be controlled when driving from a high to a low.
     * <li>Bit 3 (HAEN) enables/disables the hardware address pins (A1, A0) on the MCP23S08. This bit is not used on the
     *     MCP23008. The address pins are always enabled on the MCP23008.
     * <li>Bit 2 (ODR) enables/disables the INT pin for open-drain configuration.
     * <li>Bit 1 (INTPOL) sets the polarity of the INT pin. This bit is functional
     *     only when the ODR bit is cleared, configuring the INT pin as active push-pull
     * </ul>
     *
     * Bits 0 and 6-7 are unused and should be set to 0.
     */
    public void setIoConfiguration(int config) {
        i2c.writeRegister(Register.IOCON, config);
    }

    /**
     * Controls how the associated pin value is compared for the interrupt-on-change feature by writing to the
     * "INTCON" register. If a bit is set, the corresponding I/O pin is compared against the associated bit in the
     * DEFVAL register. If a bit value is clear, the corresponding I/O pin is compared against the previous value.
     */
    public void setInterruptControl(int pins) {
        i2c.writeRegister(Register.INTCON, pins);
    }

    /**
     * The OLAT register provides access to the output latches. A read from this register results in a read of the
     * OLAT and not the port itself. A write to this register modifies the output latches that modify the pins
     * configured as outputs.
     */
    public void setOutputLatches(int bits) {
        return i2c.writeRegister(Register.OLAT, bits);
    }

    /**
     * Enables the pull-up resistors for the PORT pins. If a bit is set and the corresponding pin is
     * configured as an input, the corresponding PORT pin is internally pulled up with a 100 kOhm resistor
     */
    public void setPullupResistorConfiguration(int pins) {
        i2c.writeRegister(Register.GPPU, pins);
    }


    @Override
    public void setOutputTriggerMask(int mask) {
        this.triggerMask = mask;
    }

    /**
     * Set each bit to 0 for output and 1 for input to configure the corresponding pin by writing to the "IODIR"
     * register.
     */
    public void setIoDir(int ioDir) {
        this.ioDir = ioDir;
        i2c.writeRegister(Register.IODIR, ioDir);
    }

    @Override
    public void setOutputState(int index, boolean state) {
        int mask = 1 << index;
        if ((ioDir & mask) != 0) {
            throw new IllegalStateException("Pin " + index + " is configured for output.");
        }
        if (state) {
            setOutputState(outputBits | mask);
        } else {
            setOutputState(outputBits & ~mask);
        }
    }

    @Override
    public void setOutputState(int bits) {
        int changedBits = outputBits ^ bits;
        outputBits = bits;
        if ((changedBits & triggerMask) != 0) {
            this.i2c.writeRegister(Register.GPIO, outputBits);
        }
    }

    @Override
    public OnOffWrite<?> getOutput(int index) {
        return onOffWriteArray[index];
    }

    private class OnOffWriteImpl implements OnOffWrite<OnOffWriteImpl> {
        final int index;

        OnOffWriteImpl(int index) {
            this.index = index;
        }

        @Override
        public OnOffWriteImpl on() throws IOException {
            setOutputState(index, true);
            return this;
        }

        @Override
        public OnOffWriteImpl off() throws IOException {
            setOutputState(index, false);
            return this;
        }
    }
}
