package com.pi4j.drivers.display.graphics.crowpi2matrix;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.graphics.AbstractGraphicsDisplayDriverTest;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.ws281x.Ws281xDriver;
import com.pi4j.io.spi.Spi;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;

@Disabled
public class CrowPi2I2cLedMatrixDriverTest extends AbstractGraphicsDisplayDriverTest {
    @Override
    public GraphicsDisplayDriver createDriver(Context pi4j) {
        try {
            return new CrowPi2I2cLedMatrixDriver(pi4j);
        } catch (RuntimeException e) {
            // TODO(https://github.com/Pi4J/pi4j/issues/489): Catch Pi4j exceptions instead.
            Assumptions.abort("CrowPi2 I2C LED Matrix not found");
            throw new RuntimeException(e);
        }
    }
}
