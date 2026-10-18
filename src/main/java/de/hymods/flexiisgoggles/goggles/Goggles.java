package de.hymods.flexiisgoggles.goggles;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Every pair of goggles and the most it can do. Each one also comes as a helm with the goggles
 * mounted, crafted at the armor bench, which counts the same. The config can lower these values,
 * never raise them.
 */
public enum Goggles {
    ZOOM_1("Zoom_1", 1, 1.5f, 0),
    ZOOM_2("Zoom_2", 2, 3f, 0),
    ZOOM_3("Zoom_3", 3, 5f, 0),
    ZOOM_4("Zoom_4", 4, 8f, 0),
    NIGHT_VISION_1("NightVision_1", 1, 0f, 1),
    NIGHT_VISION_2("NightVision_2", 2, 0f, 2),
    NIGHT_VISION_3("NightVision_3", 3, 0f, 3),
    NIGHT_VISION_4("NightVision_4", 4, 0f, 5),
    ULTRA("Ultra", 4, 12f, 6);

    private static final Map<String, Goggles> BY_ITEM = new HashMap<>();

    static {
        for (Goggles goggles : values()) {
            BY_ITEM.put(goggles.itemId().toLowerCase(Locale.ROOT), goggles);
            BY_ITEM.put(goggles.helmId().toLowerCase(Locale.ROOT), goggles);
        }
    }

    private final String key;
    private final int tier;
    private final float maxZoom;
    private final int maxNightVision;

    Goggles(String key, int tier, float maxZoom, int maxNightVision) {
        this.key = key;
        this.tier = tier;
        this.maxZoom = maxZoom;
        this.maxNightVision = maxNightVision;
    }

    /** The name in the config and in the item ids. */
    public String key() {
        return this.key;
    }

    /** The optician's bench tier that makes them, 1 to 4. */
    public int tier() {
        return this.tier;
    }

    /** 0 if they don't zoom. */
    public float maxZoom() {
        return this.maxZoom;
    }

    /** 0 if they have no night vision. */
    public int maxNightVision() {
        return this.maxNightVision;
    }

    public String itemId() {
        return "Flexii_Goggles_" + this.key;
    }

    /** The helm with these goggles mounted. */
    public String helmId() {
        if (this == ULTRA) {
            return "Flexii_Helm_Overseer";
        }
        return (this.maxZoom > 0f ? "Flexii_Helm_Scout_" : "Flexii_Helm_Nightwatch_") + this.tier;
    }

    /** The goggles an item is or carries, or null. Item ids are case-insensitive in Hytale. */
    @Nullable
    public static Goggles byItemId(@Nullable String itemId) {
        return itemId == null ? null : BY_ITEM.get(itemId.toLowerCase(Locale.ROOT));
    }

    @Nullable
    public static Goggles byKey(@Nullable String key) {
        for (Goggles goggles : values()) {
            if (goggles.key.equalsIgnoreCase(key)) {
                return goggles;
            }
        }
        return null;
    }
}
