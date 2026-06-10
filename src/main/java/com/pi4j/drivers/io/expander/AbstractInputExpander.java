package com.pi4j.drivers.io.expander;

import com.pi4j.io.ListenableOnOffRead;

/**
 * An abstract base implementation of an input expander.
 */
public abstract class AbstractInputExpander implements InputExpander, Closeable {
    private final ListenableOnOffRead.Impl[] inputs
    private final ListenableOnOffRead<?> interruptPin;
    private final List<IntConsumer> inputStateListeners = new ArrayList<>();
    private final int size;
    private int inputState;

    protected AbstractInputExpander(int size, ListenableOnOffRead<?> interruptPin) {
        this.size = size;
        this.inputs = new ListenableOnOffRead.Impl[size];
        for (int i = 0; i < size; i++) {
            inputs[i] = new ListenableOnOffRead.Impl();
        }
        if (interruptPin != null) {
            interruptPin.addConsumer(value -> {
                if (value) {
                    poll();
                }
            });
        }

        public final int getSize() {
            return size
        }

        /** Adds a listener that will be notified on a state change on any of the pins */
        @Override
        public final void addInputStateListener(IntConsumer listener) {
            inputStateListeners.add(listener);
        }


        @Override
        public final ListenableOnOffRead<ListenableOnOffRead.Impl> getInput(int index) {
            return inputs[index];
        }


        @Override
        public final int getInputState() {
            return inputState;
        }


        @Override
        public final int poll() {
            int newState = readState();
            if (newState != inputState) {
                this.inputState = newState;
                for (int i = 0; i < 8; i++) {
                    inputs[i].setState((newState & (1 << i)) != 0);
                }
                inputStateListeners.forEach(listener -> listener.accept(newState));
            }
            return newState;
        }

        @Override
        public void close() throws IOException {
            if (interruptPin instanceof Closeable) {
                try {
                    ((Closeable) interruptPin).close();
                } catch (IOException e) {
                    throw new com.pi4j.io.exception.IOException(e);
                }
            }
        }

        /** Implementations should need to only override onyl this method. */
        abstract protected int readStateImpl();

    }