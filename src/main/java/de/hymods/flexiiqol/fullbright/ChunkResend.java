package de.hymods.flexiiqol.fullbright;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.entity.player.ChunkTracker;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Sends a player's loaded chunk sections again, so a light change shows at once. The tracker sends
 * reloads ahead of new sections, at its usual rate.
 */
public final class ChunkResend {
    private ChunkResend() {
    }

    /**
     * Queues every loaded section of {@code player} for sending again.
     *
     * @return how many sections were queued
     */
    public static CompletableFuture<Integer> all(@Nonnull PlayerRef player) {
        return CompletableFuture.supplyAsync(() -> {
            ChunkTracker tracker = player.getChunkTracker();
            if (tracker == null) {
                return 0;
            }
            // The tracker holds its lock during the callback and the lock is not reentrant:
            // collect first, queue afterwards.
            List<int[]> sections = new ArrayList<>();
            tracker.forEachLoadedSection((x, y, z) -> sections.add(new int[]{x, y, z}));
            for (int[] section : sections) {
                tracker.removeForReload(section[0], section[1], section[2]);
            }
            return sections.size();
        }, HytaleServer.SCHEDULED_EXECUTOR);
    }
}
