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
import java.util.function.Function;

/**
 * Night vision per player, through block types only their client gets ({@link ClientBlocks}). Every
 * tick: does the player's client see what their goggles show? A change goes out once it has held still
 * for a moment, because every change makes the client rebuild its world; dragging the brightness slider
 * rebuilds it once. Other players and the server's own light (spawning, plant growth) stay untouched.
 */
public final class NightVisionSystem extends EntityTickingSystem<EntityStore> {
    private static final long SETTLE_NANOS = 300_000_000L;

    /**
     * What a player's client shows.
     *
     * @param level  the light level of air, 0 for the game's own light
     * @param reveal whether lumenite ore shows instead of looking like stone
     */
    public record Sight(int level, boolean reveal) {
        public static final Sight NORMAL = new Sight(0, false);
    }

    private record Wish(Sight sight, long since) {
    }

    private final Function<UUID, Sight> wanted;
    /** What each player's client shows, for those who don't see it the normal way. */
    private final Map<UUID, Sight> sent = new ConcurrentHashMap<>();
    private final Map<UUID, Wish> wishes = new ConcurrentHashMap<>();

    public NightVisionSystem(Function<UUID, Sight> wanted) {
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
        Sight want = this.wanted.apply(id);
        if (want.equals(this.sent.getOrDefault(id, Sight.NORMAL))) {
            this.wishes.remove(id);
            return;
        }
        long now = System.nanoTime();
        Wish wish = this.wishes.get(id);
        if (wish == null || !wish.sight().equals(want)) {
            this.wishes.put(id, new Wish(want, now));
            return;
        }
        if (now - wish.since() < SETTLE_NANOS) {
            return;
        }
        player.getPacketHandler().writeNoCache(ClientBlocks.packet(want.level(), want.reveal()));
        if (want.equals(Sight.NORMAL)) {
            this.sent.remove(id);
        } else {
            this.sent.put(id, want);
        }
        this.wishes.remove(id);
    }

    /** Players online whose client has night vision right now. */
    public int active() {
        return (int) this.sent.values().stream().filter(sight -> sight.level() > 0).count();
    }

    /** Back to the normal look for everyone who has ours, before the plugin goes. */
    public void resetAll(@Nonnull Iterable<PlayerRef> players) {
        for (PlayerRef player : players) {
            if (this.sent.remove(player.getUuid()) != null) {
                player.getPacketHandler().writeNoCache(ClientBlocks.packet(0, false));
            }
        }
    }

    public void forget(@Nonnull UUID player) {
        this.sent.remove(player);
        this.wishes.remove(player);
    }
}
