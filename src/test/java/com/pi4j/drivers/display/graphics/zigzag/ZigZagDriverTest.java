package com.pi4j.drivers.display.graphics.zigzag;

import java.util.Map;

import com.pi4j.drivers.display.BitmapFont;
import com.pi4j.drivers.display.graphics.Graphics;
import com.pi4j.drivers.display.graphics.GraphicsDisplay;
import com.pi4j.drivers.display.graphics.GraphicsDisplayInfo;
import com.pi4j.drivers.display.graphics.PixelFormat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZigZagDriverTest {

    private static Logger log = LoggerFactory.getLogger(ZigZagDriverTest.class);

    @Test
    public void test5x8() {
        int width = 5;
        int height = 8;
        Address[][] mapping = new Address[width][height];
        mapping[0][0] = new Address(4, 96);
        mapping[1][0] = new Address(4, 95);
        mapping[2][0] = new Address(4, 48);
        mapping[3][0] = new Address(4, 47);
        mapping[4][0] = new Address(4, 0);
        mapping[0][1] = new Address(4, 97);
        mapping[1][1] = new Address(4, 94);
        mapping[2][1] = new Address(4, 49);
        mapping[3][1] = new Address(4, 46);
        mapping[4][1] = new Address(4, 1);
        mapping[0][2] = new Address(4, 98);
        mapping[1][2] = new Address(4, 93);
        mapping[2][2] = new Address(4, 50);
        mapping[3][2] = new Address(4, 45);
        mapping[4][2] = new Address(4, 2);
        mapping[0][3] = new Address(4, 99);
        mapping[1][3] = new Address(4, 92);
        mapping[2][3] = new Address(4, 51);
        mapping[3][3] = new Address(4, 44);
        mapping[4][3] = new Address(4, 3);
        mapping[0][4] = new Address(4, 100);
        mapping[1][4] = new Address(4, 91);
        mapping[2][4] = new Address(4, 52);
        mapping[3][4] = new Address(4, 43);
        mapping[4][4] = new Address(4, 4);
        mapping[0][5] = new Address(4, 101);
        mapping[1][5] = new Address(4, 90);
        mapping[2][5] = new Address(4, 53);
        mapping[3][5] = new Address(4, 42);
        mapping[4][5] = new Address(4, 5);
        mapping[0][6] = new Address(4, 102);
        mapping[1][6] = new Address(4, 89);
        mapping[2][6] = new Address(4, 54);
        mapping[3][6] = new Address(4, 41);
        mapping[4][6] = new Address(4, 6);
        mapping[0][7] = new Address(4, 103);
        mapping[1][7] = new Address(4, 88);
        mapping[2][7] = new Address(4, 55);
        mapping[3][7] = new Address(4, 40);
        mapping[4][7] = new Address(4, 7);

        GraphicsDisplayInfo displayInfo = new GraphicsDisplayInfo(width, height, PixelFormat.RGB_888);
        FakeHardware fakeHardware = new FakeHardware();
        ZigZagDriver zzd = new ZigZagDriver(displayInfo, mapping, fakeHardware);

        GraphicsDisplay display = new GraphicsDisplay(zzd, GraphicsDisplay.Rotation.ROTATE_0);
        display.setTransferDelayMillis(0);

        BitmapFont font = BitmapFont.get5x8Font();

        Graphics graphics = display.getGraphics();
        graphics.setColor(0xffffffff);
        graphics.setFont(font);

        graphics.renderText(-1, 7, "X");

        Map<Integer, Integer> pixels = fakeHardware.getPixels();

        assertTrue(pixels.get(96) == 0x00ffffff);
        assertTrue(pixels.get(0) == 0x00ffffff);
        assertTrue(pixels.get(97) == 0x00ffffff);
        assertTrue(pixels.get(1) == 0x00ffffff);
        assertTrue(pixels.get(93) == 0x00ffffff);
        assertTrue(pixels.get(45) == 0x00ffffff);
        assertTrue(pixels.get(51) == 0x00ffffff);
        assertTrue(pixels.get(91) == 0x00ffffff);
        assertTrue(pixels.get(43) == 0x00ffffff);
        assertTrue(pixels.get(101) == 0x00ffffff);
        assertTrue(pixels.get(5) == 0x00ffffff);
        assertTrue(pixels.get(102) == 0x00ffffff);
        assertTrue(pixels.get(6) == 0x00ffffff);
    }

}
