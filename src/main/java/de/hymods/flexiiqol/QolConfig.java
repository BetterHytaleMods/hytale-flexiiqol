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

    public boolean zoomEnabled = true;
    /** How many times narrower the view gets, relative to the player's own field of view. */
    public float zoomMagnification = 4f;
    /** Mouse sensitivity while zoomed, as a share of the normal one. */
    public float zoomSensitivity = 0.35f;
    /** Share of the remaining zoom covered per movement tick: 1 is instant. */
    public float zoomInLerp = 0.15f;
    public float zoomOutLerp = 0.25f;

    public boolean fullbrightEnabled = true;
    /** Minimum block light for players with fullbright on, 0 to 15. */
    public int fullbrightLevel = 15;

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

    public ZoomSpec zoomSpec() {
        ZoomSpec defaults = ZoomSpec.defaults();
        return new ZoomSpec(this.zoomMagnification, this.zoomSensitivity, this.zoomInLerp, this.zoomOutLerp,
                defaults.perspective(), defaults.overlay(), defaults.endOnClick(), defaults.depthOfField(), defaults.onItemChange());
    }

    void normalize() {
        this.zoomMagnification = clamp(this.zoomMagnification, 1.25f, 16f, 4f);
        this.zoomSensitivity = clamp(this.zoomSensitivity, 0.05f, 1f, 0.35f);
        this.zoomInLerp = clamp(this.zoomInLerp, 0.01f, 1f, 0.15f);
        this.zoomOutLerp = clamp(this.zoomOutLerp, 0.01f, 1f, 0.25f);
        this.fullbrightLevel = Math.clamp(this.fullbrightLevel, 0, 15);
    }

    private static float clamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }
}
