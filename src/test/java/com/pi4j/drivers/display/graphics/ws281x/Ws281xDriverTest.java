package com.pi4j.drivers.display.graphics.ws281x;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.graphics.AbstractGraphicsDisplayDriverTest;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.crowpi2matrix.CrowPi2I2cLedMatrixDriver;
import com.pi4j.io.spi.Spi;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;

// We can't check in an enabled Ws281x driver test, as the chip can't be detected at runtime.
// Enable locally by commenting out the next line
@Disabled
public class Ws281xDriverTest extends AbstractGraphicsDisplayDriverTest {
    @Override
    public GraphicsDisplayDriver createDriver(Context pi4j) {
        try {
            Spi spi = pi4j.create(Spi.newConfigBuilder(pi4j).bus(0).address(0).baud(Ws281xDriver.SPI_BAUD).build());
            return new Ws281xDriver(spi, 8, 8, true);
        } catch (Exception e) {
            // TODO(https://github.com/Pi4J/pi4j/issues/489): Catch Pi4j exceptions instead.
            Assumptions.abort("CrowPi2 I2C LED Matrix not found");
            throw new RuntimeException(e);
        }
    }
}
