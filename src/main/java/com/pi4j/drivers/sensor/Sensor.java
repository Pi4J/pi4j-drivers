package com.pi4j.drivers.sensor;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface Sensor extends Closeable {
    Descriptor getDescriptor();

    void readMeasurement(float[] values);

    @Override
    void close();

    class Descriptor {
        private final List<ValueDescriptor> valueDescriptors;

        public Descriptor(ValueDescriptor... descriptors) {
            this.valueDescriptors = Collections.unmodifiableList(Arrays.asList(descriptors));
        }

        public List<ValueDescriptor> getValueDescriptors() {
            return valueDescriptors;
        }
    }

    class ValueDescriptor {
        private final int index;
        private final ValueKind kind;

        public ValueDescriptor(int index, ValueKind kind) {
            this.index = index;
            this.kind = kind;
        }

        public int getIndex() {
            return index;
        }

        public ValueKind getKind() {
            return kind;
        }
    }

    /** Units are SI units or common SI-based units  */
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
