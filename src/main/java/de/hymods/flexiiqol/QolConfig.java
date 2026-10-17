package de.hymods.flexiiqol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.hymods.flexiiqol.zoom.ZoomSpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * {@code config.json} in the plugin's data folder. Missing fields keep their defaults, values out of
 * range are clamped, and the file is written back on every load, so new settings show up in it.
 */
public final class QolConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public Zoom zoom = new Zoom();
    public Fullbright fullbright = new Fullbright();

    public static final class Zoom {
        public boolean enabled = true;
        /** How many times narrower the view gets, relative to the player's own field of view. */
        public float magnification = 4f;
        /** Mouse sensitivity while zoomed, as a share of the normal one. */
        public float sensitivity = 0.35f;
        /** Share of the remaining zoom covered per movement tick: 1 is instant. */
        public float inLerp = 0.15f;
        public float outLerp = 0.25f;
        /** How much the camera trails the mouse while zoomed: 0 is off, 0.95 the most. */
        public float glide = 0.7f;

        public ZoomSpec spec() {
            ZoomSpec defaults = ZoomSpec.defaults();
            return new ZoomSpec(this.magnification, this.sensitivity, this.inLerp, this.outLerp, defaults.perspective(),
                    defaults.overlay(), defaults.endOnClick(), defaults.depthOfField(), defaults.onItemChange());
        }

        void normalize() {
            this.magnification = clamp(this.magnification, 1.25f, 16f, 4f);
            this.sensitivity = clamp(this.sensitivity, 0.05f, 1f, 0.35f);
            this.inLerp = clamp(this.inLerp, 0.01f, 1f, 0.15f);
            this.outLerp = clamp(this.outLerp, 0.01f, 1f, 0.25f);
            this.glide = clamp(this.glide, 0f, 0.95f, 0.7f);
        }
    }

    public static final class Fullbright {
        public boolean enabled = true;
        /** Minimum block light for players with fullbright on, 1 to 15. */
        public int level = 15;

        void normalize() {
            this.level = Math.clamp(this.level, 1, 15);
        }
    }

    public static QolConfig load(Path file) throws IOException {
        QolConfig config = Files.exists(file)
                ? GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), QolConfig.class)
                : new QolConfig();
        if (config == null) {
            config = new QolConfig();
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
