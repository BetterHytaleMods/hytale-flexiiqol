package de.hymods.flexiiqol.fullbright;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.CachedPacket;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.world.SetChunk;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import de.hymods.flexiiqol.light.LightTree;

import javax.annotation.Nonnull;
import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Brightens the chunk sections one player receives. The server shares one serialized
 * {@code SetChunk} per section between all players, so the filter decodes a copy, raises its light,
 * sends the copy and drops the original. The server's own light, which spawning and plant growth
 * read, stays untouched.
 */
public final class FullbrightFilter implements PlayerPacketFilter {
    private final HytaleLogger logger;
    /** Players with fullbright on, and their light level. */
    private final Map<UUID, Integer> levels = new ConcurrentHashMap<>();
    /** Set while this filter writes its own copy, which passes through the filters again. */
    private final ThreadLocal<Boolean> writing = ThreadLocal.withInitial(() -> false);
    private final AtomicBoolean warned = new AtomicBoolean();
    private final AtomicLong rewritten = new AtomicLong();
    private final AtomicLong nanos = new AtomicLong();
    private PacketFilter handle;

    public FullbrightFilter(HytaleLogger logger) {
        this.logger = logger;
    }

    public void register() {
        this.handle = PacketAdapters.registerOutbound((PlayerPacketFilter) this);
    }

    /** Must run on shutdown, or a plugin reload leaves the old filter in place. */
    public void close() {
        if (this.handle != null) {
            PacketAdapters.deregisterOutbound(this.handle);
            this.handle = null;
        }
    }

    public void set(@Nonnull UUID player, int level) {
        if (level < 0 || level > 15) {
            throw new IllegalArgumentException("light level must be 0 to 15");
        }
        this.levels.put(player, level);
    }

    public void clear(@Nonnull UUID player) {
        this.levels.remove(player);
    }

    public boolean isOn(@Nonnull UUID player) {
        return this.levels.containsKey(player);
    }

    public int players() {
        return this.levels.size();
    }

    /** Sections rewritten so far and the average time per section in microseconds. */
    public String stats() {
        long count = this.rewritten.get();
        return count + " section(s), " + (count == 0 ? 0 : this.nanos.get() / count / 1000) + " µs each";
    }

    @Override
    public boolean test(PlayerRef player, Packet packet) {
        if (this.writing.get()) {
            return false;
        }
        Integer level = this.levels.get(player.getUuid());
        if (level == null) {
            return false;
        }
        long started = System.nanoTime();
        SetChunk copy;
        if (packet instanceof CachedPacket<?> cached && cached.getPacketType() == SetChunk.class) {
            byte[] bytes = new byte[cached.computeSize()];
            cached.serialize(MemorySegment.ofArray(bytes), 0);
            copy = SetChunk.toObject(MemorySegment.ofArray(bytes));
        } else if (packet instanceof SetChunk plain) {
            copy = new SetChunk(plain);
        } else {
            return false;
        }
        try {
            copy.localLight = LightTree.brighten(copy.localLight, level);
            if (copy.globalLight != null) {
                copy.globalLight = LightTree.brighten(copy.globalLight, level);
            }
        } catch (RuntimeException e) {
            if (this.warned.compareAndSet(false, true)) {
                this.logger.at(Level.WARNING).withCause(e).log("Fullbright: unexpected light data, sending sections unchanged");
            }
            return false;
        }
        this.writing.set(true);
        try {
            player.getPacketHandler().writeNoCache(copy);
        } finally {
            this.writing.set(false);
        }
        this.rewritten.incrementAndGet();
        this.nanos.addAndGet(System.nanoTime() - started);
        return true;
    }
}
