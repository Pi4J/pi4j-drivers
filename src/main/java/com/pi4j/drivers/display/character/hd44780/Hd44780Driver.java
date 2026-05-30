package com.pi4j.drivers.display.character.hd44780;

import com.pi4j.drivers.io.expander.mcp23008.Mcp23008Driver;
import com.pi4j.io.OnOffWrite;
import com.pi4j.io.i2c.I2C;
import com.pi4j.drivers.io.expander.pcf8574.Pcf8574OutputDriver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Used for 16x2 (1602) and similar (20x4/2004...) character LCD driven by a HD 44780 chip.
 *
 * Depending on the display IO connection, use with4BitConnection() for a 4-bit parallel connection or
 * withPcf8574Connection() for the most common I2C connection type.
 *
 * Spec: https://cdn.sparkfun.com/assets/9/5/f/7/b/HD44780.pdf
 */
public class Hd44780Driver {

    // Specified values are multiplied by 2 to allow for the minimum frequency.
    private static final int STANDARD_DELAY_MICROS = 2 * 37;
    private static final int LONG_DELAY_MICROS = 2 * 1520;

    /**
     * The standard character rom consisting of most ASCII characters and JIS X 0201 and some extra greek characters
     * and umlauts. Most notably, backslash and tilde are missing.
     */
    public static final Map<Integer, Integer> CHARACTER_ROM_A00 = generateCharacterMap(
        "βß",
            0x5c, "¥",
            126, "←→",
            0xa1, "｡｢｣､･ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝﾞﾟ",
            224, "αäßɛμσρ",
            232, "√",
            236, "¢",
            238, "ñö",
            242, "Θ∞ΩüΣπ",
            253, "÷",
            255, "█"
    );

    /**
     * Character ROM mostly corresponding to ISO-8859-1 with a few extra cyrillic and greek characters sprinkled in.
     */
    public static final Map<Integer, Integer> CHARACTER_ROM_A02 = generateCharacterMap(
            "АAВBЕEКKМMНHОOРPСCТT",
            8, "◀▶“”\u23eb\u23ec●⏎↑↓←→≤≥▲▼",
            128, "БДЖЗИЙЛПУЦЧШЩЪЫЭα♪ΓπΣστ⍾ΘΩδ∞❤εΠ",
            0xac, "ЮЯ");


    private final int width;
    private final int height;
    private final AbstractConnection connection;
    private final int[] customCharacterToCodePoint = new int[8];
    private final Map<Integer, Integer> codePointToCustomCharacter = new HashMap<>();

    private int cursorX = 0;
    private int cursorY = 0;
    private boolean cursorEnabled = false;
    private boolean displayEnabled = false;
    private boolean blinkingEnabled = false;
    private Map<Integer, Integer> characterRomMap = CHARACTER_ROM_A00;

    /** Creates a HD 44680 Driver with a parallel 4-bit 7-pin connection. */
    public static Hd44780Driver with4BitConnection(
            OnOffWrite<?> registerSelect,
            OnOffWrite<?> enable,
            OnOffWrite<?> backLight,
            OnOffWrite<?> d4,
            OnOffWrite<?> d5,
            OnOffWrite<?> d6,
            OnOffWrite<?> d7,
            int width,
            int height) {
        return new Hd44780Driver(
                new Parallel4BitConnection(registerSelect, enable, backLight, d4, d5, d6, d7),
                width,
                height);
    }

    /**
     * Creates a HD 44780 Driver for a text LCD connected via I2C using a PCF 8574 IO expander. This implementation
     * uses the Pcf8574OutputDriver class to illustrate how different drivers can be combined (opposed to a simple
     * direct AbstractConnection subclass).
     * <p>
     * This method assumes the following pin connections:
     * 0: RS, 2: Enable, 3: Backlight, 4-7: D4-D7
     * <p>
     * Most PCF 8574 based text LCDs seem to have a "daughter board" with the PC 8574 chip attached to the back
     * of the main LCD board, connected to the data and control lines of the main LCD controller board.
     */
    public static Hd44780Driver withPcf8574Connection(I2C i2c, int width, int height) {
        Pcf8574OutputDriver pcf8574 = new Pcf8574OutputDriver(i2c);
        pcf8574.setTriggerMask(0b0100);
        pcf8574.setOutput(0);
        return with4BitConnection(
                /* rs */ pcf8574.getOnOffWrite(0),
                /* enable */ pcf8574.getOnOffWrite(2),
                /* backlight*/ pcf8574.getOnOffWrite(3),
                /* d4 */ pcf8574.getOnOffWrite(4),
                /* d5 */ pcf8574.getOnOffWrite(5),
                /* d6 */ pcf8574.getOnOffWrite(6),
                /* d7 */ pcf8574.getOnOffWrite(7),
                width,
                height);
    }

    /**
     * Creates a HD 44680 Driver for a text LCD connected via I2C using a MCP 23008 IO expander. This implementation
     * uses the Mcp23008Driver class to illustrate how different drivers can be combined (opposed to a simple
     * direct AbstractConnection subclass).
     * <p>
     * This call assumes the following pin connections, as used in CrowPi2:
     * 1: RS, 2: Enable, 3-6: Data 4-7, 7: Backlight.
     */
    public static Hd44780Driver withMcp23008Connection(I2C i2c, int width, int height) {
        Mcp23008Driver mcp23008 = new Mcp23008Driver(i2c);
        mcp23008.setIoDir(0); // All pins configured for output.
        mcp23008.setTriggerMask(0b0100);
        mcp23008.setOutput(0);
        return with4BitConnection(
                /* registerSelect */ mcp23008.getOnOffWrite(1),
                /* enable */ mcp23008.getOnOffWrite(2),
                /* backLight */ mcp23008.getOnOffWrite(7),
                /* d4 */ mcp23008.getOnOffWrite(3),
                /* d5 */ mcp23008.getOnOffWrite(4),
                /* d6 */ mcp23008.getOnOffWrite(5),
                /* d7 */ mcp23008.getOnOffWrite(6),
                width,
                height);
    }

    /**
     * Creates a driver for an AIP31068 LCD controller with native I2C support. These typically only consist of a
     * single board without a separate I2C daughter board.
     */
    public static Hd44780Driver withAip31068Connection(I2C i2c, int width, int height) {
        return new Hd44780Driver(new Aip31068Connection(i2c), width, height);
    }

    /**
     * Please use one of the "withXxx" factory methods to create a driver instance. This method is public
     * to support user defined connection implementations.
     */
    public Hd44780Driver(AbstractConnection connection, int width, int height) {
        this.connection = connection;
        this.width = width;
        this.height = height;

        // Initialize display settings
        int fsCmd = CommandCodes.CMD_FUNCTION_SET
                | (height == 1 ? CommandCodes.FS_1_LINE : CommandCodes.FS_2_LINES)
                | (connection.is8Bit() ? CommandCodes.FS_8_BIT : CommandCodes.FS_4_BIT);

        if (connection.is8Bit()) {
            fsCmd |= CommandCodes.FS_8_BIT;
            connection.setDelayMicros(50_000);
            connection.sendValue(AbstractConnection.Mode.INIT, fsCmd);
            connection.setDelayMicros(4500);
            connection.sendValue(AbstractConnection.Mode.INIT, fsCmd);
            connection.setDelayMicros(100);
            connection.sendValue(AbstractConnection.Mode.INIT, fsCmd);
        } else {
            fsCmd |= CommandCodes.FS_4_BIT;
            connection.setDelayMicros(50_000);
            connection.sendValue(AbstractConnection.Mode.INIT, 3);
            connection.setDelayMicros(4500);
            connection.sendValue(AbstractConnection.Mode.INIT, 3);
            connection.setDelayMicros(100);
            connection.sendValue(AbstractConnection.Mode.INIT, 3);
            connection.setDelayMicros(100);
            connection.sendValue(AbstractConnection.Mode.INIT, 2);
        }

        sendCommand(fsCmd);
        sendCommand(CommandCodes.CMD_ENTRY_MODE | CommandCodes.EM_INCREMENT | CommandCodes.EM_DISPLAY_SHIFT_OFF);

        setDisplayEnabled(true);
        setBacklightEnabled(true);

        clearDisplay();
        returnHome();
    }

    /**
     * Sets the character ROM mapping of the chip, translating unicode code points to display characters. By default,
     * this is CHARACTER_ROM_A00. The mapping selected here needs to match the internal mapping of the chip in order
     * to display supported unicode characters correctly.
     */
    public void setCharacterRom(Map<Integer, Integer> characterRomMap) {
        this.characterRomMap = characterRomMap;
    }

    public void clearDisplay() {
        sendCommand(CommandCodes.CMD_CLEAR_DISPLAY);
        connection.setDelayMicros(LONG_DELAY_MICROS);
        cursorX = 0;
        cursorY = 0;
    }

    /**
     * Returns the Cursor to Home Position (First line, first character)
     */
    public void returnHome() {
        sendCommand(CommandCodes.CMD_RETURN_HOME);
        connection.setDelayMicros(LONG_DELAY_MICROS);
        cursorX = 0;
        cursorY = 0;
    }

    public void setBacklightEnabled(boolean backlightEnabled) {
        connection.setBacklight(backlightEnabled);
    }

    public void setCursorEnabled(boolean enabled) {
        this.cursorEnabled = enabled;
        updateDisplayControl();
    }

    public void setDisplayEnabled(boolean enabled) {
        this.displayEnabled = enabled;
        updateDisplayControl();
    }

    public void setBlinkingEnabled(boolean enabled) {
        this.blinkingEnabled = enabled;
        updateDisplayControl();
    }

    /**
     * Write a text at the current cursor position
     */
    public void write(String text) {
        final int length = text.length();
        for (int offset = 0; offset < length; ) {
            final int codepoint = text.codePointAt(offset);
            write(codepoint);
            offset += Character.charCount(codepoint);
        }
    }

    /**
     * Write a character at the current cursor position
     */
    public void write(int codePoint) {
        if (codePoint == '\n' || cursorX >= width) {
            setCursorPosition(0, (cursorY + 1) % height);
            if (codePoint == '\n') {
                return;
            }
        }
        sendData(mapCodePoint(codePoint));
        cursorX++;
    }

    /**
     * Write a text on the given position by setting the cursor position
     */
    public void writeAt(int x, int y, String text) {
        if (y > height) {
            throw new IllegalArgumentException("Line " + y + " out of range 1.." + height);
        }
        setCursorPosition(x, y);
        write(text);
    }


    /** Sets the cursor to the given column/row (x/y) position. */
    public void setCursorPosition(int x, int y) {
        if (y >= height || y < 0) {
            throw new IllegalArgumentException("Row " + y + " out of range 0.." + (height - 1));
        }
        if (x >= width || x < 0) {
            throw new IllegalArgumentException("Column " + x + " out of range 0.." + (width - 1));
        }
        sendCommand(CommandCodes.CMD_SET_DDRAM_ADDR | x + LCD_ROW_OFFSETS[y]);
        this.cursorX = x;
        this.cursorY = y;
    }

    /**
     * Uploads character data to the given position.
     */
    public void uploadCharacter(int index, long characterData, int codePoint) {
        codePointToCustomCharacter.remove(customCharacterToCodePoint[codePoint]);
        customCharacterToCodePoint[index] = codePoint;
        codePointToCustomCharacter.put(codePoint, index);
        if (index > 7 || index < 1) {
            throw new IllegalArgumentException("Custom character index " + index + " outside valid range (1..7)");
        }
        sendCommand(CommandCodes.CMD_SET_CGRAM_ADDR | index << 3);

        for (int i = 0; i < 8; i++) {
            sendData((int) ((characterData >>> ((7-i) * 8)) & 0xffL));
        }
    }

    /**
     * Scroll whole display to the right by one column.
     */
    public void scrollRight(){
        sendCommand(CommandCodes.CMD_DISPLAY_SHIFT_RIGHT);
    }

    /**
     * Scroll whole display to the left by one column.
     */
    public void scrollLeft(){
        sendCommand(CommandCodes.CMD_DISPLAY_SHIFT_LEFT);
    }


    // Private helpers

    private static Map<Integer, Integer> generateCharacterMap(String lookalikes, Object... data) {
        int index = 0;
        Map<Integer, Integer> map = new HashMap<>();
        while (index < data.length) {
            int target = (Integer) data[index++];
            String values = (String) data[index++];
            for (int i = 0; i < values.length(); i++) {
                map.put((int) values.charAt(i), target + i);
            }
        }
        for (int i = 0; i < lookalikes.length(); i+=2) {
            int target = lookalikes.charAt(i+1);
            map.put((int) lookalikes.charAt(i), map.getOrDefault(target, target));
        }
        return Collections.unmodifiableMap(map);
    }


    private int mapCodePoint(int codePoint) {
        Integer custom = codePointToCustomCharacter.get(codePoint);
        if (custom != null) {
            return custom;
        }
        Integer rom = characterRomMap.get(codePoint);
        if (rom != null) {
            return rom;
        }
        return codePoint;
    }

    private void updateDisplayControl() {
        sendCommand(CommandCodes.CMD_DISPLAY_CONTROL
                | (displayEnabled ? CommandCodes.DC_DISPLAY_ON : CommandCodes.DC_DISPLAY_OFF)
                | (cursorEnabled ? CommandCodes.DC_CURSOR_ON : CommandCodes.DC_CURSOR_OFF)
                | (blinkingEnabled ? CommandCodes.DC_BLINK_ON : CommandCodes.DC_BLINK_OFF)
        );
    }

    private void sendCommand(int id) {
        connection.sendValue(AbstractConnection.Mode.COMMAND, id);
        connection.setDelayMicros(STANDARD_DELAY_MICROS);
    }

    private void sendData(int value) {
        connection.sendValue(AbstractConnection.Mode.DATA, value);
    }


    /** Display row offsets for up to 4 rows. */
    private static final byte[] LCD_ROW_OFFSETS = {0x00, 0x40, 0x14, 0x54};
}
