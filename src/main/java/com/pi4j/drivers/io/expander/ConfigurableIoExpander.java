package com.pi4j.drivers.io.expander;

/** An IO expander where the direction (input or output) is configurable. */
public interface ConfigurableIoExpander extends InputExpander, OutputExpander {

    /** Sets the IO direction for the given pin number (0..size). */
    default void setIoDirection(int pin, Direction direction) {
        setIoDirections(1 << pin, direction);
    }


    /**
     * Sets the IO direction of multiple pins at once, as indicated by the given pin mask. If the corresponding
     * bit is 1, the given direction is applied. Other pins remain unchanged.
     *
     * Changing pins to output will send the corresponding state bit to the chip. Changing pins to
     * input will read the corresponding state bits from the chip without triggering any events or
     * updating any other pins. Unchanged pins will not be affected, even if they are covered by the mask.
     */
    void setIoDirections(int pinMask, Direction direction) {

    enum Direction {
        INPUT, OUTPUT
    }
}