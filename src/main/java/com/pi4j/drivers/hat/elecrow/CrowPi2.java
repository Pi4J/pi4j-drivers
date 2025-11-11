package com.pi4j.drivers.hat.elecrow;

import com.pi4j.context.Context;
import com.pi4j.drivers.display.character.hd44780.Hd44780Driver;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.GraphicsDisplayDriver;
import com.pi4j.drivers.display.graphics.crowpi2matrix.CrowPi2I2cLedMatrixDriver;
import com.pi4j.drivers.sound.PwmSoundDriver;
import com.pi4j.drivers.sound.SoundDriver;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfigBuilder;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmType;

import java.io.Closeable;

public class CrowPi2 implements Closeable {
    private final Context pi4j;

    private CrowPi2I2cLedMatrixDriver i2cLedMatrixDriver;
    private GraphicsDisplay graphicsDisplay;
    private Hd44780Driver hd44780Driver;
    private PwmSoundDriver soundDriver;

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
            Pwm pwm = pi4j.create(Pwm.newConfigBuilder(pi4j).pwmType(PwmType.HARDWARE).channel(2));
            soundDriver = new PwmSoundDriver(pwm);
        }
        return soundDriver;
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
