package de.hymods.flexiiqol.fullbright;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who has fullbright on, kept in {@code fullbright.json} in the data folder so it survives a relog
 * and a restart. The packet filter asks for every chunk section, so lookups stay lock-free.
 */
public final class FullbrightPlayers {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();

    public FullbrightPlayers(Path file) {
        this.file = file;
    }

    /**
     * Reads the file. A missing file means nobody; a broken file or entry is skipped, and the next
     * save replaces it.
     *
     * @return whether the file was readable
     */
    public synchronized boolean load() {
        this.players.clear();
        if (!Files.exists(this.file)) {
            return true;
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(this.file, StandardCharsets.UTF_8));
            JsonElement list = root.isJsonObject() ? root.getAsJsonObject().get("players") : null;
            if (list == null || !list.isJsonArray()) {
                return false;
            }
            for (JsonElement entry : list.getAsJsonArray()) {
                try {
                    this.players.add(UUID.fromString(entry.getAsString()));
                } catch (IllegalArgumentException | UnsupportedOperationException | IllegalStateException e) {
                    // Skip it.
                }
            }
            return true;
        } catch (IOException | JsonParseException e) {
            return false;
        }
    }

    public boolean isOn(UUID player) {
        return this.players.contains(player);
    }

    /** @return whether fullbright is on now */
    public boolean toggle(UUID player) {
        if (this.players.remove(player)) {
            return false;
        }
        this.players.add(player);
        return true;
    }

    public void set(UUID player, boolean on) {
        if (on) {
            this.players.add(player);
        } else {
            this.players.remove(player);
        }
    }

    public int size() {
        return this.players.size();
    }

    /** Writes the current state; later calls write later states. */
    public synchronized void save() throws IOException {
        JsonArray list = new JsonArray();
        List<String> sorted = this.players.stream().map(UUID::toString).sorted().toList();
        sorted.forEach(list::add);
        JsonObject root = new JsonObject();
        root.add("players", list);
        Files.createDirectories(this.file.getParent());
        Path temp = this.file.resolveSibling(this.file.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(root) + "\n", StandardCharsets.UTF_8);
        Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
