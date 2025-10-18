package com.pi4j.drivers.sensor;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Iterator;

/** Implemented by sensor drivers providing one or multiple values. */
public interface Sensor extends Closeable {
    Descriptor getDescriptor();

    /** Reads a single measurement. For information about the values, please refer to getDescriptor() */
    void readMeasurement(float[] values);

    @Override
    void close();

    /**
     * A Descriptor for a sensor. Provides meta-information about the sensor, in particular what kind of values
     * it is able to provide.
     */
    class Descriptor implements Iterable<ValueDescriptor> {
        private final ValueDescriptor[] valueDescriptors;

        public Descriptor(ValueDescriptor... descriptors) {
            this.valueDescriptors = descriptors;
        }

        /** Returns the number of values provided by the sensor. */
        public int getValueCount() {
            return valueDescriptors.length;
        }

        /** Returns the value descriptor with the given index */
        public ValueDescriptor getValueDescriptor(int index) {
            return valueDescriptors[index];
        }

        /** Returns the first index of a value of the given kind, or -1 if not found. */
        public int indexOf(ValueKind kind) {
            for (ValueDescriptor valueDescriptor : this) {
                if (valueDescriptor.getKind() == kind) {
                    return valueDescriptor.index;
                }
            }
            return -1;
        }

        @Override
        public Iterator<ValueDescriptor> iterator() {
            return Arrays.asList(valueDescriptors).iterator();
        }
    }

    /** Descriptor for a single sensor value. */
    class ValueDescriptor {
        private final int index;
        private final ValueKind kind;

        public ValueDescriptor(int index, ValueKind kind) {
            this.index = index;
            this.kind = kind;
        }

        /** The index of the described value in readMeasurement */
        public int getIndex() {
            return index;
        }

        public ValueKind getKind() {
            return kind;
        }
    }

    /**
     * Describes the kind of a sensor value.
     * Units are SI units or common SI-based units.
     */
    enum ValueKind {
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
