package com.pi4j.drivers.display.graphics;

public enum PixelFormat {

    // Creates a format with 4 bits for each color channel.
    // The values for red are shifted to the left by 8 bits, green by 4 bits and blue by 0 bits.
    RGB_444(4, 4, 4, 8, 4, 0),
    // Creates a format with 5 bits for the green channel, 6 bits for the blue channel and 5 bit for the red channel
    // The values for red are shifted to the left by 11 bits, green by
    RGB_565(5, 6, 5, 11, 5, 0),
    // The same as RGB_565, but the resulting bytes are swapped to a little endian format. This only makes sense for
    // channels that are not byte-aligned but form a multiple of 8 bit, so it's special-cased in the code.
    RGB_565_LE(5, 6, 5, 11, 5, 0),
    RGB_888(8, 8, 8, 16, 8, 0),
    // The same as RGB_888, but with the shift for red and green swapped.
    GRB_888(8, 8, 8, 8, 16, 0);

    private final int redBitCount;
    private final int greenBitCount;
    private final int blueBitCount;

    private final int redShift;
    private final int greenShift;
    private final int blueShift;

    private final int redMask;
    private final int greenMask;
    private final int blueMask;

    PixelFormat(int redBitCount, int greenBitCount, int blueBitCount, int redShift, int greenShift, int blueShift) {
        this.redBitCount = redBitCount;
        this.greenBitCount = greenBitCount;
        this.blueBitCount = blueBitCount;

        this.redShift = redShift;
        this.greenShift = greenShift;
        this.blueShift = blueShift;

        this.redMask = (1 << redBitCount) - 1;
        this.greenMask = (1 << greenBitCount) - 1;
        this.blueMask = (1 << blueBitCount) - 1;
    }

    // The total number of bits used by this format.
    public int getBitCount() {
        return redBitCount + greenBitCount + blueBitCount;
    }

    /**
     * Writes count bits (up to 24) into the given buffer at the given bit offset.
     *
     * @param value
     *            The value to write.
     * @param count
     *            The number of bits (up to 24).
     * @param buffer
     *            The buffer to write the bits to
     * @param bitOffset
     *            The bit offset in the buffer
     */
    private void writeBits(int value, int count, byte[] buffer, int bitOffset) {
        int byteOffset = bitOffset / 8;

        // We need to special-case RGB_565_LE here anyway (the expected bytes are gggBbbbb, RrrrrGgg),
        // so we use this opportunity to also short-circuit other byte-aligned formats.
        switch (this) {
            case RGB_888, GRB_888 -> {
                buffer[byteOffset] = (byte) (value >>> 16);
                buffer[byteOffset + 1] = (byte) (value >>> 8);
                buffer[byteOffset + 2] = (byte) value;
            }
            case RGB_565 -> {
                buffer[byteOffset] = (byte) (value >>> 8);
                buffer[byteOffset + 1] = (byte) value;
            }
            case RGB_565_LE -> {
                buffer[byteOffset] = (byte) value;
                buffer[byteOffset + 1] = (byte) (value >>> 8);
            }
            default -> {
                bitOffset %= 8;
                int mask = ((1 << count) - 1) << (32 - count - bitOffset);

                value <<= (32 - count - bitOffset);
                while (mask != 0) {
                    buffer[byteOffset] = (byte) ((buffer[byteOffset] & ~(mask >> 24)) | (value >> 24));
                    byteOffset++;
                    value <<= 8;
                    mask <<= 8;
                }
            }
        }
    }

    /** Converts a value from a 24 bit RGB integer value to "this" pixel format. */
    int fromRgb(int rgb) {
        if (this == RGB_888) {
            return rgb;
        }
        int red = (rgb >> (24 - redBitCount)) & redMask;
        int green = (rgb >> (16 - greenBitCount)) & greenMask;
        int blue = (rgb >> (8 - blueBitCount)) & blueMask;
        return (red << redShift) | (green << greenShift) | (blue << blueShift);
    }

    /**
     * Writes a 24-bit RGB value into the given buffer in "this" pixel format at the given *bit* offset, returning the
     * number of bits written.
     */
    int writeRgb(int rgb, byte[] buffer, int bitOffset) {
        int count = redBitCount + greenBitCount + blueBitCount;
        writeBits(fromRgb(rgb), count, buffer, bitOffset);
        return count;
    }

    /**
     * Writes 24 bit integer RGB values from srcRgb to dst in "this" pixel format.
     *
     * @param srcRgb
     *            The source array with rgb values in 24 bit integers.
     * @param srcOffset
     *            The start offset in the source array
     * @param dst
     *            The destination buffer.
     * @param dstBitOffset
     *            The bit offset in the destination buffer.
     * @param pixelCount
     *            The number of pixels to be transferred.
     *
     * @return The number of bits written.
     */
    int writeRgb(int[] srcRgb, int srcOffset, byte[] dst, int dstBitOffset, int pixelCount) {
        return writeRgb(srcRgb, srcOffset, 1, dst, dstBitOffset, pixelCount);
    }

    /**
     * Writes 24 bit integer RGB values from srcRgb to dst in "this" pixel format.
     *
     * @param srcRgb
     *            The source array with rgb values in 24 bit integers.
     * @param srcOffset
     *            The start offset in the source array
     * @param srcStride
     *            The value to add to the start offset after each pixel.
     * @param dst
     *            The destination buffer.
     * @param dstBitOffset
     *            The bit offset in the destination buffer.
     * @param pixelCount
     *            The number of pixels to be transferred.
     *
     * @return The number of bits written.
     */
     int writeRgb(int[] srcRgb, int srcOffset, int srcStride, byte[] dst, int dstBitOffset, int pixelCount) {
        int bitsWritten = 0;
        for (int i = 0; i < pixelCount; i++) {
            bitsWritten += writeRgb(srcRgb[srcOffset], dst, dstBitOffset + bitsWritten);
            srcOffset += srcStride;
        }
        return bitsWritten;
    }

    /**
     * Fills the dst array with pixels of the same 24 bit rgb color, converted to "this" format.
     */
    int fillRgb(byte[] dst, int dstBitOffset, int pixelCount, int rgb) {
        int bitsWritten = 0;
        int nativeColor = fromRgb(rgb);
        int bitCount = getBitCount();
        for (int i = 0; i < pixelCount; i++) {
            writeBits(nativeColor, bitCount, dst, dstBitOffset + bitsWritten);
            bitsWritten += bitCount;
        }
        return bitsWritten;
    }
}
