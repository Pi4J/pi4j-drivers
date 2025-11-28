package com.pi4j.drivers.sensor.geospatial.mcp7941x;

import java.time.LocalDateTime;

import com.pi4j.io.i2c.I2C;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mcp7941xDriver {
    private static Logger log = LoggerFactory.getLogger(Mcp7941xDriver.class);

    private static final int START_OSCILLATOR = 0x80;

    private static final int SECONDS = 0x00;
    private static final int MINUTES = 0x01;
    private static final int HOURS = 0x02;
    private static final int DAY_OF_WEEK = 0x03;
    private static final int DAY = 0x04;
    private static final int MONTH = 0x05;
    private static final int YEAR = 0x06;

    private final I2C i2c;

    public Mcp7941xDriver(I2C i2c) {

        this.i2c = i2c;
    }

    public LocalDateTime getDateTime() {

        int year = bcdToDec(i2c.readRegister(YEAR)) + 2000;
        int month = bcdToDec(i2c.readRegister(MONTH));
        int day = bcdToDec(i2c.readRegister(DAY));

        int hour = bcdToDec(i2c.readRegister(HOURS));
        int minute = bcdToDec(i2c.readRegister(MINUTES));
        int seconds = bcdToDec(i2c.readRegister(SECONDS) & 0x7F);

        return LocalDateTime.of(year, month, day, hour, minute, seconds);
    }

    public void setDateTime(LocalDateTime localDateTime) {

        i2c.writeRegister(SECONDS, (byte) decToBcd(localDateTime.getSecond()) | START_OSCILLATOR);
        i2c.writeRegister(MINUTES, (byte) decToBcd(localDateTime.getMinute()));
        i2c.writeRegister(HOURS, (byte) decToBcd(localDateTime.getHour()));

        i2c.writeRegister(DAY, (byte) decToBcd(localDateTime.getDayOfMonth()));
        i2c.writeRegister(MONTH, (byte) decToBcd(localDateTime.getMonthValue()));
        i2c.writeRegister(YEAR, (byte) decToBcd(localDateTime.getYear() - 2000));
    }

    public static int bcdToDec(int bcd) {
        return ((bcd >> 4) * 10) + (bcd & 0x0F);
    }

    private static int decToBcd(int val) {
        return ((val / 10) << 4) | (val % 10);
    }

}
