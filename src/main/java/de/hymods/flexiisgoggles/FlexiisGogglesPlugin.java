package de.hymods.flexiisgoggles;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import de.hymods.flexiisgoggles.command.GogglesCommand;
import de.hymods.flexiisgoggles.command.NightVisionCommand;
import de.hymods.flexiisgoggles.goggles.Effective;
import de.hymods.flexiisgoggles.goggles.WornGoggles;
import de.hymods.flexiisgoggles.nightvision.NightVisionSystem;
import de.hymods.flexiisgoggles.settings.PlayerSettings;
import de.hymods.flexiisgoggles.zoom.Glide;
import de.hymods.flexiisgoggles.zoom.GlideSystem;
import de.hymods.flexiisgoggles.zoom.UseEntityGuard;
import de.hymods.flexiisgoggles.zoom.ZoomAssets;
import de.hymods.flexiisgoggles.zoom.ZoomBindingSystem;
import de.hymods.flexiisgoggles.zoom.ZoomSpec;
import de.hymods.flexiisgoggles.zoom.ZoomSteps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.stream.IntStream;

public class FlexiisGogglesPlugin extends JavaPlugin {
    private final AtomicReference<GogglesConfig> config = new AtomicReference<>(new GogglesConfig());
    private final AtomicBoolean saveQueued = new AtomicBoolean();
    /** Whether the zoom roots have loaded. */
    private volatile boolean zoomLoaded;
    private ZoomAssets zoomAssets;
    private PlayerSettings players;
    private WornGoggles worn;
    private Glide glide;
    private NightVisionSystem nightVision;

    public FlexiisGogglesPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        try {
            this.config.set(GogglesConfig.load(this.configFile()));
        } catch (IOException | RuntimeException e) {
            this.getLogger().at(Level.WARNING).withCause(e).log("Cannot read %s, using the defaults", this.configFile());
        }
        this.zoomAssets = new ZoomAssets(this.getIdentifier().toString(), this.getDataDirectory().resolve("generated"));
        this.players = new PlayerSettings(this.getDataDirectory().resolve("players.json"));
        this.worn = new WornGoggles();
        this.glide = new Glide();
        this.nightVision = new NightVisionSystem(player -> this.effective(player).nightVisionLevel());
        this.getEntityStoreRegistry().registerSystem(this.worn);
        this.getEntityStoreRegistry().registerSystem(new ZoomBindingSystem(this::zoomRoot));
        this.getEntityStoreRegistry().registerSystem(new UseEntityGuard());
        this.getEntityStoreRegistry().registerSystem(new GlideSystem(this.glide, player -> Glide.lerp(this.effective(player).glideAmount())));
        this.getEntityStoreRegistry().registerSystem(this.nightVision);
        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            UUID player = event.getPlayerRef().getUuid();
            this.worn.forget(player);
            this.glide.forget(player);
            this.nightVision.forget(player);
        });
        this.getCommandRegistry().registerCommand(new GogglesCommand(this));
        this.getCommandRegistry().registerCommand(new NightVisionCommand(this));
    }

    @Override
    protected void start() {
        if (!this.players.load()) {
            this.getLogger().at(Level.WARNING).log("Cannot read players.json, everyone starts with the defaults");
        }
        try {
            this.loadZoom(this.config());
        } catch (IOException | RuntimeException e) {
            this.getLogger().at(Level.WARNING).withCause(e).log("Zoom roots did not load, R stays as it is");
        }
    }

    @Override
    protected void shutdown() {
        if (this.glide != null) {
            this.glide.resetAll(Universe.get().getPlayers());
        }
        if (this.nightVision != null) {
            this.nightVision.resetAll(Universe.get().getPlayers());
        }
        if (this.players != null && this.saveQueued.getAndSet(false)) {
            this.save();
        }
    }

    public GogglesConfig config() {
        return this.config.get();
    }

    public boolean zoomLoaded() {
        return this.zoomLoaded;
    }

    public PlayerSettings players() {
        return this.players;
    }

    public WornGoggles worn() {
        return this.worn;
    }

    public NightVisionSystem nightVision() {
        return this.nightVision;
    }

    public Path configFile() {
        return this.getDataDirectory().resolve("config.json");
    }

    /** What the player's goggles allow right now (everything, if the server doesn't require goggles). */
    public Effective.Ability ability(@Nonnull UUID player) {
        return Effective.Ability.of(this.config(), this.worn.worn(player));
    }

    /** What applies to a player now: their goggles, and their own picks within them. */
    public Effective effective(@Nonnull UUID player) {
        return Effective.of(this.config(), this.players.get(player), this.ability(player));
    }

    /** Changes a player's picks; they apply on the next tick and are saved a moment later. */
    public void choose(@Nonnull UUID player, @Nonnull UnaryOperator<PlayerSettings.Choice> change) {
        this.players.update(player, change);
        if (this.saveQueued.compareAndSet(false, true)) {
            // Sliders change a lot at once; one save a second later covers them all.
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                this.saveQueued.set(false);
                this.save();
            }, 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Reads {@code config.json} again and applies it. Limits and defaults change at once, the zoom
     * steps once the returned future completes.
     */
    public CompletableFuture<Void> reload() throws IOException {
        GogglesConfig fresh = GogglesConfig.load(this.configFile());
        this.config.set(fresh);
        return CompletableFuture.runAsync(() -> {
            try {
                this.loadZoom(fresh);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, HytaleServer.SCHEDULED_EXECUTOR);
    }

    /** Writes and loads every zoom step; the server sends them to everyone online right away. */
    private void loadZoom(GogglesConfig config) throws IOException {
        List<ZoomSpec> steps = IntStream.range(0, ZoomSteps.count()).mapToObj(i -> config.zoom.spec(ZoomSteps.value(i))).toList();
        this.zoomAssets.load(steps);
        this.zoomLoaded = true;
    }

    @Nullable
    private String zoomRoot(UUID player) {
        float zoom = this.effective(player).zoom();
        return this.zoomLoaded && zoom > 0f ? ZoomAssets.root(zoom) : null;
    }

    private void save() {
        try {
            this.players.save();
        } catch (IOException e) {
            this.getLogger().at(Level.WARNING).withCause(e).log("Cannot save players.json");
        }
    }
}
