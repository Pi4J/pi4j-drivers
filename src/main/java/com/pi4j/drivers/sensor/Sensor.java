package com.pi4j.drivers.sensor;

import java.io.Closeable;

/** Implemented by sensor drivers providing one or multiple values. */
public interface Sensor extends Closeable {
    SensorDescriptor getDescriptor();

    /** Reads a single measurement. For information about the values, please refer to getDescriptor() */
    void readMeasurement(float[] values);

    @Override
    void close();


}
