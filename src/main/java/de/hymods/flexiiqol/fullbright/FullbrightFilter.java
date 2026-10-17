package de.hymods.flexiiqol.fullbright;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.CachedPacket;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.world.ServerSetBlock;
import com.hypixel.hytale.protocol.packets.world.ServerSetBlocks;
import com.hypixel.hytale.protocol.packets.world.SetChunk;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import de.hymods.flexiiqol.light.LightTree;

import javax.annotation.Nonnull;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Brightens the chunk sections a player with fullbright receives. The server shares one serialized
 * {@code SetChunk} per section between all players, so the filter decodes a copy, raises its light,
 * sends the copy and drops the original. The server's own light, which spawning and plant growth
 * read, stays untouched. Block changes are noted for {@link EditResend}.
 */
public final class FullbrightFilter implements PlayerPacketFilter {
    private final HytaleLogger logger;
    private final FullbrightPlayers players;
    private final EditResend edits;
    /** Light level for players with fullbright on; 0 while fullbright is off on the server. */
    private volatile int level;
    /** Set while this filter writes its own copy, which passes through the filters again. */
    private final ThreadLocal<Boolean> writing = ThreadLocal.withInitial(() -> false);
    private final AtomicBoolean warned = new AtomicBoolean();
    private final AtomicLong rewritten = new AtomicLong();
    private final AtomicLong nanos = new AtomicLong();
    private PacketFilter handle;

    public FullbrightFilter(HytaleLogger logger, FullbrightPlayers players, EditResend edits) {
        this.logger = logger;
        this.players = players;
        this.edits = edits;
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

    /** @param level 0 to 15; 0 sends the server's light unchanged */
    public void configure(boolean enabled, int level) {
        if (level < 0 || level > 15) {
            throw new IllegalArgumentException("light level must be 0 to 15");
        }
        this.level = enabled ? level : 0;
    }

    public int level() {
        return this.level;
    }

    /** Sections rewritten so far and the average time per section in microseconds. */
    public String stats() {
        long count = this.rewritten.get();
        return "%d section(s) rewritten, %d µs each, %d sent again after edits".formatted(
                count, count == 0 ? 0 : this.nanos.get() / count / 1000, this.edits.resent());
    }

    @Override
    public boolean test(PlayerRef player, Packet packet) {
        if (this.writing.get()) {
            return false;
        }
        int level = this.level;
        if (level == 0 || !this.players.isOn(player.getUuid())) {
            return false;
        }
        switch (packet) {
            case ServerSetBlock block -> {
                this.edits.mark(player, ChunkUtil.chunkCoordinate(block.x), ChunkUtil.chunkCoordinate(block.y), ChunkUtil.chunkCoordinate(block.z));
                return false;
            }
            case ServerSetBlocks blocks -> {
                this.edits.mark(player, blocks.x, blocks.y, blocks.z);
                return false;
            }
            default -> {
            }
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
