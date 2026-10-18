package de.hymods.flexiisgoggles.goggles;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every pair of goggles and the most it can do. Each one also comes mounted on the game's helmets of
 * its tier, crafted at the armor bench, which count the same. The config can lower these values,
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
    ULTRA("Ultra", 5, 16f, 6);

    /** The helmet metals of each tier; thorium and cobalt are both third-tier helmets. */
    private static final List<List<String>> HELMET_METALS = List.of(
            List.of("Copper"), List.of("Iron"), List.of("Thorium", "Cobalt"), List.of("Adamantite"));

    private static final Map<String, Goggles> BY_ITEM = new HashMap<>();

    static {
        for (Goggles goggles : values()) {
            BY_ITEM.put(goggles.itemId().toLowerCase(Locale.ROOT), goggles);
            for (String helm : goggles.helmIds()) {
                BY_ITEM.put(helm.toLowerCase(Locale.ROOT), goggles);
            }
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

    /** The optician's bench tier that makes them, 1 to 5. */
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

    /** The zoom a player gets before picking their own: the full strength, except 12x for the UltraGoggles. */
    public float startZoom() {
        return this == ULTRA ? 12f : this.maxZoom;
    }

    /** Whether these goggles show lumenite ore while their night vision is on; elsewhere it looks like stone. */
    public boolean revealsLumenite() {
        return this == NIGHT_VISION_4 || this == ULTRA;
    }

    public String itemId() {
        return "Flexii_Goggles_" + this.key;
    }

    /** The helms with these goggles mounted, one per helmet metal of their tier. */
    public List<String> helmIds() {
        if (this == ULTRA) {
            return List.of("Flexii_Helm_Overseer");
        }
        String kind = this.maxZoom > 0f ? "Flexii_Helm_Scout_" : "Flexii_Helm_Nightwatch_";
        return HELMET_METALS.get(this.tier - 1).stream().map(metal -> kind + metal).toList();
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
