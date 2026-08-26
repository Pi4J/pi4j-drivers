package com.pi4j.drivers.display.graphics.ssd1677;

/** Controls partial display updates. */
public interface PartialUpdatePolicy {

    /**
     * Called before the given screen region is updated. Tells the driver whether it should perform
     * a partial display update (fast but causes ghosting) or a full update (slow).
     */
    boolean shouldPerformPartialUpdate(int x, int y, int width, int height);

    /**
     * A default partial update policy that forces a full refresh after 100'000 pixels or 5 partial refreshes.
     */
    PartialUpdatePolicy DEFAULT_POLICY = new PartialUpdatePolicy() {
        int updateCount = 0;
        int pixelCount = 0;
        @Override
        public boolean shouldPerformPartialUpdate(int x, int y, int width, int height) {
            pixelCount += width * height;
            updateCount++;

            if (pixelCount > 100_000 || updateCount > 5) {
                pixelCount = 0;
                updateCount = 0;
                return false;
            }
            return true;
        }
    };

}
