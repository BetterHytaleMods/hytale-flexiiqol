package de.hymods.flexiisgoggles.nightvision;

import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.protocol.UpdateType;
import com.hypixel.hytale.protocol.packets.assets.UpdateBlockTypes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Block types as one player's client should know them, for what their goggles show.
 *
 * <p>Night vision: Hytale's client works out the light itself, from the light each block type gives
 * off, and ignores the light in the chunk data. With a light level of its own, every open space is lit
 * at least that much, while torches, lava and the sun still light up more. The radius stays 0: block
 * lights with a radius also get a point light each, and with every air block glowing those would add
 * up to a glare.
 *
 * <p>Lumenite: the ore is stone to everyone. Strong night vision swaps in the look of
 * {@code Flexii_Ore_Lumenite_Revealed} (glowing crystals), which is never placed in the world, so its
 * model and texture are known to every client.
 */
final class ClientBlocks {
    static final String LUMENITE = "Flexii_Ore_Lumenite_Stone";
    static final String LUMENITE_REVEALED = "Flexii_Ore_Lumenite_Revealed";

    private ClientBlocks() {
    }

    /**
     * Air with {@code level} (or as the game defines it for 0), and lumenite revealed or not.
     * {@code updateMapGeometry} makes the client rebuild the world it has loaded, as it does when block
     * types change live.
     */
    static UpdateBlockTypes packet(int level, boolean revealLumenite) {
        BlockTypeAssetMap<String, BlockType> assets = BlockType.getAssetMap();
        Map<Integer, com.hypixel.hytale.protocol.BlockType> types = new HashMap<>();
        // The server caches and shares each type's packet object, so change copies only.
        com.hypixel.hytale.protocol.BlockType air = copy(assets.getAsset(BlockType.EMPTY_ID));
        if (level > 0) {
            air.light = new ColorLight((byte) 0, (byte) level, (byte) level, (byte) level);
        }
        types.put(BlockType.EMPTY_ID, air);
        int lumenite = assets.getIndex(LUMENITE);
        int revealed = assets.getIndex(LUMENITE_REVEALED);
        if (lumenite != Integer.MIN_VALUE && revealed != Integer.MIN_VALUE) {
            com.hypixel.hytale.protocol.BlockType ore = copy(assets.getAsset(lumenite));
            if (revealLumenite) {
                com.hypixel.hytale.protocol.BlockType look = assets.getAsset(revealed).toPacket();
                ore.drawType = look.drawType;
                ore.model = look.model;
                ore.modelTexture = look.modelTexture;
                ore.modelScale = look.modelScale;
                ore.cubeTextures = look.cubeTextures;
                ore.cubeShadingMode = look.cubeShadingMode;
                ore.light = look.light;
                ore.particleColor = look.particleColor;
            }
            types.put(lumenite, ore);
        }
        UpdateBlockTypes packet = new UpdateBlockTypes();
        packet.type = UpdateType.AddOrUpdate;
        packet.blockTypes = types;
        packet.maxId = assets.getNextIndex();
        packet.updateMapGeometry = true;
        return packet;
    }

    private static com.hypixel.hytale.protocol.BlockType copy(@Nullable BlockType asset) {
        return new com.hypixel.hytale.protocol.BlockType(asset.toPacket());
    }
}
