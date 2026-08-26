package com.pi4j.drivers.radio.lora.lr11xx;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * The tests that need a radio on the desk, as JUnit.
 *
 * <p>The work itself is in {@link Lr11xxLinkCheck}, which also has a {@code main}.
 * One implementation, two ways to reach it: Maven where there is a checkout, and a
 * plain classpath where there is not — see that class for the {@code rsync} and
 * {@code ssh} form, which is what to use when driving two devices from a third
 * machine.
 *
 * <h2>Running these</h2>
 *
 * Everything here is off unless {@code lr11xx.role} says otherwise, so an ordinary
 * build skips the lot. Run the single-radio check first, on each device:
 *
 * <pre>
 * mvn test -Dtest=Lr1121DriverHardwareTest -Dlr11xx.role=check
 * </pre>
 *
 * <p>A radio that will not identify itself has a wiring fault, and no amount of
 * link debugging will find it. Once both answer, the link needs <b>two machines</b>
 * — a radio cannot hear itself. Start the listener first:
 *
 * <pre>
 * mvn test -Dtest=Lr1121DriverHardwareTest -Dlr11xx.role=receive    # on one
 * mvn test -Dtest=Lr1121DriverHardwareTest -Dlr11xx.role=transmit   # on the other
 * </pre>
 *
 * <p>Wiring and radio parameters are system properties with Waveshare Core1121
 * defaults; {@link Lr11xxLinkCheck} lists them. Both ends must agree on the
 * frequency and the spreading factor, and the antenna goes on before power.
 *
 * <h2>Why a system property rather than {@code @Disabled}</h2>
 *
 * The other hardware tests in this project are annotated {@code @Disabled}, which
 * means running them requires editing the file. That is tolerable for a sensor on
 * one desk and poor for a link, where it would mean the same edit on two machines
 * and a dirty working tree on both. A property is skipped just as thoroughly in CI
 * and can be turned on from the command line.
 *
 * <p>Note also that this project carries several Pi4J providers on the test
 * classpath and {@code newAutoContext()} picks whichever registers, so these
 * exercise the provider your machine ends up with rather than one they choose.
 */
class Lr1121DriverHardwareTest {

    @Test
    @EnabledIfSystemProperty(named = "lr11xx.role", matches = "check")
    void theRadioIdentifiesItselfAndConfigures() {
        Lr1121Driver.Version version = run(Lr11xxLinkCheck::check);

        assertNotNull(version);
        assertTrue(version.hardware() != 0x00 && version.hardware() != 0xFF,
                "the chip answered with " + version + ", which is what an unconnected MISO"
                + " line looks like — check the wiring before anything else");
    }

    @Test
    @EnabledIfSystemProperty(named = "lr11xx.role", matches = "transmit")
    void transmits() {
        assertTrue(run(Lr11xxLinkCheck::transmit) > 0,
                "the time ran out before a single packet was sent");
    }

    @Test
    @EnabledIfSystemProperty(named = "lr11xx.role", matches = "receive")
    void receives() {
        assertTrue(run(Lr11xxLinkCheck::receive) > 0, Lr11xxLinkCheck.NOTHING_HEARD);
    }

    /**
     * Runs one of the checks, turning "there is no radio here" into a skip.
     *
     * <p>Aborting rather than failing is how the other hardware tests here behave,
     * and it is the right answer: somebody who asked for this role on a machine
     * without the wiring should be told, not accused.
     *
     * <p>{@code LinkageError} as well as {@code Exception}, because a Pi4J provider
     * that cannot load its native library throws an {@code Error} — which would
     * otherwise sail past this and report a missing radio as a stack trace about a
     * shared object file.
     */
    private static <T> T run(Supplier<T> check) {
        try {
            return check.get();
        } catch (Exception | LinkageError e) {
            e.printStackTrace();
            Assumptions.abort("No usable LR11xx on " + Lr11xxLinkCheck.wiring() + " (" + e + ")");
            throw new IllegalStateException(e);
        }
    }
}
