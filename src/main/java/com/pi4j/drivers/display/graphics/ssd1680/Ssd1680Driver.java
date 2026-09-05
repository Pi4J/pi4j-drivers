package com.pi4j.drivers.display.graphics.ssd1680;

import com.pi4j.drivers.display.graphics.*;
import com.pi4j.drivers.display.graphics.ssd1677.UpdateMode;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;
import com.pi4j.util.Delay;


public abstract class Ssd1680Driver implements GraphicsDisplayDriver {
    private static final int BUSY_LINE_DELAY_MS = 50;

    private final Spi spi;
    private final DigitalOutput rst;
    private final DigitalOutput dc;
    private final DigitalInput busy;
    private final Delay delay = new Delay();
    private final GraphicsDisplayDescriptor descriptor;
    private byte[] transferBuffer = new byte[64];
    private byte[] screenBuffer = new byte[64];

    private Runnable updateShadowMemory;
    private PartialUpdatePolicy partialUpdatePolicy = PartialUpdatePolicy.DEFAULT_POLICY;
    boolean firstRender = true;
    boolean previousPartial = false;

    // Time it might take for the busy line to go up after sending the screen or a soft reset.

    protected Ssd1680Driver(
            Spi spi,
            DigitalOutput dc,
            DigitalOutput rst,
            DigitalInput busy,
            int width,
            int height) {
        this.spi = spi;
        this.dc = dc;
        this.rst = rst;
        this.busy = busy;
        this.descriptor = new GraphicsDisplayDescriptor(
                width,
                height,
                PixelFormat.MONOCHROME,
                8, GraphicsDisplay.Rotation.ROTATE_0);

        reset();
        sendCommand(Command.SW_RESET);
    }

    // Subclasses need to implement these ------------------------------------------------------------------------------

    /** The initialization code for the given mode goes here. */
    protected abstract void setUpdateMode(UpdateMode mode);

    // Public API ------------------------------------------------------------------------------------------------------

    @Override
    public void close() {
        sendCommand(Command.DEEP_SLEEP_MODE, 0x03); //enter deep sleep
        delay.setMillis(100).materialize();
    }

    @Override
    public int getTransferLimit() {
        return Integer.MAX_VALUE;
    }

    @Override
    public GraphicsDisplayDescriptor getDisplayInfo() {
        return descriptor;
    }

    @Override
    public boolean isBusy() {
        return busy.isOn();
    }

    @Override
    public void setPixels(int x, int y, int width, int height, byte[] data) {
        boolean partial = !firstRender && partialUpdatePolicy.shouldPerformPartialUpdate(x, y, width, height);
        firstRender = false;

        if (partial || partial != previousPartial) {
            setUpdateMode(partial ? UpdateMode.PARTIAL : UpdateMode.FULL);
        }
        previousPartial = partial;

        // For partial updates, it's important that this comes after setting the mode above, which was discovered
        // accidentally during hours of experimentation. I suspect it's because the reset call resets potential
        // implicit primary / secondary buffer swaps. Documentation is sparse, agents deliver contradictionary
        // information and the waveshare driver code is tricky to follow / analyze. The chip might also have
        // display specific modifications.
        if (updateShadowMemory != null) {
            updateShadowMemory.run();
            updateShadowMemory = null;
        }

        if (width % 8 != 0) {
            width += 8 - (width % 8);
        }

        setWindow(x, y, x + width - 1, y + height - 1);
        int byteCount = (width * height + 7) / 8;
        sendCommand(Command.WRITE_RAM);
        sendData(data, 0, byteCount);
        sendCommand(Command.MASTER_ACTIVATION);
        delay.setMillis(50);

        final int finalWidth = width;
        if (screenBuffer.length < byteCount) {
            screenBuffer = new byte[byteCount];
        }
        System.arraycopy(data, 0, screenBuffer, 0, byteCount);
        updateShadowMemory = () -> {
            setWindow(x, y, x + finalWidth - 1, y + height - 1);
            sendCommand(Command.WRITE_RAM_2);
            sendData(screenBuffer, 0, byteCount);
        };
    }

    void setPartialUpdatePolicy(PartialUpdatePolicy partialUpdatePolicy) {
        this.partialUpdatePolicy = partialUpdatePolicy;
    }

    // Helpers -------------------------------------------------------------------------------------------------

    protected void reset() {
        rst.setState(1);
        delay.setMillis(100).materialize();
        rst.setState(0);
        delay.setMillis(20).materialize();
        rst.setState(1);
        delay.setMillis(200);  // Was 100, but following commands had extra 100
    }

    protected void sendCommand(Command command, int... data) {
        if (command.dataCount != -1 && command.dataCount != data.length) {
            throw new IllegalArgumentException("Expected " + command.dataCount + " data bytes for " + command + "; got: " + data.length);
        }

        blockWhileBusy();

        dc.setState(0);
        spi.write(command.code);
        sendData(data);

        if (command == Command.SW_RESET || command == Command.MASTER_ACTIVATION) {
            delay.setMillis(BUSY_LINE_DELAY_MS);
        }
    }

    protected void sendData(int... data) {
        if (data.length > transferBuffer.length) {
            transferBuffer = new byte[data.length];
        }
        for (int i = 0; i < data.length; i++) {
            transferBuffer[i] = (byte) data[i];
        }
        sendData(transferBuffer, 0, data.length);
    }

    protected void sendData(byte[] data, int offset, int length) {
        dc.setState(1);
        int written = 0;
        while (written < length) {
            int count = Math.min(length - written, 4000);
            spi.write(data, offset + written, count);
            written += count;
        }
    }

    private void blockWhileBusy() {
        delay.materialize();
        while(isBusy()) {
            delay.setMillis(10).materialize();
        }
    }

    /** Note that x-coordinates have to be a multiple of 8 */
    protected void setWindow(int xStart, int yStart, int xEnd, int yEnd) {
        sendCommand(Command.SET_RAM_Y_ADDRESS_RANGE, yStart & 0xFF, (yStart>>8) & 0x03, yEnd & 0xFF, (yEnd>>8) & 0x03);
        sendCommand(Command.SET_RAM_X_ADDRESS_RANGE,(xStart >>> 3) & 0xFF, (xEnd >>> 3) & 0xFF);
        sendCommand(Command.SET_RAM_Y_ADDRESS, yStart & 0xff, (yStart >> 8) & 0x03);
        sendCommand(Command.SET_RAM_X_ADDRESS, (xStart >> 3) & 0xFF);
    }
}
