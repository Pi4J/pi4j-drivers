package com.pi4j.drivers.sensor.environment.scd4x;

/**
 * Internal command code constants
 */
class CommandCodes {
    static final int START_PERIODIC_MEASUREMENT = 0x21b1;
    static final int READ_MEASUREMENT = 0xec05;
    static final int STOP_PERIODIC_MEASUREMENT = 0x3f86;
    static final int SET_TEMPERATURE_OFFSET = 0x241d;
    static final int GET_TEMPERATURE_OFFSET = 0x2318;
    static final int SET_SENSOR_ALTITUDE = 0x2427;
    static final int GET_SENSOR_ALTITUDE = 0x2322;
    static final int SET_AMBIENT_PRESSURE = 0xe000;
    static final int PERFORM_FORCED_RECALIBRATION = 0x362f;
    static final int SET_AUTOMATIC_SELF_CALIBRATION_ENABLED = 0x2416;
    static final int GET_AUTOMATIC_SELF_CALIBRATION_ENABLED = 0x2313;
    static final int START_LOW_POWER_PERIODIC_MEASUREMENT = 0x21ac;
    static final int GET_DATA_READY_STATUS = 0xe4b8;
    static final int GET_SERIAL_NUMBER = 0x3682;
    static final int PERFORM_SELF_TEST = 0x3639;
    static final int PERFORM_FACTORY_RESET = 0x3632;
    static final int RE_INIT = 0x3646;
    static final int MEASURE_SINGLE_SHOT = 0x29d;
    static final int MEASURE_SINGLE_SHOT_RHT_ONLY = 0x2196;
    static final int POWER_DOWN = 0x36e0;
    static final int WAKE_UP = 0x36f6;
    static final int SET_AUTOMATIC_SELF_CALIBRATION_INITIAL_PERIOD = 0x2445;
    static final int GET_AUTOMATIC_SELF_CALIBRATION_INITIAL_PERIOD = 0x2340;
    static final int SET_AUTOMATIC_SELF_CALIBRATION_STANDARD_PERIOD = 0x244e;
    static final int GET_AUTOMATIC_SELF_CALIBRATION_STANDARD_PERIOD = 0x234b;
}
