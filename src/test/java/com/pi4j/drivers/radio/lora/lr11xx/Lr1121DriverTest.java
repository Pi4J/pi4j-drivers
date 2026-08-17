package com.pi4j.drivers.radio.lora.lr11xx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * The bytes this driver puts on the wire, checked against the ones Semtech's own
 * C driver sends.
 *
 * <p>That is the whole point of this test class. The radio cannot be asked
 * whether a command was understood — a wrong byte produces a receiver that hears
 * nothing, with no error anywhere — so the only way to be sure without hardware
 * is to compare against a reference implementation, command by command. The
 * reference is SWDR001, which Waveshare ships with the Core1121 demo.
 */
class Lr1121DriverTest {

    private final RecordingIo transport = new RecordingIo();
    private final Lr1121Driver radio = new Lr1121Driver(transport);

    // ------------------------------------------------------------------
    // The shape every command has
    // ------------------------------------------------------------------

    /**
     * A command is a big-endian opcode and then its arguments. Getting the byte
     * order backwards is the mistake that produces a radio which answers nothing,
     * so it is worth one test of its own.
     */
    @Test
    void aCommandIsABigEndianOpcodeFollowedByItsArguments() {
        radio.standby();

        // SetStandby, 0x011C, with the "RC oscillator" argument.
        assertEquals("011C00", transport.transactions().getFirst());
    }

    /**
     * A command that answers takes two transactions with a wait between them, and
     * the radio sends a status byte before the answer. Dropping that byte is the
     * difference between reading a firmware version and reading nonsense shifted
     * by one.
     */
    @Test
    void anAnswerArrivesAfterAStatusByteThatIsNotPartOfIt() {
        transport.willAnswer(0x22, 0x01, 0x03, 0x07);

        Lr1121Driver.Version version = radio.version();

        assertEquals("0101", transport.transactions().getFirst());
        assertEquals(0x22, version.hardware());
        assertEquals(0x01, version.useCase());
        assertEquals(0x0307, version.firmware());
    }

    /** The radio is asked to be ready before every transaction, never assumed to be. */
    @Test
    void theBusyLineIsWaitedForBeforeSpeaking() {
        transport.willAnswer(0x00, 0x00, 0x00, 0x00);

        radio.version();

        assertEquals(2, transport.readyWaits,
                "once before the command and once before reading the answer");
    }

    // ------------------------------------------------------------------
    // Bringing the radio up
    // ------------------------------------------------------------------

    /** An LR1121 that has finished booting, which configure() waits for. */
    private void radioHasBooted() {
        transport.willAnswer(0x22, 0x03, 0x01, 0x01);
    }

    /**
     * The board's own values reach the chip unchanged. These are the numbers that
     * cannot be derived from anything — they describe how the module was wired —
     * so the test states them literally, as the vendor's demo does.
     */
    @Test
    void theBoardConfigurationIsSentAsTheVendorSendsIt() {
        radioHasBooted();
        radio.configure(Lr1121Driver.BoardConfig.core1121());

        // SetTcxoMode: 3.0 V, 300 ticks of 30.52 us.
        assertTrue(transport.wrote("01170600012C"), transport.transactions().toString());
        // SetDioAsRfSwitch: enable RFSW0|RFSW1, standby 0, rx RFSW0, tx RFSW1,
        // tx_hp RFSW1, tx_hf 0, gnss 0, wifi 0.
        assertTrue(transport.wrote("01120300010202000000"), transport.transactions().toString());
    }

    /**
     * The oscillator is configured before the calibration, not after. Calibrating
     * first trims the radio against a clock it is about to stop using, and the
     * result is a quiet receiver rather than an error.
     */
    @Test
    void theOscillatorIsConfiguredBeforeCalibrating() {
        radioHasBooted();
        radio.configure(Lr1121Driver.BoardConfig.core1121());

        int tcxo = indexOfCommand("0117");
        int calibrate = indexOfCommand("010F");
        assertTrue(tcxo < calibrate,
                "SetTcxoMode at %d should come before Calibrate at %d".formatted(tcxo, calibrate));
    }

    @Test
    void configuringResetsTheRadioFirst() {
        radioHasBooted();
        radio.configure(Lr1121Driver.BoardConfig.core1121());

        assertEquals(1, transport.resets);
    }

    /** A board with no crystal oscillator is not told to wait for one. */
    @Test
    void aBoardWithoutATcxoIsNotSentTheCommand() {
        Lr1121Driver.BoardConfig noTcxo = new Lr1121Driver.BoardConfig(Lr1121Driver.BoardConfig.NO_TCXO, 0,
                Lr1121Driver.BoardConfig.RFSW0, 0, Lr1121Driver.BoardConfig.RFSW0, 0, 0, 0,
                Lr1121Driver.BoardConfig.PA_HIGH_POWER, Lr1121Driver.BoardConfig.PA_SUPPLY_BATTERY, 0x04, 0x07);

        radioHasBooted();
        radio.configure(noTcxo);

        assertTrue(transport.transactions().stream().noneMatch(t -> t.startsWith("0117")),
                "SetTcxoMode should not be sent: " + transport.transactions());
    }

    /**
     * An LR11xx starts in a bootloader that checks its flash and then hands over
     * to the application firmware. Asked during that window it answers as the
     * bootloader, which accepts firmware updates and refuses everything else — so
     * configuring immediately produces a radio that fails every command for a
     * reason that looks nothing like impatience.
     */
    @Test
    void configuringWaitsForTheBootloaderToHandOver() {
        transport.willAnswer(0x22, 0xDF, 0x00, 0x00);   // still booting
        transport.willAnswer(0x22, 0xDF, 0x00, 0x00);   // still booting
        radioHasBooted();

        radio.configure(Lr1121Driver.BoardConfig.core1121());

        assertTrue(transport.wrote("01170600012C"),
                "and once it has, the configuration goes out as usual");
    }

    /** A module whose firmware is genuinely missing says so rather than hanging. */
    @Test
    void aRadioThatNeverLeavesItsBootloaderIsReported() {
        transport.willKeepAnswering(0x22, 0xDF, 0x00, 0x00);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> radio.configure(Lr1121Driver.BoardConfig.core1121()));

        assertTrue(failure.getMessage().contains("bootloader"), failure.getMessage());
    }

    // ------------------------------------------------------------------
    // The modulation
    // ------------------------------------------------------------------

    @Test
    void theFrequencyIsSentAsFourBigEndianBytes() {
        radio.configureLora(868_100_000L, Lr1121Driver.LoraSettings.defaults());

        // 868100000 = 0x33BE27A0
        assertTrue(transport.wrote("020B33BE27A0"), transport.transactions().toString());
    }

    @Test
    void theModulationIsSpreadingFactorBandwidthCodingRateAndTheOptimisation() {
        radio.configureLora(868_100_000L, Lr1121Driver.LoraSettings.defaults());

        // SF9, BW 125 kHz, CR 4/5, low data rate optimisation off.
        assertEquals("020F09040100", transport.transactionFor("020F"));
    }

    /**
     * The low data rate optimisation is derived rather than configured, because
     * both ends have to make the same decision and a link where one of them did
     * not is a link that silently does not work. It is required when a symbol
     * lasts longer than 16 ms.
     */
    @Test
    void theLowDataRateOptimisationFollowsFromTheSymbolLength() {
        // SF9 at 125 kHz: 4.1 ms per symbol.
        assertEquals(false, Lr1121Driver.LoraSettings.defaults().lowDataRateOptimisation());
        // SF11: 16.4 ms, just over the line.
        assertEquals(true, Lr1121Driver.LoraSettings.defaults().withSpreadingFactor(11).lowDataRateOptimisation());
        assertEquals(true, Lr1121Driver.LoraSettings.defaults().withSpreadingFactor(12).lowDataRateOptimisation());
        // SF11 at 500 kHz is back under it.
        assertEquals(false, new Lr1121Driver.LoraSettings(11, Lr1121Driver.LoraSettings.BW_500_KHZ, Lr1121Driver.LoraSettings.CR_4_5,
                Lr1121Driver.LoraSettings.SYNC_WORD_PRIVATE, 8, true, true, false).lowDataRateOptimisation());
    }

    /**
     * The image calibration is done for the band the radio was just pointed at,
     * because it is the one calibration that is about a band rather than about
     * the chip. Skipping it costs sensitivity and reports nothing at the time,
     * which is the worst way for a radio to be wrong.
     *
     * <p>The band edges are in units of 4 MHz rounded outwards, so 868.1 MHz
     * becomes 860 to 880 — one step wider than the 0xD7 to 0xDB the vendor uses
     * for 863-870, and containing it. Wider costs nothing here: the calibration
     * is of a filter, and a band that covers the channel is what it needs to be.
     */
    @Test
    void theImageIsCalibratedForTheBandInUse() {
        radio.configureLora(868_100_000L, Lr1121Driver.LoraSettings.defaults());

        assertEquals("0111D7DC", transport.transactionFor("0111"));
        assertTrue(indexOfCommand("020B") < indexOfCommand("0111"),
                "the frequency is set before the band around it is calibrated");
    }

    /** And it follows the frequency rather than being fixed to one band. */
    @Test
    void anotherBandIsCalibratedForItself() {
        radio.configureLora(434_000_000L, Lr1121Driver.LoraSettings.defaults());

        // (434 - 8) / 4 = 106.5 -> 106 = 0x6A, (434 + 8) / 4 = 110.5 -> 111 = 0x6F
        assertEquals("01116A6F", transport.transactionFor("0111"));
    }

    @Test
    void theSyncWordSeparatesAPrivateLinkFromALoRaWANOne() {
        radio.configureLora(868_100_000L, Lr1121Driver.LoraSettings.defaults());

        assertEquals("022B12", transport.transactionFor("022B"));
    }

    // ------------------------------------------------------------------
    // Receiving
    // ------------------------------------------------------------------

    /**
     * GetStatus is a direct read: bytes clocked out with no command sent first,
     * and no status byte in front of them because the first byte is one.
     *
     * <p>Worth a test of its own because of how it failed. Read as an ordinary
     * command, the interrupt word comes out shifted one byte left, so RX_DONE
     * reads as 0x0800 instead of 0x08 — the interrupt fires, the flags are read,
     * and every packet is dropped as "not a reception". Nothing errors anywhere.
     */
    @Test
    void theInterruptFlagsAreReadWithoutSendingACommand() {
        transport.willAnswerDirectly(0x00, 0x00, 0x00, 0x00, 0x00, 0x08);

        assertEquals(Lr1121Driver.IRQ_RX_DONE, radio.irqStatus());
        assertEquals(0, transport.transactionCount(),
                "a direct read sends nothing at all");
    }

    /** And the whole 32 bit word is assembled, not just its last byte. */
    @Test
    void theWholeInterruptWordIsRead() {
        transport.willAnswerDirectly(0x00, 0x00, 0x00, 0x40, 0x00, 0x08);

        assertEquals(0x00400008, radio.irqStatus());
    }

    @Test
    void aPacketIsReadFromWhereTheRadioSaysItIs() {
        transport.willAnswerDirectly(0x00, 0x00, 0x00, 0x00, 0x00, 0x08);   // GetStatus: RX done
        transport.willAnswer(0x03, 0x80);                            // 3 bytes at offset 0x80
        transport.willAnswer(0xAA, 0xBB, 0xCC);                      // ReadBuffer8
        transport.willAnswer(0x94, 0x14, 0x00);                      // RSSI -74, SNR 5

        Lr1121Driver.ReceivedPacket packet = radio.receive(Lr1121Driver.LoraSettings.defaults(), Duration.ofSeconds(1))
                .orElseThrow();

        assertArrayEquals(new byte[] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, packet.payload());
        // ReadBuffer8 is given the offset and the length the radio just reported.
        assertEquals("010A8003", transport.transactionFor("010A"));
    }

    /** The two numbers that say whether a link is comfortable or marginal. */
    @Test
    void theSignalQualityIsDecodedAsTheDatasheetScalesIt() {
        transport.willAnswerDirectly(0x00, 0x00, 0x00, 0x00, 0x00, 0x08);
        transport.willAnswer(0x01, 0x00);
        transport.willAnswer(0x42);
        transport.willAnswer(0x94, 0x14, 0x00);

        Lr1121Driver.ReceivedPacket packet = radio.receive(Lr1121Driver.LoraSettings.defaults(), Duration.ofSeconds(1))
                .orElseThrow();

        assertEquals(-74, packet.rssiDbm(), "0x94 is 148, and RSSI is minus half of it");
        assertEquals(5.0, packet.snrDb(), 0.01, "0x14 is 20, and SNR is a quarter of it");
    }

    /**
     * A packet the radio knows is corrupt is not a packet. Handing the bytes back
     * with a warning would put the decision in every caller, and the answer is
     * always the same.
     */
    @Test
    void aPacketThatFailedItsCrcIsNotReturned() {
        transport.willAnswerDirectly(0x00, 0x00, 0x00, 0x00, 0x00, 0x88);   // RX done and CRC error

        assertEquals(Optional.empty(),
                radio.receive(Lr1121Driver.LoraSettings.defaults(), Duration.ofSeconds(1)));
    }

    /** Hearing nothing is the ordinary case, not a failure. */
    @Test
    void aTimeoutIsEmptyRatherThanAnException() {
        transport.interruptFires = false;
        transport.willKeepAnsweringDirectly(0, 0, 0, 0, 0, 0);   // no flags, ever

        assertEquals(Optional.empty(),
                radio.receive(Lr1121Driver.LoraSettings.defaults(), Duration.ofMillis(50)));
    }

    /**
     * The interrupt line is a hint and the flags are the truth. A receiver whose
     * interrupt wire is dead still hears packets, a fifth of a second late — which
     * is the difference between a link that works and one that is silent for a
     * reason nothing reports.
     */
    @Test
    void aPacketIsFoundEvenIfTheInterruptNeverFires() {
        transport.interruptFires = false;
        transport.willAnswerDirectly(0x00, 0x00, 0x00, 0x00, 0x00, 0x08);
        transport.willAnswer(0x01, 0x00);
        transport.willAnswer(0x42);
        transport.willAnswer(0x94, 0x14, 0x00);

        assertTrue(radio.receive(Lr1121Driver.LoraSettings.defaults(), Duration.ofSeconds(1)).isPresent());
    }

    /**
     * The receiver is told to listen indefinitely and the waiting is done on this
     * side. Leaving it to the radio's own timeout would mean a receiver that
     * cannot be interrupted.
     */
    @Test
    void theRadioIsToldToListenUntilItIsToldOtherwise() {
        transport.interruptFires = false;
        transport.willKeepAnsweringDirectly(0, 0, 0, 0, 0, 0);

        radio.receive(Lr1121Driver.LoraSettings.defaults(), Duration.ofMillis(50));

        assertEquals("0209000000", transport.transactionFor("0209"));
    }

    // ------------------------------------------------------------------
    // Sending
    // ------------------------------------------------------------------

    @Test
    void aPayloadIsWrittenToTheBufferBeforeItIsSent() {
        transport.willAnswerDirectly(0x00, 0x00, 0x00, 0x00, 0x00, 0x04);   // TX done

        radio.transmit(new byte[] {0x01, 0x02, 0x03}, Lr1121Driver.LoraSettings.defaults(), 14,
                Duration.ofSeconds(5));

        assertEquals("0109010203", transport.transactionFor("0109"));
        assertTrue(indexOfCommand("0109") < indexOfCommand("020A"),
                "the buffer has to be filled before the transmission starts");
    }

    /** The packet parameters carry the length, so they follow the payload. */
    @Test
    void theLengthIsSentWithThePacketParameters() {
        transport.willAnswerDirectly(0x00, 0x00, 0x00, 0x00, 0x00, 0x04);

        radio.transmit(new byte[16], Lr1121Driver.LoraSettings.defaults(), 14, Duration.ofSeconds(5));

        // preamble 8, explicit header, 16 bytes, CRC on, IQ standard.
        assertEquals("021000080010" + "0100", transport.transactionFor("0210"));
    }

    /**
     * The amplifier comes from the board, not from the power asked for.
     *
     * <p>A module routes one of the chip's outputs to its antenna and leaves the
     * others unconnected. Choosing by power — low path below 14 dBm, high above —
     * therefore transmits into a pin that goes nowhere for half the range, on a
     * board like the Core1121 that only wires the high power output. Nothing
     * reports it: the radio transmits and the far end hears the leakage.
     *
     * <p>The order is select, supply, duty cycle, size.
     */
    @Test
    void theAmplifierComesFromTheBoardRatherThanFromThePower() {
        transport.willAnswerDirectly(0x00, 0x00, 0x00, 0x00, 0x00, 0x04);
        radio.configure(Lr1121Driver.BoardConfig.core1121());
        transport.willAnswerDirectly(0x00, 0x00, 0x00, 0x00, 0x00, 0x04);

        radio.transmit(new byte[1], Lr1121Driver.LoraSettings.defaults(), 14, Duration.ofSeconds(5));

        // High power amplifier, from the battery, duty 0x04, size 0x07.
        assertEquals("021501010407", transport.transactionFor("0215"));
    }

    /** And a low power output is asked for only when the board actually has one. */
    @Test
    void aBoardWiredToItsLowPowerOutputSaysSo() {
        Lr1121Driver.BoardConfig lowPower = new Lr1121Driver.BoardConfig(Lr1121Driver.BoardConfig.NO_TCXO, 0,
                Lr1121Driver.BoardConfig.RFSW0, 0, Lr1121Driver.BoardConfig.RFSW0, 0, 0, 0,
                Lr1121Driver.BoardConfig.PA_LOW_POWER, Lr1121Driver.BoardConfig.PA_SUPPLY_REGULATOR, 0x04, 0x00);
        transport.willAnswer(0x22, 0x03, 0x01, 0x01);
        radio.configure(lowPower);
        transport.willAnswerDirectly(0x00, 0x00, 0x00, 0x00, 0x00, 0x04);

        radio.transmit(new byte[1], Lr1121Driver.LoraSettings.defaults(), 14, Duration.ofSeconds(5));

        assertEquals("021500000400", transport.transactionFor("0215"));
    }

    @Test
    void aTransmissionThatIsNeverConfirmedIsAnError() {
        transport.interruptFires = false;

        assertThrows(IllegalStateException.class, () -> radio.transmit(
                new byte[1], Lr1121Driver.LoraSettings.defaults(), 14, Duration.ofSeconds(5)));
    }

    @Test
    void aPayloadTooLargeForAPacketIsRefusedBeforeTheRadioSeesIt() {
        assertThrows(IllegalArgumentException.class, () -> radio.transmit(
                new byte[256], Lr1121Driver.LoraSettings.defaults(), 14, Duration.ofSeconds(5)));

        assertEquals(0, transport.transactionCount());
    }

    // ------------------------------------------------------------------

    @Test
    void closingTheRadioClosesTheWires() {
        radio.close();

        assertTrue(transport.closed);
    }

    private int indexOfCommand(String opcodeHex) {
        var transactions = transport.transactions();
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).startsWith(opcodeHex)) {
                return i;
            }
        }
        throw new AssertionError("No command " + opcodeHex + " among " + transactions);
    }
}
