package de.hymods.flexiisgoggles;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import de.hymods.flexiisgoggles.command.FullbrightCommand;
import de.hymods.flexiisgoggles.command.GogglesCommand;
import de.hymods.flexiisgoggles.fullbright.FullbrightSystem;
import de.hymods.flexiisgoggles.settings.PlayerSettings;
import de.hymods.flexiisgoggles.zoom.Glide;
import de.hymods.flexiisgoggles.zoom.GlideSystem;
import de.hymods.flexiisgoggles.zoom.UseEntityGuard;
import de.hymods.flexiisgoggles.zoom.ZoomAssets;
import de.hymods.flexiisgoggles.zoom.ZoomBindingSystem;
import de.hymods.flexiisgoggles.zoom.ZoomSpec;

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
    private Glide glide;
    private FullbrightSystem fullbright;

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
        this.glide = new Glide();
        this.fullbright = new FullbrightSystem(this::brightness);
        this.getEntityStoreRegistry().registerSystem(new ZoomBindingSystem(this::zoomRoot));
        this.getEntityStoreRegistry().registerSystem(new UseEntityGuard());
        this.getEntityStoreRegistry().registerSystem(new GlideSystem(this.glide, this::glideLerp));
        this.getEntityStoreRegistry().registerSystem(this.fullbright);
        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            UUID player = event.getPlayerRef().getUuid();
            this.glide.forget(player);
            this.fullbright.forget(player);
        });
        this.getCommandRegistry().registerCommand(new GogglesCommand(this));
        this.getCommandRegistry().registerCommand(new FullbrightCommand(this));
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
        if (this.fullbright != null) {
            this.fullbright.resetAll(Universe.get().getPlayers());
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

    public FullbrightSystem fullbright() {
        return this.fullbright;
    }

    public Path configFile() {
        return this.getDataDirectory().resolve("config.json");
    }

    /** What applies to a player now: their own picks, the config's defaults for the rest. */
    public PlayerSettings.Values values(@Nonnull UUID player) {
        return PlayerSettings.Values.of(this.config(), this.players.get(player));
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
     * Reads {@code config.json} again and applies it. Defaults, glide and fullbright change at once,
     * the zoom steps once the returned future completes.
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
        List<ZoomSpec> steps = IntStream.rangeClosed(PlayerSettings.ZOOM_MIN, PlayerSettings.ZOOM_MAX).mapToObj(config.zoom::spec).toList();
        this.zoomAssets.load(steps);
        this.zoomLoaded = true;
    }

    @Nullable
    private String zoomRoot(UUID player) {
        return this.config().zoom.enabled && this.zoomLoaded ? ZoomAssets.root(this.values(player).zoom()) : null;
    }

    @Nullable
    private Float glideLerp(UUID player) {
        return Glide.lerp(this.values(player).glideAmount());
    }

    private int brightness(UUID player) {
        PlayerSettings.Values values = this.values(player);
        return this.config().fullbright.enabled && values.fullbright() ? values.brightness() : 0;
    }

    private void save() {
        try {
            this.players.save();
        } catch (IOException e) {
            this.getLogger().at(Level.WARNING).withCause(e).log("Cannot save players.json");
        }
    }
}
