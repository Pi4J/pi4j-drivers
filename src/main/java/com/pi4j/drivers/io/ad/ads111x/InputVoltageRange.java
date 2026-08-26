package com.pi4j.drivers.io.ad.ads111x;

public enum InputVoltageRange {
    V_6_144(6.144),
    V_4_096(4.096),
    /** Default */
    V_2_048(2.048),
    V_1_024(1.024),
    V_0_512(0.512),
    V_0_256(0.256);

    public final double value;

    InputVoltageRange(double value) {
        this.value = value;
    }

}
