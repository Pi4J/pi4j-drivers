package com.pi4j.drivers.display.graphics.st77xx;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.graphics.AbstractGraphicsDisplayDriverTest;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.PixelFormat;
import com.pi4j.drivers.display.graphics.st7789.St7789Driver;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfigBuilder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;

/**
 * This test assumes the waveshare 1.3inch IPS display HAT pinout, see https://www.waveshare.com/1.3inch-lcd-hat.htm
 */
// TODO(b/488): Re-enable when we can safely detect the chip and skip the test if absent.
@Disabled
public class St77xxDriverTest extends AbstractGraphicsDisplayDriverTest {
    private static final int BACKLIGHT_BCM = 24;
    private static final int DC_ADDRESS = 25;
    private static final int SPI_BAUDRATE = 62_500_000;
    private static final int RST_BCM = 27;
    private static final int SPI_BUS = 0;
    private static final int SPI_BCM = 0;

    @Override
    public GraphicsDisplayDriver createDriver(Context pi4j) {
        try {
            DigitalOutput bl = pi4j
                    .create(DigitalOutputConfigBuilder.newInstance(pi4j).bcm(BACKLIGHT_BCM).build());
            bl.high();
            DigitalOutput rst = pi4j.create(DigitalOutputConfigBuilder.newInstance(pi4j).bcm(RST_BCM).build());
            rst.high();
            DigitalOutput dc = pi4j.create(DigitalOutputConfigBuilder.newInstance(pi4j).bcm(DC_ADDRESS).build());
            Spi spi = pi4j.create(
                    SpiConfigBuilder.newInstance(pi4j).bus(SPI_BUS).bcm(SPI_BCM).baud(SPI_BAUDRATE).build());
            return new St7789Driver(spi, dc, 240, PixelFormat.RGB_444);
        } catch (RuntimeException e) {
            e.printStackTrace();
            // TODO(https://github.com/Pi4J/pi4j/issues/489): Catch Pi4j exceptions instead.
            Assumptions.abort("St7789 not found");
            throw new RuntimeException(e);
        }
    }

}
