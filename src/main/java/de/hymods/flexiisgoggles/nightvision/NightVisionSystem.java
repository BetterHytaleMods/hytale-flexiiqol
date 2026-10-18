package de.hymods.flexiisgoggles.nightvision;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToIntFunction;

/**
 * Night vision per player, through the light of air ({@link AirLight}). Every tick: does the player's
 * client have the level the player wants? A change goes out once it has held still for a moment,
 * because every change makes the client rebuild its world; dragging the brightness slider rebuilds it
 * once. Other players and the server's own light (spawning, plant growth) stay untouched.
 */
public final class NightVisionSystem extends EntityTickingSystem<EntityStore> {
    private static final long SETTLE_NANOS = 300_000_000L;

    private record Wish(int level, long since) {
    }

    private final ToIntFunction<UUID> wanted;
    /** The air light each player's client has, for those who have ours. */
    private final Map<UUID, Integer> sent = new ConcurrentHashMap<>();
    private final Map<UUID, Wish> wishes = new ConcurrentHashMap<>();

    /** @param wanted the level a player wants, 0 for the game's own light */
    public NightVisionSystem(ToIntFunction<UUID> wanted) {
        this.wanted = wanted;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef player = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (player == null) {
            return;
        }
        UUID id = player.getUuid();
        int want = this.wanted.applyAsInt(id);
        if (want == this.sent.getOrDefault(id, 0)) {
            this.wishes.remove(id);
            return;
        }
        long now = System.nanoTime();
        Wish wish = this.wishes.get(id);
        if (wish == null || wish.level() != want) {
            this.wishes.put(id, new Wish(want, now));
            return;
        }
        if (now - wish.since() < SETTLE_NANOS) {
            return;
        }
        player.getPacketHandler().writeNoCache(AirLight.packet(want));
        if (want == 0) {
            this.sent.remove(id);
        } else {
            this.sent.put(id, want);
        }
        this.wishes.remove(id);
    }

    /** Players online whose client has night vision right now. */
    public int active() {
        return this.sent.size();
    }

    /** Back to the game's light for everyone who has ours, before the plugin goes. */
    public void resetAll(@Nonnull Iterable<PlayerRef> players) {
        for (PlayerRef player : players) {
            if (this.sent.remove(player.getUuid()) != null) {
                player.getPacketHandler().writeNoCache(AirLight.packet(0));
            }
        }
    }

    public void forget(@Nonnull UUID player) {
        this.sent.remove(player);
        this.wishes.remove(player);
    }
}
