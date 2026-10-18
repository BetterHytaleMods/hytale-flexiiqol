package de.hymods.flexiisgoggles.nightvision;

import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.protocol.UpdateType;
import com.hypixel.hytale.protocol.packets.assets.UpdateBlockTypes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

import java.util.Map;

/**
 * Air as one player's client should know it. Hytale's client works out the light itself, from the
 * light each block type gives off, and ignores the light in the chunk data. With a light level of its
 * own, every open space is lit at least that much, while torches, lava and the sun still light up
 * more. The radius stays 0: block lights with a radius also get a point light each, and with every
 * air block glowing those would add up to a glare.
 */
final class AirLight {
    private AirLight() {
    }

    /**
     * Air with {@code level}, or as the game defines it for 0. {@code updateMapGeometry} makes the
     * client rebuild the world it has loaded, as it does when block types change live.
     */
    static UpdateBlockTypes packet(int level) {
        BlockTypeAssetMap<String, BlockType> assets = BlockType.getAssetMap();
        // The server caches and shares each type's packet object, so change a copy only.
        com.hypixel.hytale.protocol.BlockType air = new com.hypixel.hytale.protocol.BlockType(assets.getAsset(BlockType.EMPTY_ID).toPacket());
        if (level > 0) {
            air.light = new ColorLight((byte) 0, (byte) level, (byte) level, (byte) level);
        }
        UpdateBlockTypes packet = new UpdateBlockTypes();
        packet.type = UpdateType.AddOrUpdate;
        packet.blockTypes = Map.of(BlockType.EMPTY_ID, air);
        packet.maxId = assets.getNextIndex();
        packet.updateMapGeometry = true;
        return packet;
    }
}
