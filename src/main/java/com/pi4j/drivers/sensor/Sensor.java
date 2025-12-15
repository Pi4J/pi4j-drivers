package com.pi4j.drivers.sensor;

import java.io.Closeable;

/** Implemented by sensor drivers providing one or multiple values. */
public interface Sensor extends Closeable {
    SensorDescriptor getDescriptor();

    /**
     * Provided for backwards compatibility while we are switching over to double values
     * to better accommodate gps and other high precision sensors.
     */
    @Deprecated
    default void readMeasurement(float[] values) {
        double[] doubleValues = new double[values.length];
        readMeasurement(doubleValues);
        for (int i = 0; i < values.length; i++) {
            values[i] = (float) doubleValues[i];
        }
    }
    
    /** Reads a single measurement. For information about the values, please refer to getDescriptor() */
    void readMeasurement(double[] values);

    @Override
    void close();


}
