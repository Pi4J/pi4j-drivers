package com.pi4j.drivers.sensor;

import com.pi4j.context.Context;
import com.pi4j.drivers.sensor.environment.bmx280.Bmx280Driver;
import com.pi4j.drivers.sensor.environment.hts221.Hts221Driver;
import com.pi4j.drivers.sensor.environment.lps25h.Lps25hDriver;
import com.pi4j.drivers.sensor.environment.scd4x.Scd4xDriver;
import com.pi4j.drivers.sensor.environment.tcs3400.Tcs3400Driver;
import com.pi4j.drivers.sensor.geospatial.lsm9ds1.Lsm9ds1Driver;
import com.pi4j.drivers.sensor.geospatial.lsm9ds1.Lsm9ds1MagnetometerDriver;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfigBuilder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Tools for detecting sensors.
 */
public class SensorDetector {
    private static final SensorDescriptor[] DESCRIPTORS = {
            Bmx280Driver.DESCRIPTOR_BME_280,
            Bmx280Driver.DESCRIPTOR_BMP_280,
            Hts221Driver.DESCRIPTOR,
            Lps25hDriver.DESCRIPTOR,
            Scd4xDriver.DESCRIPTOR,
            Tcs3400Driver.DESCRIPTOR,
            Lsm9ds1Driver.DESCRIPTOR,
            Lsm9ds1MagnetometerDriver.DESCRIPTOR,
    };

    /** Returns a list of all detected I2c sensors on the given bus. */
    public static List<Sensor> detectI2cSensors(Context context, int bus) {
        return detectI2cSensors(context, bus, EnumSet.allOf(SensorDescriptor.Kind.class));
    }

    /** Returns a list of all detected I2c sensors on the given bus, providing at least one value in the given set of kinds. */
    public static List<Sensor> detectI2cSensors(Context context, int bus, EnumSet<SensorDescriptor.Kind> kinds) {
        List<Sensor> result = new ArrayList<>();
        for (SensorDescriptor descriptor: DESCRIPTORS) {
            if (descriptor.getValues().stream().anyMatch(value -> kinds.contains(value.getKind()))) {
                for (int address: descriptor.getI2cAddresses()) {
                    try {
                        I2C i2c = context.create(I2CConfigBuilder.newInstance(context).bus(bus).device(address));
                        Sensor sensor = descriptor.detect(i2c);
                        if (sensor != null) {
                            result.add(sensor);
                        }
                    } catch (Exception e) {
                        // Ignore all exception
                    }
                }
            }
        }
        return result;
    }
}
