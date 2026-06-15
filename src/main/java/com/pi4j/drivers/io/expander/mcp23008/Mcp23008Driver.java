package com.pi4j.drivers.io.expander.mcp23008;

import com.pi4j.drivers.io.expander.AbstractConfigurableIoExpander;
import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.i2c.I2C;

/**
 * Driver for the MCP 23008 io expander.
 *
 * Contains the implementation of the abstract methods from AnbstractConfigurableIoExpander and the corresponding
 * initialization code.
 *
 * Additionally, this contains method to set all the configuration options and registers that are not covered
 * by the ConfigurableIoExpander interface.
 *
 * Note that many configuration calls set the values for all pins simultaneously; please set the corresponding
 * bits in the integer value accordingly; Java supports binary number representation using the "0b" prefix.
 * Alternatively, (1 << pin) can be used (where pin ranges from 0 to 7).
 *
 * Datasheet:
 * https://ww1.microchip.com/downloads/aemDocuments/documents/APID/ProductDocuments/DataSheets/MCP23008-MCP23S08-Data-Sheet-DS20001919.pdf
 */
public class Mcp23008Driver extends AbstractConfigurableIoExpander {

    /** Contains the bit mask for SEQOP (1<<5) used in the setIoConfiguration() call. */
    public static final int SEQOP = 1 << 5;
    /** Contains the bit mask for DISSLW (1<<4) used in the setIoConfiguration() call. */
    public static final int DISSLW = 1 << 4;
    /** Contains the bit mask for HAEN (1<<3) used in the setIoConfiguration() call. */
    public static final int HAEN = 1 << 3;
    /** Contains the bit mask for ODR (1<<2) used in the setIoConfiguration() call. */
    public static final int ODR = 1 << 2;
    /** Contains the bit mask for INTPOL (1<<1) used in the setIoConfiguration() call. */
    public static final int INTPOL = 1 << 1;

    protected final I2C i2c;

    public Mcp23008Driver(I2C i2c) {
        this(i2c, null);
    }

    /**
     * If the interrupt pin is provided, it is used to trigger interrupts for the individual input pin
     * abstractions available on this driver via getInput(pin).
     */
    public Mcp23008Driver(I2C i2c, ListenableOnOffRead<?> interruptPin) {
        this(i2c, 8, interruptPin);
    }

    protected Mcp23008Driver(I2C i2c, int size, ListenableOnOffRead<?> interruptPin) {
        super(size, interruptPin);
        this.i2c = i2c;
        if (interruptPin != null) {
            setInterruptModes((1 << size) - 1, InterruptMode.ON_CHANGE);
        }
        inputDirectionBits = readRegister(Register.IODIR);
        inputStates = outputStates = readInputsImpl();
    }

    /** Protected to allow the Mcp23017 driver to write a 16-bit value */
    protected void writeRegister(int register, int value) {
        i2c.writeRegister(register, value);
    }

    /** Protected to allow the Mcp23017 driver to write a 16-bit value */
    protected int readRegister(int register) {
        return i2c.readRegister(register);
    }

    /**
     * The INTF register reflects the interrupt condition on the PORT pins of any pin that is enabled for interrupts
     * via the GPINTEN register. A ‘set’ bit indicates that the associated pin caused the interrupt.
     */
    public int getInterruptFlags() {
        return readRegister(Register.INTF);
    }

    /**
     * The INTCAP register captures the GPIO port value at the time the interrupt occurred. The register is
     * ‘read-only’ and is updated only when an interrupt occurs. The register will remain unchanged until the
     * interrupt is cleared via a read of the 'INTCAP' or GPIO registers..
     */
    public int getInterruptCapture() {
        return readRegister(Register.INTCAP);
    }

    /**
     * The OLAT register provides access to the output latches. A read from this register results in a read of the
     * OLAT and not the port itself. A write to this register modifies the output latches that modify the pins
     * configured as outputs.
     */
    public int getOutputLatches() {
        return readRegister(Register.OLAT);
    }

    /**
     * Sets the pin polarity for all pins by writing to the "IPOL" register. If a bit is set, the corresponding GPIO
     * register will reflect the inverted value on the pin.
     */
    public void setInputPolarities(int pins) {
        writeRegister(Register.IPOL, pins);
    }

    /**
     * Sets the pin polarity for the given pin by updating the "IPOL" register. If a bit is set, the corresponding GPIO
     * register will reflect the inverted value on the pin.
     */
    public void setInputPolarity(int pin, boolean invert) {
        setInputPolarities(1 << pin, invert);
    }

    /**
     * Sets the pin polarities for all pins indicated by the given pin mask to the given value.
     */
    public void setInputPolarities(int pinMask, boolean invert) {
        int previous = getInputPolarities();
        setInputPolarities(invert ? previous | pinMask : previous & ~pinMask);
    }

    /**
      * Reads the pin polarity for all pins by reading the "IPOL" register. If a bit is set, the corresponding GPIO
      * register will reflect the inverted value on the pin.
      */
    public int getInputPolarities() {
        return readRegister(Register.IPOL);
    }

    /**
     * Sets the interrupt mode for the given pin; please refer to InterruptMode for a description of the modes.
     */
    public void setInterruptMode(int pin, InterruptMode mode) {
        setInterruptModes(1 << pin, mode);
    }

    /**
     * Sets the interrupt mode for multiple pins as indicated by the pin mask.
     * Please refer to InterruptMode for a description of the modes.
     */
    public void setInterruptModes(int pinMask, InterruptMode mode) {
        int interruptEnabled = readRegister(Register.GPINTEN);
        if (mode == InterruptMode.OFF) {
            writeRegister(Register.GPINTEN, interruptEnabled & ~pinMask);
        } else {
            writeRegister(Register.GPINTEN, interruptEnabled | pinMask);
            int interruptOnChange = readRegister(Register.INTCON);
            if (mode == InterruptMode.ON_CHANGE) {
                writeRegister(Register.INTCON, interruptOnChange & ~pinMask);
            } else {
                writeRegister(Register.INTCON, interruptOnChange | pinMask);
                int defaultValues = readRegister(Register.DEFVAL);
                if (mode == InterruptMode.ON_0) {
                    writeRegister(Register.DEFVAL, defaultValues | pinMask);
                } else {
                    writeRegister(Register.DEFVAL, defaultValues & ~pinMask);
                }
            }
        }
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
        writeRegister(Register.IOCON, config);
    }

    /** Returns the current IO configuration; please refer to setIoConfiguration for details. */
    public int getIoConfiguration() {
        return readRegister(Register.IOCON);
    }

    /**
     * The OLAT register provides access to the output latches. Writing to this register modifies the output latches
     * that modify the pins configured as outputs.
     */
    public void setOutputLatches(int bits) {
        writeRegister(Register.OLAT, bits);
    }

    /** Configures the output latch for the given pin to the given value */
    public void setOutputLatch(int pin, boolean value) {
        setOutputLatches(1 << pin, value);
    }

    /** Configures the output latch for all pins indicated by the given bit mask to the given value. */
    public void setOutputLatches(int pinMask, boolean value) {
        int previous = getOutputLatches();
        setOutputLatches(value ? previous | pinMask : previous & ~pinMask);
    }

    /**
     * Enables the pull-up resistors for the PORT pins. If a bit is set and the corresponding pin is
     * configured as an input, the corresponding PORT pin is internally pulled up with a 100 kOhm resistor
     */
    public void setPullupResistorConfigurations(int pins) {
        writeRegister(Register.GPPU, pins);
    }

    /**
     * Enables or disables the pull-up resistors for the given PORT pin. If enable is true
     * and the corresponding pin is configured as an input, the corresponding PORT pin is internally pulled
     * up with a 100 kOhm resistor
     */
    public void setPullupResistorConfiguration(int pin, boolean enable) {
        setPullupResistorConfigurations(1 << pin, enable);
    }

    /**
     * Enables or disables the pull-up resistors for the given set of PORT pins indicated by the pinMask.
     * For any pin in the mask, if enable is true and the corresponding pin is configured as an input,
     * the corresponding PORT pin is internally pulled up with a 100 kOhm resistor
     */
    public void setPullupResistorConfigurations(int pinMask, boolean enable) {
        int previous = getPullupResistorConfigurations();
        setPullupResistorConfigurations(enable ? previous | pinMask : previous & ~pinMask);
    }

    /**
     * Reads the pull-up resistor configuration. If a bit is set and the corresponding pin is
     * configured as an input, the corresponding PORT pin is internally pulled up with a 100 kOhm resistor
     */
    public int getPullupResistorConfigurations() {
        return readRegister(Register.GPPU);
    }

    @Override
    protected void writeOutputsImpl(int bits) {
        writeRegister(Register.GPIO, bits);
    }

    @Override
    protected int readInputsImpl() {
        return readRegister(Register.GPIO);
    }

    @Override
    public void setIoDirectionsImpl(int inputPins) {
        writeRegister(Register.IODIR, inputPins);
    }

    public enum InterruptMode {
        /** No interrupt */
        OFF,
        /** The interrupt pin is triggered if the value changes to 0. */
        ON_0,
        /** The interrupt pin is triggered if the value changes to 1. */
        ON_1,
        /** The interrupt pin is triggered if the value changes from the previous value. */
        ON_CHANGE
    }

}
