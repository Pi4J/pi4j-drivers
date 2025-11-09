package com.pi4j.drivers.input;

import com.pi4j.context.Context;
import com.pi4j.io.ListenableOnOffRead;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple digital game controller. Note that not all keys are available on all controllers / hats.
 * For instance, the Sense hat only provides a Joystick with directional inputs and a "center" click.
 */
public class GameController implements Closeable {

    /**
     * Key names based on a typical simple Game controller and some additional KEY_1 .. KEY_3 labels found on the
     * Waveshare 1.4" and 1.33" display hats.
     * <p>
     * While it would be an option to map "special" hat keys to standard game controller keys, which of these
     * keys are needed will depend on the application -- so we pass this problem up to the application, supporting
     * this case with getKeyOrFallback().
     */
    public enum Key {
        LEFT,
        RIGHT,
        UP,
        DOWN,
        CENTER,
        A,
        B,
        X,
        Y,
        SELECT,
        START,
        KEY_1,
        KEY_2,
        KEY_3,
        RT,
        LT
    }

    /**
     * A builder that provides a straightforward way to create a controller out of digital inputs.
     */
    public static class Builder {
        private final Context pi4j;
        private final Map<Key, ListenableOnOffRead<?>> keyMap = new HashMap<>();

        public Builder(Context pi4J) {
            this.pi4j = pi4J;
        }

        public Builder addDigitalInput(Key key, ListenableOnOffRead<?> onOff) {
            keyMap.put(key, onOff);
            return this;
        }

        /** Add a switch between VCC and the given pin as a controller key. */
        public Builder addVccSwitch(Key key, int pin) {
            return addDigitalInput(key, pi4j.create(DigitalInput.newConfigBuilder(pi4j).address(pin).build()));
        }

        /**
         * Add a switch between GND and the given pin as a controller key. The pin will be pulled up and a
         * "low" state will be interpreted as "on".
         */
        public Builder addGndSwitch(Key key, int pin) {
            return addDigitalInput(key, pi4j.create(DigitalInput.newConfigBuilder(pi4j)
                    .address(pin)
                    .pull(PullResistance.PULL_UP)
                    .onState(DigitalState.LOW)
                    .build()));
        }

        public GameController build() {
            return new GameController(keyMap);
        }
    }

    private final Map<Key, ListenableOnOffRead<?>> keyMap = new HashMap<>();

    /** Creates a new Game Controller with the given keys */
    public GameController(Map<Key, ListenableOnOffRead<?>> keyMap) {
        this.keyMap.putAll(keyMap);
    }

    /** Returns true if this controller supports the given key. */
    public boolean supportsKey(Key key) {
        return keyMap.containsKey(key);
    }

    /** Returns a listenable on/off state encapsulation for the given key, or null if not available */
    public ListenableOnOffRead<?> getKey(Key key) {
        return keyMap.get(key);
    }

    /**
     * Returns a listenable on/off state encapsulation for the first key in the arguments that is available on this
     * controller, or null if none is available.
     */
    public ListenableOnOffRead<?> getKeyOrFallback(Key... keys) {
        for (Key key: keys) {
            ListenableOnOffRead<?> available = getKey(key);
            if (available != null) {
                return available;
            }
        }
        return null;
    }

    public void close() {
        Exception failure = null;
        for (ListenableOnOffRead<?> input: keyMap.values()) {
            if (input instanceof Closeable) {
                try {
                    ((Closeable) input).close();
                } catch (Exception e) {
                    failure = e;
                }
            }
        }
        if (failure != null) {
            throw new com.pi4j.io.exception.IOException("At least one key close() operation failed.", failure);
        }
    }
}
