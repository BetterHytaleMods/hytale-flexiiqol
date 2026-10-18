package de.hymods.flexiisgoggles.zoom;

import java.util.Locale;

/** The zoom strengths there are: 1.5x, then whole steps up to 12x. Each has its own root interaction. */
public final class ZoomSteps {
    private static final float[] VALUES = {1.5f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f};
    public static final float MIN = VALUES[0];
    public static final float MAX = VALUES[VALUES.length - 1];

    private ZoomSteps() {
    }

    public static int count() {
        return VALUES.length;
    }

    public static float value(int index) {
        return VALUES[Math.clamp(index, 0, VALUES.length - 1)];
    }

    /** The step closest to {@code value}. */
    public static int index(float value) {
        int best = 0;
        for (int i = 1; i < VALUES.length; i++) {
            if (Math.abs(VALUES[i] - value) < Math.abs(VALUES[best] - value)) {
                best = i;
            }
        }
        return best;
    }

    /** The highest step at or below {@code cap}; 0 if even the first step is above it. */
    public static int maxIndex(float cap) {
        int best = 0;
        for (int i = 1; i < VALUES.length; i++) {
            if (VALUES[i] <= cap + 1e-4f) {
                best = i;
            }
        }
        return best;
    }

    /** The step closest to {@code value}. */
    public static float snap(float value) {
        return value(index(value));
    }

    /** "1.5" or "5". */
    public static String label(float value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
