package de.hymods.flexiisgoggles.ore;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/**
 * Saved with a chunk once it has its quartz: from the world generator (new chunks) or from
 * {@link QuartzBackfill} (chunks from before the mod). A chunk with it never gets ore again. Without the
 * mod the game keeps it as an unknown component.
 */
public final class OreMarker implements Component<ChunkStore> {
    static final int VERSION = 1;
    public static final BuilderCodec<OreMarker> CODEC = BuilderCodec.builder(OreMarker.class, OreMarker::new)
            .<Integer>append(new KeyedCodec<>("Version", Codec.INTEGER), (marker, version) -> marker.version = version, marker -> marker.version)
            .add()
            .build();

    private int version = VERSION;

    @Override
    public Component<ChunkStore> clone() {
        OreMarker copy = new OreMarker();
        copy.version = this.version;
        return copy;
    }
}
