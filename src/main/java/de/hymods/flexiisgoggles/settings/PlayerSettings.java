package de.hymods.flexiisgoggles.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import de.hymods.flexiisgoggles.zoom.ZoomSteps;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * What each player picked on {@code /goggles}, kept in {@code players.json} in the data folder, so it
 * survives a relog and a restart. A setting the player never touched stays empty and follows their
 * goggles or the server's default.
 */
public final class PlayerSettings {
    /** Glide in tenths: 0 is off, 18 trails the most. */
    public static final int GLIDE_MAX = 18;
    public static final int BRIGHTNESS_MIN = 1;
    public static final int BRIGHTNESS_MAX = 6;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** One player's own picks; null follows the default. Values out of range are clamped. */
    public record Choice(@Nullable Float zoom, @Nullable Integer glide, @Nullable Boolean nightVision, @Nullable Integer brightness) {
        public static final Choice NONE = new Choice(null, null, null, null);

        public Choice {
            zoom = zoom == null || !Float.isFinite(zoom) ? null : ZoomSteps.snap(zoom);
            glide = glide == null ? null : Math.clamp(glide, 0, GLIDE_MAX);
            brightness = brightness == null ? null : Math.clamp(brightness, BRIGHTNESS_MIN, BRIGHTNESS_MAX);
        }

        public Choice withZoom(float zoom) {
            return new Choice(zoom, this.glide, this.nightVision, this.brightness);
        }

        public Choice withGlide(int glide) {
            return new Choice(this.zoom, glide, this.nightVision, this.brightness);
        }

        public Choice withNightVision(boolean on) {
            return new Choice(this.zoom, this.glide, on, this.brightness);
        }

        public Choice withBrightness(int brightness) {
            return new Choice(this.zoom, this.glide, this.nightVision, brightness);
        }

        public boolean isEmpty() {
            return this.zoom == null && this.glide == null && this.nightVision == null && this.brightness == null;
        }
    }

    private final Path file;
    private final Map<UUID, Choice> choices = new ConcurrentHashMap<>();

    public PlayerSettings(Path file) {
        this.file = file;
    }

    /**
     * Reads the file. A missing file means nobody picked anything yet; a broken entry or value is
     * skipped, and the next save replaces it.
     *
     * @return whether the file was readable
     */
    public synchronized boolean load() {
        this.choices.clear();
        if (!Files.exists(this.file)) {
            return true;
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(this.file, StandardCharsets.UTF_8));
            JsonElement players = root.isJsonObject() ? root.getAsJsonObject().get("players") : null;
            if (players == null || !players.isJsonObject()) {
                return false;
            }
            for (Map.Entry<String, JsonElement> entry : players.getAsJsonObject().entrySet()) {
                UUID player;
                try {
                    player = UUID.fromString(entry.getKey());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject fields = entry.getValue().getAsJsonObject();
                Double zoom = number(fields, "zoom");
                Double glide = number(fields, "glide");
                Double brightness = number(fields, "brightness");
                Choice choice = new Choice(zoom == null ? null : zoom.floatValue(), glide == null ? null : (int) Math.round(glide),
                        flag(fields, "nightVision"), brightness == null ? null : (int) Math.round(brightness));
                if (!choice.isEmpty()) {
                    this.choices.put(player, choice);
                }
            }
            return true;
        } catch (IOException | JsonParseException | IllegalStateException e) {
            return false;
        }
    }

    public Choice get(UUID player) {
        return this.choices.getOrDefault(player, Choice.NONE);
    }

    /** @return the player's picks after {@code change} */
    public Choice update(UUID player, UnaryOperator<Choice> change) {
        Choice updated = this.choices.compute(player, (id, old) -> {
            Choice next = change.apply(old == null ? Choice.NONE : old);
            return next.isEmpty() ? null : next;
        });
        return updated == null ? Choice.NONE : updated;
    }

    /** Players who picked anything. */
    public int size() {
        return this.choices.size();
    }

    /** Writes the current state; later calls write later states. */
    public synchronized void save() throws IOException {
        JsonObject players = new JsonObject();
        Map<String, Choice> sorted = new TreeMap<>();
        this.choices.forEach((player, choice) -> sorted.put(player.toString(), choice));
        sorted.forEach((player, choice) -> {
            JsonObject fields = new JsonObject();
            if (choice.zoom() != null) {
                fields.addProperty("zoom", choice.zoom());
            }
            if (choice.glide() != null) {
                fields.addProperty("glide", choice.glide());
            }
            if (choice.nightVision() != null) {
                fields.addProperty("nightVision", choice.nightVision());
            }
            if (choice.brightness() != null) {
                fields.addProperty("brightness", choice.brightness());
            }
            players.add(player, fields);
        });
        JsonObject root = new JsonObject();
        root.add("players", players);
        Files.createDirectories(this.file.getParent());
        Path temp = this.file.resolveSibling(this.file.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(root) + "\n", StandardCharsets.UTF_8);
        Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    @Nullable
    private static Double number(JsonObject fields, String name) {
        JsonElement value = fields.get(name);
        if (value instanceof JsonPrimitive primitive && primitive.isNumber()) {
            double number = primitive.getAsDouble();
            return Double.isFinite(number) ? number : null;
        }
        return null;
    }

    @Nullable
    private static Boolean flag(JsonObject fields, String name) {
        JsonElement value = fields.get(name);
        return value instanceof JsonPrimitive primitive && primitive.isBoolean() ? primitive.getAsBoolean() : null;
    }
}
