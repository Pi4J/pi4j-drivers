package com.pi4j.drivers.examples.waveshare13in3k;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.drivers.display.graphics.Argb32;
import com.pi4j.drivers.display.graphics.Graphics;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.epaper.waveshare13in3k.Waveshare13in3kDriver;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiMode;

import java.io.*;
import java.util.Random;

public class Waveshare13in3k {

    public static void main(String[] args) throws InterruptedException, IOException {
        Context context = Pi4J.newAutoContext();
        DigitalOutput pwr = context.create(DigitalOutput.newConfigBuilder(context).bcm(18));
        pwr.setState(true);
        DigitalOutput dc = context.create(DigitalOutput.newConfigBuilder(context).bcm(25));
        DigitalOutput rst = context.create(DigitalOutput.newConfigBuilder(context).bcm(17));
        DigitalInput busy = context.create(DigitalInput.newConfigBuilder(context).bcm(24));
        Spi spi = context.create(Spi.newConfigBuilder(context).bus(0).channel(0).baud(2_000_000).mode(SpiMode.MODE_0));

        System.out.println("Creating driver");
        Waveshare13in3kDriver driver = new Waveshare13in3kDriver(spi, dc, rst, busy, false);

        /*
        System.out.println("Sending clear command");
        driver.EPD_13IN3K_Clear();
        Random random = new Random();

        byte[] data = new byte[960*680/8];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) random.nextInt(256);
        }

        System.out.println("Sending image command");
        driver.EPD_13IN3K_Display(data);


        System.out.println("Sending color base 0 command");
        driver.EPD_13IN3K_color_Base(0);

        System.out.println("Sending sleep command");
        driver.enterSleepMode();
        pwr.setState(false); */

        GraphicsDisplay display = new GraphicsDisplay(driver);
        Graphics graphics = display.getGraphics();

        graphics.setColor(Argb32.WHITE);
        graphics.fillRect(0, 0, display.getWidth(), display.getHeight());

        graphics.setColor(Argb32.BLACK);
        graphics.setTextScale(2);

        InputStream is = new FileInputStream("/dev/vcsa");
        int lineCount = is.read();
        int columnCount = is.read();
        int cursorX = is.read();
        int cursorY = is.read();

        for (int y = 0; y < lineCount; y++) {
            for (int x = 0; x < columnCount; x++) {
                int code = is.read();
                int attr = is.read();
                boolean atCursor = x == cursorX && y == cursorY;
                graphics.setColor(atCursor ? Argb32.BLACK : Argb32.WHITE);
                int sx = x * 12;
                int sy = y * 20;
                graphics.fillRect(sx, sy, 12, 20);
                graphics.setColor(atCursor ? Argb32.WHITE : Argb32.BLACK);
                graphics.renderCharacter(sx, sy + 20, code);
            }
        }

/*

        for (int i = 0; i < 25; i++) {
            //graphics.setColor(Argb32.fromRgb(i * 10, i*10, i*10));
            graphics.renderText(0, i * 20, "This is line number " + i);
        }

        Thread.sleep(5_000);
        graphics.setColor(Argb32.WHITE);
        graphics.fillRect(0, 0, display.getWidth(), display.getHeight());
        Thread.sleep(5_000);

        graphics.setColor(Argb32.BLACK);
        for (int i = 0; i < 25; i++) {
            //graphics.setColor(Argb32.fromRgb(i * 10, i*10, i*10));
            graphics.renderText(0, i * 20, "Updated " + System.currentTimeMillis());
        }
*/
        Thread.sleep(10_000);

        display.close();
    }
}
