package de.hymods.flexiisgoggles.ore;

import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.worldgen.HytaleWorldGenProvider;

import javax.annotation.Nonnull;

/**
 * Quartz for worlds that existed before the mod: the world generator only puts ore into chunks it makes
 * from now on. When a chunk of the game's own generator loads for the first time with the mod, it gets
 * {@link QuartzVeins} in its stone and an {@link OreMarker}; a chunk the generator makes new only gets the
 * marker, as the generator already gave it quartz.
 */
public final class QuartzBackfill {
    static final String ORE = "Flexii_Ore_Quartz_Stone";
    private static final String STONE = "Rock_Stone";

    private final ComponentType<ChunkStore, OreMarker> marker;

    public QuartzBackfill(ComponentType<ChunkStore, OreMarker> marker) {
        this.marker = marker;
    }

    public void onChunk(@Nonnull ChunkPreLoadProcessEvent event) {
        Holder<ChunkStore> holder = event.getHolder();
        World world = event.getWorld();
        if (holder.getComponent(this.marker) != null || !(world.getWorldConfig().getWorldGenProvider() instanceof HytaleWorldGenProvider)) {
            return;
        }
        if (!event.isNewlyGenerated()) {
            WorldChunk chunk = event.getChunk();
            fill(holder, world.getWorldConfig().getSeed(), chunk.getX(), chunk.getZ());
            chunk.markNeedsSaving();
        }
        holder.addComponent(this.marker, new OreMarker());
    }

    /** Turns the stone at the chunk's vein blocks into quartz ore; how many blocks that was. */
    static int fill(Holder<ChunkStore> holder, long seed, int chunkX, int chunkZ) {
        BlockTypeAssetMap<String, BlockType> blocks = BlockType.getAssetMap();
        int ore = blocks.getIndex(ORE);
        int stone = blocks.getIndex(STONE);
        ChunkColumn column = holder.getComponent(ChunkColumn.getComponentType());
        Holder<ChunkStore>[] sections = column == null ? null : column.getSectionHolders();
        if (ore == Integer.MIN_VALUE || stone == Integer.MIN_VALUE || sections == null) {
            return 0;
        }
        int placed = 0;
        for (int[] at : QuartzVeins.blocks(seed, chunkX, chunkZ)) {
            int s = at[1] >> 5;
            BlockSection section = s < sections.length && sections[s] != null ? sections[s].getComponent(BlockSection.getComponentType()) : null;
            if (section != null && section.get(at[0], at[1], at[2]) == stone) {
                section.set(at[0], at[1], at[2], ore, 0, 0);
                placed++;
            }
        }
        return placed;
    }
}
