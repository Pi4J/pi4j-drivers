package com.pi4j.drivers.hat.elecrow;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.character.hd44780.Hd44780Driver;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.crowpi2matrix.CrowPi2I2cLedMatrixDriver;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfigBuilder;

public class CrowPi2 {
    private final Context pi4j;

    private CrowPi2I2cLedMatrixDriver i2cLedMatrixDriver;
    private GraphicsDisplay graphicsDisplay;
    private Hd44780Driver hd44780Driver;

    public CrowPi2(Context pi4j) {
        this.pi4j = pi4j;
    }

    public Hd44780Driver getTextDisplay() {
        if (hd44780Driver == null) {
            I2C i2c = pi4j.create(I2CConfigBuilder.newInstance(pi4j).bus(1).device(0x21).build());
            hd44780Driver = Hd44780Driver.withI2cConnection(i2c, Hd44780Driver.I2cConnectionType.MCP23008, 16, 2);
        }
        return hd44780Driver;
    }

    public GraphicsDisplayDriver getGraphicsDisplayDriver() {
        if (i2cLedMatrixDriver == null) {
            I2C i2c = pi4j.create(I2CConfigBuilder.newInstance(pi4j).bus(1).device(CrowPi2I2cLedMatrixDriver.I2C_ADDRESS).build());
            i2cLedMatrixDriver = new CrowPi2I2cLedMatrixDriver(i2c);
        }
        return i2cLedMatrixDriver;
    }

    public GraphicsDisplay getGraphicsDisplay() {
        if (graphicsDisplay == null) {
            graphicsDisplay = new GraphicsDisplay(getGraphicsDisplayDriver());
        }
        return graphicsDisplay;
    }
}
