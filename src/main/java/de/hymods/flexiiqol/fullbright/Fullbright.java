package de.hymods.flexiiqol.fullbright;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/** Fullbright per player: who has it on, the packet filter, and sending sections again. */
public final class Fullbright {
    private final HytaleLogger logger;
    private final FullbrightPlayers players;
    private final EditResend edits;
    private final FullbrightFilter filter;

    public Fullbright(HytaleLogger logger, Path file) {
        this.logger = logger;
        this.players = new FullbrightPlayers(file);
        this.edits = new EditResend(logger);
        this.filter = new FullbrightFilter(logger, this.players, this.edits);
    }

    public void start() {
        if (!this.players.load()) {
            this.logger.at(Level.WARNING).log("Fullbright: cannot read the player list, starting with nobody");
        }
        this.filter.register();
        this.edits.start();
    }

    public void stop() {
        this.filter.close();
        this.edits.stop();
    }

    /** Applies the config; players with fullbright on get their sections again if the light changed. */
    public void configure(boolean enabled, int level) {
        int before = this.filter.level();
        this.filter.configure(enabled, level);
        if (this.filter.level() != before) {
            for (PlayerRef player : Universe.get().getPlayers()) {
                if (this.players.isOn(player.getUuid())) {
                    ChunkResend.all(player);
                }
            }
        }
    }

    /** Whether the server lets players switch fullbright on. */
    public boolean isAvailable() {
        return this.filter.level() > 0;
    }

    public boolean isOn(@Nonnull UUID player) {
        return this.players.isOn(player);
    }

    /** @return whether fullbright is on now */
    public boolean toggle(@Nonnull PlayerRef player) {
        boolean on = this.players.toggle(player.getUuid());
        this.changed(player);
        return on;
    }

    public void set(@Nonnull PlayerRef player, boolean on) {
        this.players.set(player.getUuid(), on);
        this.changed(player);
    }

    /** The player left; whether fullbright is on stays saved. */
    public void forget(@Nonnull UUID player) {
        this.edits.forget(player);
    }

    public int players() {
        return this.players.size();
    }

    public String stats() {
        return this.filter.stats();
    }

    private void changed(PlayerRef player) {
        // The scheduler runs one task at a time, so the saves land in order.
        CompletableFuture.runAsync(() -> {
            try {
                this.players.save();
            } catch (IOException e) {
                this.logger.at(Level.WARNING).withCause(e).log("Fullbright: cannot save the player list");
            }
        }, HytaleServer.SCHEDULED_EXECUTOR);
        if (this.isAvailable()) {
            ChunkResend.all(player);
        }
    }
}
