package com.pi4j.drivers.display.graphics;

/** Controls partial display updates. */
public interface PartialUpdatePolicy {
    /**
     * A default partial update policy that forces a full refresh after 20 partial refreshes or 100'000 refreshed
     * pixels
     */
    PartialUpdatePolicy DEFAULT_POLICY = create(20, 100_000);

    /**
     * Called before the given screen region is updated. Tells the driver whether it should perform
     * a partial display update (fast but causes ghosting) or a full update (slow).
     */
    boolean shouldPerformPartialUpdate(int x, int y, int width, int height);

    /** Creates a policy that forces a full update after the given update or total pixel count */
    static PartialUpdatePolicy create(int updateCountLimit, int pixelCountLimit) {
        return new PartialUpdatePolicy() {
            int updateCount = 0;
            int pixelCount = 0;

            @Override
            public boolean shouldPerformPartialUpdate(int x, int y, int width, int height) {
                pixelCount += width * height;
                updateCount++;

                if (pixelCount > pixelCountLimit || updateCount > updateCountLimit) {
                    pixelCount = 0;
                    updateCount = 0;
                    return false;
                }
                return true;
            }
        };
    }
}
