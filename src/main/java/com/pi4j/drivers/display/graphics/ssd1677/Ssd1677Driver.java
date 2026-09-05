package com.pi4j.drivers.display.graphics.ssd1677;

import com.pi4j.drivers.display.graphics.*;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;
import com.pi4j.util.Delay;

public abstract class Ssd1677Driver implements GraphicsDisplayDriver {
    private final Spi spi;
    private final DigitalOutput rst;
    private final DigitalOutput dc;
    private final DigitalInput busy;
    private final Delay delay = new Delay();
    private final GraphicsDisplayDescriptor descriptor;
    private byte[] transferBuffer = new byte[64];
    private byte[] screenBuffer = new byte[64];

    private Runnable updateShadowMemory = null;
    private PartialUpdatePolicy partialUpdatePolicy = PartialUpdatePolicy.DEFAULT_POLICY;
    private boolean firstRender = true;
    private final boolean grayscale;

    protected Ssd1677Driver(
            Spi spi,
            DigitalOutput dc,
            DigitalOutput rst,
            DigitalInput busy,
            int width,
            int height,
            boolean grayscale) {
        this.spi = spi;
        this.dc = dc;
        this.rst = rst;
        this.busy = busy;
        this.descriptor = new GraphicsDisplayDescriptor(
                width,
                height,
                grayscale ? PixelFormat.GRAYSCALE_2 : PixelFormat.MONOCHROME,
                8, GraphicsDisplay.Rotation.ROTATE_0);
        this.grayscale = grayscale;
        initialize();
    }

    // Subclasses need to implement these ------------------------------------------------------------------------------

    /** Concrete display implementations implement the general initialization code here. */
    protected abstract void initialize();

    /** The initialization code for the given mode goes here. */
    protected abstract void setUpdateMode(UpdateMode mode);

    // Public API ------------------------------------------------------------------------------------------------------

    @Override
    public void close() {
        sendCommand(Command.DEEP_SLEEP_MODE, 0x03); //enter deep sleep
        delay.setMillis(100).materialize();
    }

    @Override
    public boolean isBusy() {
        return busy.isOn();
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
    public void setPixels(int x, int y, int width, int height, byte[] data) {
        if (grayscale) {
            setPixelsGrayscale(x, y, width, height, data);
        } else {
            setPixelsMonochrome(x, y, width, height, data);
        }
    }

    void setPartialUpdatePolicy(PartialUpdatePolicy partialUpdatePolicy) {
        this.partialUpdatePolicy = partialUpdatePolicy;
    }

    // Helpers -------------------------------------------------------------------------------------------------

    public void setPixelsMonochrome(int x, int y, int width, int height, byte[] data) {
        if (updateShadowMemory != null) {
            updateShadowMemory.run();
            updateShadowMemory = null;
        }

        setWindow(x, y, x + width - 1, y + height - 1);

        int byteCount = (width * height + 7) / 8;

        sendCommand(Command.WRITE_RAM_BW);
        sendData(data, 0, byteCount);

        if (firstRender || !partialUpdatePolicy.shouldPerformPartialUpdate(x, y, width, height)) {
            setUpdateMode(UpdateMode.FULL);

            sendCommand(Command.WRITE_RAM_RED);
            sendData(data, 0, byteCount);

            sendCommand(Command.MASTER_ACTIVATION); //Activate Display Update Sequence
            firstRender = false;
        } else {
            setUpdateMode(UpdateMode.PARTIAL);

            // We need to update the red memory for the next update after updating the display, but we don't
            // want to block here while the display is busy. So we just defer this.
            if (screenBuffer.length < byteCount) {
                screenBuffer = new byte[byteCount];
            }
            System.arraycopy(data, 0, screenBuffer, 0, byteCount);
            updateShadowMemory = () -> {
                setWindow(x, y, x + width - 1, y + height - 1);
                sendCommand(Command.WRITE_RAM_RED);
                sendData(screenBuffer, 0, byteCount);
            };

            sendCommand(Command.MASTER_ACTIVATION);
        }
    }


    public void setPixelsGrayscale(int x0, int y0, int width, int height, byte[] data) {
        setWindow(x0, y0, x0 + width - 1, y0 + height - 1);

        int byteCount = (width * height) / 4;
        int i = 0;
        int j = byteCount / 2;
        if (screenBuffer.length < byteCount) {
            screenBuffer = new byte[byteCount];
        }
        for (int sourceAddress = 0; sourceAddress < byteCount; sourceAddress += 2) {
            int word = ~((data[sourceAddress] << 8) | (data[sourceAddress + 1] & 255));
            screenBuffer[i++] = (byte) ((word & 0x8000) >> 8
                | (word & 0x2000) >> 7
                    | (word & 0x0800) >> 6
                    | (word & 0x0200) >> 5
                    | (word & 0x0080) >> 4
                    | (word & 0x0020) >> 3
                    | (word & 0x0008) >> 2
                    | (word & 0x0002) >> 1);
            screenBuffer[j++] = (byte) ((word & 0x4000) >> 7
                    | (word & 0x1000) >> 6
                    | (word & 0x0400) >> 5
                    | (word & 0x0100) >> 4
                    | (word & 0x0040) >> 3
                    | (word & 0x0010) >> 2
                    | (word & 0x0004) >> 1
                    | (word & 0x0001));
        }
        setUpdateMode(UpdateMode.GRAYSCALE);

        sendCommand(Command.WRITE_RAM_RED);
        sendData(screenBuffer, 0, byteCount / 2);

        sendCommand(Command.WRITE_RAM_BW);
        sendData(screenBuffer, byteCount / 2, byteCount / 2);

        sendCommand(Command.MASTER_ACTIVATION);
    }


    protected void reset() {
        rst.setState(1);
        delay.setMillis(100).materialize();
        rst.setState(0);
        delay.setMillis(20).materialize();
        rst.setState(1);
        delay.setMillis(200);  // Was 100, but following commands had extra 100
    }

    protected void sendCommand(Command command, int... data) {
        blockWhileBusy();
        delay.materialize();

        if (command.dataCount != -1 && command.dataCount != data.length) {
            throw new IllegalArgumentException("Expected " + command.dataCount + " data bytes for " + command + "; got: " + data.length);
        }

        dc.setState(0);
        spi.write(command.code);
        sendData(data);
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
            delay.setMillis(20).materialize();
        }
        delay.setMillis(20);
    }

    private void sendLookupTable(int[] lookupTable) {
        sendCommand(Command.WRITE_LUT_REGISTER, lookupTable);
    }

    /**
     * Sends the lookuptable contained in the first 105 entries, then gate voltage, source voltage and write vcom
     * from the rest.
     */
    protected void sendLookupTableAndVoltages(int[] lookupTable) {
        sendCommand(Command.WRITE_LUT_REGISTER);
        for (int count = 0; count < 105; count++) {
            sendData(lookupTable[count]);
        }
        sendCommand(Command.GATE_VOLTAGE, lookupTable[105]);
        sendCommand(Command.SOURCE_DRIVING_VOLTAGE_CONTROL, lookupTable[106], lookupTable[107], lookupTable[108]);
        sendCommand(Command.WRITE_VCOM_REGISTER, lookupTable[109]);
    }

    protected void setWindow(int xStart, int yStart, int xEnd, int yEnd) {
        sendCommand(Command.SET_RAM_X_ADDRESS_RANGE,xStart & 0xFF, (xStart>>8) & 0x03, xEnd & 0xFF, (xEnd>>8) & 0x03);
        sendCommand(Command.SET_RAM_Y_ADDRESS_RANGE, yStart & 0xFF, (yStart>>8) & 0x03, yEnd & 0xFF, (yEnd>>8) & 0x03);
        sendCommand(Command.SET_RAM_X_ADDRESS, xStart & 0xFF, (xStart >> 8) & 0x03);
        sendCommand(Command.SET_RAM_Y_ADDRESS, yStart & 0xff, (yStart >> 8) & 0x03);
    }
}
