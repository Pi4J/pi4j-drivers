package com.pi4j.drivers.hat.elecrow;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.character.hd44780.Hd44780Driver;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.crowpi2matrix.CrowPi2I2cLedMatrixDriver;
import com.pi4j.drivers.input.GameController;
import com.pi4j.drivers.io.ad.mcp300x.Mcp300xDriver;
import com.pi4j.drivers.sound.PwmSoundDriver;
import com.pi4j.drivers.sound.SoundDriver;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfigBuilder;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmType;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfigBuilder;

import java.io.Closeable;
import java.util.Collections;

public class CrowPi2 implements Closeable {
    private final Context pi4j;

    private CrowPi2I2cLedMatrixDriver i2cLedMatrixDriver;
    private GraphicsDisplay graphicsDisplay;
    private Hd44780Driver hd44780Driver;
    private PwmSoundDriver soundDriver;
    private GameController gameController;
    private Mcp300xDriver mcp3008;

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

    /**
     * Note that this seems to require an explicit entry in config.txt to enable pwm channel2 on pin 18:
     * <code>
     *   dtoverlay=pwm-2chan
     * </code>
     */
    public SoundDriver getSoundDriver() {
        if (soundDriver == null) {
            Pwm pwm = pi4j.create(Pwm.newConfigBuilder(pi4j).pwmType(PwmType.HARDWARE).chip(0).channel(2));
            soundDriver = new PwmSoundDriver(pwm);
        }
        return soundDriver;
    }

    private Mcp300xDriver getMcp3008() {
        if (mcp3008 == null) {
            Spi spi = pi4j.create(SpiConfigBuilder.newInstance(pi4j).bus(0).channel(1));
            mcp3008 = new Mcp300xDriver(spi);
        }
        return mcp3008;
    }

    public GameController getGameController() {
        if (gameController == null) {
            gameController = new GameController(Collections.emptyMap()) {
                @Override
                public float getAnalogJoystickX() {
                   return Math.max(-1f, Math.min((getMcp3008().readChannel(0) - 512f) / 256f, 1f));
                }
                @Override
                public float getAnalogJoystickY() {
                    return Math.max(-1f, Math.min((getMcp3008().readChannel(1) - 512f) / 256f, 1f));
                }
            };
        }
        return gameController;
    }

    @Override
    public void close() {
        if (graphicsDisplay != null) {
            graphicsDisplay.close();
        } else if (i2cLedMatrixDriver != null) {
            i2cLedMatrixDriver.close();
        }

        // TODO: Support close in text displays and close them here.

        if (soundDriver != null) {
            soundDriver.close();
        }
    }
}
