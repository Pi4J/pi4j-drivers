package com.pi4j.drivers.sensor.environment.bmx280;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.exception.Pi4JException;
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
    static final int CHANNEL = 0;

    private Context pi4j;

    @BeforeEach
    public void setup() {
        pi4j = Pi4J.newAutoContext();
    }

    @Override
    Bmx280Driver createDriver() {
        try {
            Spi spi = pi4j.create(SpiConfigBuilder.newInstance(pi4j)
                    .bus(BUS).channel(CHANNEL).mode(SpiMode.MODE_0).baud(Spi.DEFAULT_BAUD).build());
            return new Bmx280Driver(spi);
        } catch (Pi4JException e) {
            Assumptions.abort("BMx280 not found on spi bus " + BUS + " channel " + CHANNEL);
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void shutdown() {
        pi4j.shutdown();
    }
}
