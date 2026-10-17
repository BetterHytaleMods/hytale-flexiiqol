package de.hymods.flexiiqol.fullbright;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.entity.player.ChunkTracker;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.lighting.ChunkLightingManager;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * After a block changes, the client lights the area around it again on its own, and a brightened
 * section can turn dark. Once the edits in a section have settled and the server has relit it, the
 * section goes out again, through the filter.
 */
public final class EditResend {
    /** Edits closer together than this count as one. */
    private static final long QUIET_NANOS = TimeUnit.MILLISECONDS.toNanos(250);
    /** Send anyway if the server is still relighting by then. */
    private static final long GIVE_UP_NANOS = TimeUnit.SECONDS.toNanos(3);

    private record Section(int x, int y, int z) {
    }

    private record Pending(long first, long last) {
    }

    private final HytaleLogger logger;
    private final Map<UUID, Map<Section, Pending>> pending = new ConcurrentHashMap<>();
    private final AtomicBoolean warned = new AtomicBoolean();
    private final AtomicLong resent = new AtomicLong();
    private ScheduledFuture<?> task;

    public EditResend(HytaleLogger logger) {
        this.logger = logger;
    }

    public void start() {
        this.task = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::flush, 100, 100, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel(false);
            this.task = null;
        }
        this.pending.clear();
    }

    /** A block in section {@code x y z} changed for {@code player}. Runs on whichever thread sends packets. */
    void mark(@Nonnull PlayerRef player, int x, int y, int z) {
        long now = System.nanoTime();
        this.pending.computeIfAbsent(player.getUuid(), id -> new ConcurrentHashMap<>())
                .compute(new Section(x, y, z), (section, old) -> new Pending(old == null ? now : old.first(), now));
    }

    public void forget(@Nonnull UUID player) {
        this.pending.remove(player);
    }

    /** Sections sent again after edits so far. */
    public long resent() {
        return this.resent.get();
    }

    private void flush() {
        try {
            long now = System.nanoTime();
            for (Map.Entry<UUID, Map<Section, Pending>> entry : this.pending.entrySet()) {
                PlayerRef player = Universe.get().getPlayer(entry.getKey());
                if (player == null) {
                    this.pending.remove(entry.getKey());
                    continue;
                }
                World world = Universe.get().getWorld(player.getWorldUuid());
                ChunkTracker tracker = player.getChunkTracker();
                if (world == null || tracker == null) {
                    continue;
                }
                ChunkLightingManager lighting = world.getChunkLighting();
                Map<Section, Pending> sections = entry.getValue();
                for (Map.Entry<Section, Pending> section : sections.entrySet()) {
                    Section s = section.getKey();
                    Pending p = section.getValue();
                    if (now - p.last() < QUIET_NANOS) {
                        continue;
                    }
                    if (now - p.first() < GIVE_UP_NANOS && lighting != null && lighting.isQueued(new Vector3i(s.x(), s.y(), s.z()))) {
                        continue;
                    }
                    // Only if no edit came in meanwhile; a newer one waits for its own quiet time.
                    if (sections.remove(s, p)) {
                        tracker.removeForReload(s.x(), s.y(), s.z());
                        this.resent.incrementAndGet();
                    }
                }
            }
        } catch (RuntimeException e) {
            // An exception would cancel the schedule for good.
            if (this.warned.compareAndSet(false, true)) {
                this.logger.at(Level.WARNING).withCause(e).log("Fullbright: cannot send edited sections again");
            }
        }
    }
}
