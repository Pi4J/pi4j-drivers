package com.pi4j.drivers.io.expander;

/** An IO expander where the direction (input or output) is configurable. */
interface ConfigurableIoExpander implements InputExpander, OutputExpander {

    /** Sets the IO direction for the given pin number. */
    default void setIoDirection(int pin, Direction direction) {
        setIoDirections(1 << pin, direction);
    }

    /**
     * Sets the IO direction of multiple pins at once, as indicated by the given pin mask. If the corresponding
     * bit is 1, the given direction is applied. Other pins remain unchanged.
     */
    void setIoDirections(int pinMask, Direction direction);

    enum Direction {
        INPUT, OUTPUT
    }
}