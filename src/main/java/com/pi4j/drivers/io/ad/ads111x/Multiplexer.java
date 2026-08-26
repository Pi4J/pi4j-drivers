package com.pi4j.drivers.io.ad.ads111x;

/** Determines which input pins to compare for a measurement */
public enum Multiplexer {
    AIN0_AIN1,
    AIN0_AIN3,
    AIN1_AIN3,
    AIN2_AIN3,
    AIN0_GND,
    AIN1_GND,
    AIN2_GND,
    AIN3_GND,
}
