package de.hymods.flexiiqol.zoom;

import com.hypixel.hytale.assetstore.AssetLoadResult;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The zoom root interactions, one per zoom step, written at runtime. They are loaded under the
 * plugin's asset pack, so the server sends them to every client right away and drops them when the
 * plugin unloads.
 */
public final class ZoomAssets {
    /** Every zoom root starts with this, which is how a binding is recognised as FlexiiQOL's. */
    public static final String PREFIX = "Root_FlexiiQOL_Zoom";

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

    /** The root interaction of the zoom step {@code magnification}. */
    public static String root(int magnification) {
        return PREFIX + "_" + magnification + "x";
    }

    /** Writes every step and loads them in one go; files of steps that are gone are deleted. */
    public void load(Collection<ZoomSpec> steps) throws IOException {
        Files.createDirectories(this.dir);
        Set<String> keys = new HashSet<>();
        List<Path> files = new ArrayList<>();
        for (ZoomSpec step : steps) {
            String key = root(step.magnification());
            Path file = this.dir.resolve(key + ".json");
            Path temp = this.dir.resolve(key + ".json.tmp");
            Files.writeString(temp, step.toRootJson(), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            keys.add(key);
            files.add(file);
        }
        AssetLoadResult<String, RootInteraction> result = RootInteraction.getAssetStore().loadAssetsFromPaths(this.packKey, files);
        if (result.hasFailed() || !result.getLoadedAssets().keySet().containsAll(keys)) {
            throw new IOException("zoom roots did not load (failed keys " + result.getFailedToLoadKeys() + ")");
        }
        try (DirectoryStream<Path> old = Files.newDirectoryStream(this.dir, PREFIX + "*.json")) {
            for (Path file : old) {
                if (!keys.contains(file.getFileName().toString().replaceFirst("\\.json$", ""))) {
                    Files.deleteIfExists(file);
                }
            }
        }
    }
}
