package com.pi4j.drivers.sensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Descriptor for a sensor. Provides meta-information about the sensor, in particular what kind of values
 * it is able to provide.
 */
public class SensorDescriptor {
    private final List<Value> values;

    public SensorDescriptor(List<Value> values) {
        this.values = Collections.unmodifiableList(values);
    }

    /**
     * Returns an unmodifiable list of the value descriptors
     */
    public List<Value> getValues() {
        return values;
    }

    /**
     * Returns the first index of a value of the given kind, or -1 if not found.
     */
    public int indexOf(Kind kind) {
        for (Value valueDescriptor : values) {
            if (valueDescriptor.getKind() == kind) {
                return valueDescriptor.index;
            }
        }
        return -1;
    }

    public static class Builder {
        private final List<Value> values = new ArrayList<>();

        public Builder addValue(Kind kind) {
            values.add(new Value(values.size(), kind));
            return this;
        }

        public SensorDescriptor build() {
            return new SensorDescriptor(values);
        }
    }

    /** Descriptor for a single sensor value. */
    public static class Value {
        private final int index;
        private final Kind kind;

        public Value(int index, Kind kind) {
            this.index = index;
            this.kind = kind;
        }

        /** The index of the described value in readMeasurement */
        public int getIndex() {
            return index;
        }

        public Kind getKind() {
            return kind;
        }
    }


    /**
     * Describes the kind of a sensor value.
     * Units are SI units or common SI-based units.
     */
    public enum Kind {
        /** Acceleration in the x-direction m/s^2 */
        ACCELERATION_X,

        /** Acceleration in the y-direction m/s^2 */
        ACCELERATION_Y,

        /** Acceleration in the y-direction m/s^2. This is typically 9.81 when due to the Earth's gravity. */
        ACCELERATION_Z,

        /** Angular velocity around the x-axis in deg/s */
        ANGULAR_VELOCITY_X,

        /** Angular velocity around the x-axis in deg/s */
        ANGULAR_VELOCITY_Y,

        /** Angular velocity around the x-axis in deg/s */
        ANGULAR_VELOCITY_Z,

        /** CO2 in ppm */
        CO2,

        /** Relative Humidity (0…100%) */
        HUMIDITY,

        /** Unfiltered light value in Lux */
        LIGHT,
        /** Red light value in Lux */
        LIGHT_RED,
        /** Green light value in Lux */
        LIGHT_GREEN,
        /** Blue light value in Lux */
        LIGHT_BLUE,

        /** X-component of the magnetic field in Gauss */
        MAGNETIC_FIELD_X,
        /** Y-component of the magnetic field in Gauss */
        MAGNETIC_FIELD_Y,
        /** Z-component of the magnetic field in Gauss */
        MAGNETIC_FIELD_Z,

        /**
         * Pressure in hectoPascal (using hectoPascal instead of pascal as it's very common, equivalent to mBar and
         * regular values are in a more reasonable range (~1000 instead of ~100'000).
         */
        PRESSURE,

        /** Temperature in degree Celsius */
        TEMPERATURE,
    }

}
