package com.pi4j.drivers.display.graphics.console;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.graphics.AbstractGraphicsDisplayDriverTest;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import org.junit.jupiter.api.Assumptions;

public class ConsoleGraphicsDriverTest extends AbstractGraphicsDisplayDriverTest {
    @Override
    public GraphicsDisplayDriver createDriver(Context pi4j) {
        try {
            return new ConsoleGraphicsDriver(64, 32, true);
        } catch (RuntimeException e) {
            // TODO(https://github.com/Pi4J/pi4j/issues/489): Catch Pi4j exceptions instead.
            Assumptions.abort("CrowPi2 I2C LED Matrix not found");
            throw new RuntimeException(e);
        }
    }
}
