package com.pi4j.drivers.display.character.hd44780;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class Hd44780DriverTest {

    private static final int BUS = 1;
    private static final int DEVICE_ADDRESS = 0x27;

    private Context pi4j;

    @BeforeEach
    public void setUp() {
        pi4j = Pi4J.newAutoContext();
    }

    @AfterEach
    public void tearDown() {
        pi4j.shutdown();
    }


    /**
     * Renders Hello Wörld (ｼ) and a time stamp if a 20x4 display is connected via a
     * PCF 8574 on address 0x27.
     */
    @Test
    public void hd44780Test() {
        Hd44780Driver characterLcd = createDriver();

        characterLcd.setBacklightEnabled(true);
        characterLcd.clearDisplay();
        characterLcd.setBlinkingEnabled(true);
        characterLcd.setCursorEnabled(true);
        characterLcd.write("Hello Wörld (ｼ)\n" + System.currentTimeMillis());
    }

    @Test
    public void writeToInvalidLineShouldThrowError() {
        Hd44780Driver characterLcd = createDriver(16, 4);
        assertThrows(IllegalArgumentException.class, () -> characterLcd.writeAt(0, 5, "Test"));
    }

    private Hd44780Driver createDriver() {
        return createDriver(16, 2);
    }

    private Hd44780Driver createDriver(int width, int height)  {
        try {
            I2C i2c = pi4j.create(I2C.newConfigBuilder(pi4j)
                    .bus(BUS)
                    .device(DEVICE_ADDRESS)
                    .build());

            return Hd44780Driver.withPcf8574Connection(i2c, width, height);
        } catch (RuntimeException e) {
            e.printStackTrace();
            // TODO(https://github.com/Pi4J/pi4j/issues/489): Catch Pi4j exceptions instead.
            Assumptions.abort("HD 44780 connected via PCF 8574 not found");
            throw new RuntimeException(e);
        }
    }

}
