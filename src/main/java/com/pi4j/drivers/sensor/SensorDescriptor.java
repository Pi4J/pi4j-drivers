package com.pi4j.drivers.sensor;

import com.pi4j.io.i2c.I2C;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * A Descriptor for a sensor. Provides meta-information about the sensor, in particular what kind of values
 * it is able to provide.
 */
public class SensorDescriptor {
    private final String sensorName;
    private final List<Value> values;
    private final List<Integer> i2cAddresses;
    private final Function<I2C, Sensor> i2cSensorDetector;

    public SensorDescriptor(
            String sensorName,
            List<Value> values,
            List<Integer> i2cAddresses,
            Function<I2C, Sensor> i2cSensorDetector
    ) {
        this.sensorName = sensorName;
        this.values = Collections.unmodifiableList(values);
        this.i2cAddresses = Collections.unmodifiableList(i2cAddresses);
        this.i2cSensorDetector = i2cSensorDetector;
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

    /** Returns the type name of the sensor. */
    public String getSensorName() {
        return sensorName;
    }

    public List<Integer> getI2cAddresses() {
        return i2cAddresses;
    }

    public Sensor detect(I2C i2c) {
        return i2cSensorDetector.apply(i2c);
    }

    public static class Builder {
        private final String sensorName;
        private final List<Value> values = new ArrayList<>();
        private final List<Integer> i2cAddresses = new ArrayList<>();
        private Function<I2C, Sensor> i2cSensorDetector;

        public Builder(String sensorName) {
            this.sensorName = sensorName;
        }

        public Builder addValue(Kind kind) {
            values.add(new Value(values.size(), kind));
            return this;
        }

        public Builder addI2cAddress(int address) {
            i2cAddresses.add(address);
            return this;
        }

        public Builder setI2cSensorDetector(Function<I2C, Sensor> i2cSensorDetector) {
            this.i2cSensorDetector = i2cSensorDetector;
            return this;
        }

        public SensorDescriptor build() {
            return new SensorDescriptor(sensorName, values, i2cAddresses, i2cSensorDetector);
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
        ACCELERATION_X(MeasurementUnit.METER_PER_SECOND_SQUARED),

        /** Acceleration in the y-direction m/s^2 */
        ACCELERATION_Y(MeasurementUnit.METER_PER_SECOND_SQUARED),

        /** Acceleration in the y-direction m/s^2. This is typically 9.81 when due to the Earth's gravity. */
        ACCELERATION_Z(MeasurementUnit.METER_PER_SECOND_SQUARED),

        /** Angular velocity around the x-axis in deg/s */
        ANGULAR_VELOCITY_X(MeasurementUnit.METER_PER_SECOND),

        /** Angular velocity around the x-axis in deg/s */
        ANGULAR_VELOCITY_Y(MeasurementUnit.METER_PER_SECOND),

        /** Angular velocity around the x-axis in deg/s */
        ANGULAR_VELOCITY_Z(MeasurementUnit.METER_PER_SECOND),

        /** CO2 in ppm */
        CO2(MeasurementUnit.PARTS_PER_MILLION),

        /** A distance in meter */
        DISTANCE(MeasurementUnit.METER),

        /** Relative Humidity (0…100%) */
        HUMIDITY(MeasurementUnit.PERCENT),

        /** Unfiltered light value in Lux */
        LIGHT(MeasurementUnit.LUX),
        /** Red light value in Lux */
        LIGHT_RED(MeasurementUnit.LUX),
        /** Green light value in Lux */
        LIGHT_GREEN(MeasurementUnit.LUX),
        /** Blue light value in Lux */
        LIGHT_BLUE(MeasurementUnit.LUX),

        /** X-component of the magnetic field in Gauss */
        MAGNETIC_FIELD_X(MeasurementUnit.GAUSS),
        /** Y-component of the magnetic field in Gauss */
        MAGNETIC_FIELD_Y(MeasurementUnit.GAUSS),
        /** Z-component of the magnetic field in Gauss */
        MAGNETIC_FIELD_Z(MeasurementUnit.GAUSS),

        /**
         * Pressure in hectoPascal (using hectoPascal instead of pascal as it's very common, equivalent to mBar and
         * regular values are in a more reasonable range (~1000 instead of ~100'000).
         */
        PRESSURE(MeasurementUnit.PASCAL),

        /** Temperature in degree Celsius */
        TEMPERATURE(MeasurementUnit.DEGREE_CELSIUS);

        public final MeasurementUnit measurementUnit;

        Kind(MeasurementUnit measurementUnit) {
            this.measurementUnit = measurementUnit;
        }
    }

    public enum MeasurementUnit {
        DEGREE_CELSIUS("°C"),
        METER("m"),
        METER_PER_SECOND("m/s"),
        METER_PER_SECOND_SQUARED("m/s²"),
        GAUSS("Gs"),
        LUX("lx"),
        PARTS_PER_MILLION("ppm"),
        PASCAL("Pa"),
        PERCENT("%");

        public final String name;

        MeasurementUnit(String name) {
            this.name = name;
        }
    }
}
