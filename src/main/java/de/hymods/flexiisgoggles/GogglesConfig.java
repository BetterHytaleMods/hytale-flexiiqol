package de.hymods.flexiisgoggles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.hymods.flexiisgoggles.goggles.Goggles;
import de.hymods.flexiisgoggles.settings.PlayerSettings;
import de.hymods.flexiisgoggles.zoom.ZoomSpec;
import de.hymods.flexiisgoggles.zoom.ZoomSteps;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code config.json} in the plugin's data folder. Missing fields keep their defaults, values out of
 * range are clamped, and the file is written back on every load, so new settings show up in it.
 */
public final class GogglesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** Zoom and night vision only while wearing goggles; false gives both to everyone. */
    public boolean requireGoggles = true;
    public Zoom zoom = new Zoom();
    public NightVision nightVision = new NightVision();
    /** How strong each pair of goggles is on this server: at most what they can do, lower if wanted. */
    public Map<String, Limits> goggles = new LinkedHashMap<>();

    public static final class Zoom {
        public boolean enabled = true;
        /** Zoom strength without goggles (requireGoggles false), until a player picks their own, 1.5 to 16. */
        public float magnification = 4f;
        /** Mouse sensitivity at 4x, as a share of the normal one; the other strengths scale it. */
        public float sensitivity = 0.35f;
        /** Share of the remaining zoom covered per movement tick: 1 is instant. */
        public float inLerp = 0.15f;
        public float outLerp = 0.25f;
        /** How much the camera trails the mouse while zoomed: 0 is off, 1.8 the most. */
        public float glide = 0.5f;

        /** The zoom at {@code magnification}, with the mouse slowed down to match. */
        public ZoomSpec spec(float magnification) {
            float sensitivity = Math.clamp(this.sensitivity * 4f / magnification, 0.05f, 1f);
            return new ZoomSpec(magnification, sensitivity, this.inLerp, this.outLerp);
        }

        void normalize() {
            this.magnification = ZoomSteps.snap(clamp(this.magnification, ZoomSteps.MIN, ZoomSteps.MAX, 4f));
            this.sensitivity = clamp(this.sensitivity, 0.05f, 1f, 0.35f);
            this.inLerp = clamp(this.inLerp, 0.01f, 1f, 0.15f);
            this.outLerp = clamp(this.outLerp, 0.01f, 1f, 0.25f);
            this.glide = Math.round(clamp(this.glide, 0f, PlayerSettings.GLIDE_MAX / 10f, 0.5f) * 10f) / 10f;
        }
    }

    public static final class NightVision {
        public boolean enabled = true;
        /** Brightness without goggles (requireGoggles false), until a player picks their own, 1 to 6. */
        public int level = 3;

        void normalize() {
            this.level = Math.clamp(this.level, PlayerSettings.BRIGHTNESS_MIN, PlayerSettings.BRIGHTNESS_MAX);
        }
    }

    /** One pair of goggles on this server; null where the goggles don't have that. */
    public static final class Limits {
        public Float zoom;
        public Integer nightVision;
    }

    /** How far these goggles zoom here; 0 if they don't. */
    public float zoomCap(Goggles goggles) {
        Limits limits = this.goggles.get(goggles.key());
        return limits == null || limits.zoom == null ? goggles.maxZoom() : limits.zoom;
    }

    /** How bright night vision gets with these goggles here; 0 if they have none. */
    public int nightVisionCap(Goggles goggles) {
        Limits limits = this.goggles.get(goggles.key());
        return limits == null || limits.nightVision == null ? goggles.maxNightVision() : limits.nightVision;
    }

    public static GogglesConfig load(Path file) throws IOException {
        GogglesConfig config = Files.exists(file)
                ? GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), GogglesConfig.class)
                : new GogglesConfig();
        if (config == null) {
            config = new GogglesConfig();
        }
        config.normalize();
        config.save(file);
        return config;
    }

    public void save(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(this) + "\n", StandardCharsets.UTF_8);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    void normalize() {
        if (this.zoom == null) {
            this.zoom = new Zoom();
        }
        if (this.nightVision == null) {
            this.nightVision = new NightVision();
        }
        this.zoom.normalize();
        this.nightVision.normalize();
        Map<String, Limits> given = this.goggles == null ? Map.of() : this.goggles;
        Map<String, Limits> normalized = new LinkedHashMap<>();
        for (Goggles goggles : Goggles.values()) {
            Limits old = given.get(goggles.key());
            Limits limits = new Limits();
            if (goggles.maxZoom() > 0f) {
                float zoom = old == null || old.zoom == null || !Float.isFinite(old.zoom) ? goggles.maxZoom() : old.zoom;
                limits.zoom = ZoomSteps.value(ZoomSteps.maxIndex(Math.clamp(zoom, ZoomSteps.MIN, goggles.maxZoom())));
            }
            if (goggles.maxNightVision() > 0) {
                int level = old == null || old.nightVision == null ? goggles.maxNightVision() : old.nightVision;
                limits.nightVision = Math.clamp(level, PlayerSettings.BRIGHTNESS_MIN, goggles.maxNightVision());
            }
            normalized.put(goggles.key(), limits);
        }
        this.goggles = normalized;
    }

    private static float clamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }
}
