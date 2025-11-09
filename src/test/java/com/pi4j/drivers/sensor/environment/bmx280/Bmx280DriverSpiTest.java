package com.pi4j.drivers.sensor.environment.bmx280;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.exception.Pi4JException;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfigBuilder;
import com.pi4j.io.spi.SpiMode;
import org.junit.jupiter.api.*;

/**
 * Runs tests if a BME 280 configured to the BMP 280 address or a BMP 280 is connected to spi bus 0;
 * aborts otherwise.
 */
public class Bmx280DriverSpiTest extends AbstractBmx280DriverTest {

    static final int BUS = 0;
    static final int CSB = 21;

    private Context pi4j;

    @BeforeEach
    public void setup() {
        pi4j = Pi4J.newAutoContext();
    }

    @Override
    Bmx280Driver createDriver() {
        try {
            DigitalOutput csb = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
                    .bcm(CSB).initial(DigitalState.HIGH).shutdown(DigitalState.HIGH).build());
            Spi spi = pi4j.create(SpiConfigBuilder.newInstance(pi4j)
                    .bcm(0).bus(BUS).mode(SpiMode.MODE_0).baud(Spi.DEFAULT_BAUD).provider("linuxfs-spi").build());
            return new Bmx280Driver(spi, csb);
        } catch (Pi4JException | IllegalStateException e) {
            // The illegal state occurs in a test environment if no gpio pin can be created.
            // Not sure if that should really be an Pi4JException, too.
            Assumptions.abort("BMx280 not found on spi bus " + BUS + " csb " + CSB);
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void shutdown() {
        pi4j.shutdown();
    }
}
