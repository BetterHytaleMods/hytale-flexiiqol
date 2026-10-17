package de.hymods.flexiiqol.zoom;

import com.hypixel.hytale.assetstore.AssetLoadResult;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Zoom root interactions written at runtime. They are loaded under the plugin's asset pack, so the
 * server sends them to every client right away and drops them when the plugin unloads.
 */
public final class ZoomAssets {
    /** Every zoom root starts with this, which is how a binding is recognised as FlexiiQOL's. */
    public static final String PREFIX = "Root_FlexiiQOL_Zoom";
    /** The zoom R gets, written from the config. */
    public static final String ROOT = PREFIX;

    private final String packKey;
    private final Path dir;

    /**
     * @param packKey the plugin's asset pack, {@code Group:Name}
     * @param dir     where the generated files go, outside any pack
     */
    public ZoomAssets(String packKey, Path dir) {
        this.packKey = packKey;
        this.dir = dir;
    }

    /**
     * Writes {@code spec} as root interaction {@code key} and loads it.
     *
     * @return the root interaction's id
     */
    public String load(String key, ZoomSpec spec) throws IOException {
        if (!key.startsWith(PREFIX) || !key.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("zoom root ids start with " + PREFIX + ": " + key);
        }
        Files.createDirectories(this.dir);
        Path file = this.dir.resolve(key + ".json");
        Path temp = this.dir.resolve(key + ".json.tmp");
        Files.writeString(temp, spec.toRootJson(), StandardCharsets.UTF_8);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        AssetLoadResult<String, RootInteraction> result = RootInteraction.getAssetStore().loadAssetsFromPaths(this.packKey, List.of(file));
        if (result.hasFailed() || !result.getLoadedAssets().containsKey(key)) {
            throw new IOException("root interaction " + key + " did not load (failed keys " + result.getFailedToLoadKeys() + ")");
        }
        return key;
    }
}
