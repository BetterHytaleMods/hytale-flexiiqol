package de.hymods.flexiisgoggles.goggles;

import de.hymods.flexiisgoggles.GogglesConfig;
import de.hymods.flexiisgoggles.settings.PlayerSettings;
import de.hymods.flexiisgoggles.zoom.ZoomSteps;

import javax.annotation.Nullable;

/**
 * What a player gets right now: as much as their goggles allow (or the server, without the goggles
 * requirement), and within that their own picks. A pick the player never made follows the goggles'
 * full strength.
 *
 * @param zoom          the zoom strength, 0 without zoom
 * @param zoomCap       the most the player can pick, 0 without zoom
 * @param glide         glide in tenths
 * @param nightVision   whether night vision is on
 * @param brightness    the night vision level picked, 0 without night vision
 * @param brightnessCap the most the player can pick, 0 without night vision
 */
public record Effective(float zoom, float zoomCap, int glide, boolean nightVision, int brightness, int brightnessCap) {

    /** What the worn goggles, or the server without the requirement, allow. 0 means none. */
    public record Ability(float zoomCap, int nightVisionCap) {
        public static final Ability NONE = new Ability(0f, 0);

        public static Ability of(GogglesConfig config, @Nullable Goggles worn) {
            float zoom;
            int nightVision;
            if (!config.requireGoggles) {
                zoom = ZoomSteps.MAX;
                nightVision = PlayerSettings.BRIGHTNESS_MAX;
            } else if (worn != null) {
                zoom = config.zoomCap(worn);
                nightVision = config.nightVisionCap(worn);
            } else {
                zoom = 0f;
                nightVision = 0;
            }
            return new Ability(config.zoom.enabled ? zoom : 0f, config.nightVision.enabled ? nightVision : 0);
        }

        public boolean any() {
            return this.zoomCap > 0f || this.nightVisionCap > 0;
        }
    }

    public static Effective of(GogglesConfig config, PlayerSettings.Choice choice, Ability ability) {
        boolean free = !config.requireGoggles;
        float zoom = 0f;
        if (ability.zoomCap() > 0f) {
            float wanted = choice.zoom() != null ? choice.zoom() : free ? config.zoom.magnification : ability.zoomCap();
            zoom = ZoomSteps.value(Math.min(ZoomSteps.index(wanted), ZoomSteps.maxIndex(ability.zoomCap())));
        }
        int glide = choice.glide() != null ? choice.glide() : Math.round(config.zoom.glide * 10f);
        int cap = ability.nightVisionCap();
        boolean on = cap > 0 && (choice.nightVision() != null ? choice.nightVision() : !free);
        int brightness = cap == 0 ? 0 : Math.min(choice.brightness() != null ? choice.brightness() : free ? config.nightVision.level : cap, cap);
        return new Effective(zoom, ability.zoomCap(), glide, on, brightness, cap);
    }

    /** The light level of air for this player, 0 for the game's own light. */
    public int nightVisionLevel() {
        return this.nightVision ? this.brightness : 0;
    }

    /** Glide as the camera takes it: 0 is off, up to 1.8. */
    public float glideAmount() {
        return this.glide / 10f;
    }
}
