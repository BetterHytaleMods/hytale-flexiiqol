package de.hymods.flexiisgoggles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.hymods.flexiisgoggles.settings.PlayerSettings;
import de.hymods.flexiisgoggles.zoom.ZoomSpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * {@code config.json} in the plugin's data folder: what the server allows, and what players get until
 * they pick their own on {@code /goggles}. Missing fields keep their defaults, values out of range are
 * clamped, and the file is written back on every load, so new settings show up in it.
 */
public final class GogglesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public Zoom zoom = new Zoom();
    public Fullbright fullbright = new Fullbright();

    public static final class Zoom {
        public boolean enabled = true;
        /** How many times narrower the view gets, 2 to 12, relative to the player's own field of view. */
        public int magnification = 4;
        /** Mouse sensitivity at 4x, as a share of the normal one; the other steps scale it. */
        public float sensitivity = 0.35f;
        /** Share of the remaining zoom covered per movement tick: 1 is instant. */
        public float inLerp = 0.15f;
        public float outLerp = 0.25f;
        /** How much the camera trails the mouse while zoomed: 0 is off, 1.8 the most. */
        public float glide = 0.5f;

        /** The zoom step {@code magnification}, with the mouse slowed down to match. */
        public ZoomSpec spec(int magnification) {
            float sensitivity = Math.clamp(this.sensitivity * 4f / magnification, 0.05f, 1f);
            return new ZoomSpec(magnification, sensitivity, this.inLerp, this.outLerp);
        }

        void normalize() {
            this.magnification = Math.clamp(this.magnification, PlayerSettings.ZOOM_MIN, PlayerSettings.ZOOM_MAX);
            this.sensitivity = clamp(this.sensitivity, 0.05f, 1f, 0.35f);
            this.inLerp = clamp(this.inLerp, 0.01f, 1f, 0.15f);
            this.outLerp = clamp(this.outLerp, 0.01f, 1f, 0.25f);
            this.glide = Math.round(clamp(this.glide, 0f, PlayerSettings.GLIDE_MAX / 10f, 0.5f) * 10f) / 10f;
        }
    }

    public static final class Fullbright {
        public boolean enabled = true;
        /** Brightness for players who switch fullbright on, 1 to 6. */
        public int level = 3;

        void normalize() {
            this.level = Math.clamp(this.level, PlayerSettings.BRIGHTNESS_MIN, PlayerSettings.BRIGHTNESS_MAX);
        }
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
        if (this.fullbright == null) {
            this.fullbright = new Fullbright();
        }
        this.zoom.normalize();
        this.fullbright.normalize();
    }

    private static float clamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }
}
