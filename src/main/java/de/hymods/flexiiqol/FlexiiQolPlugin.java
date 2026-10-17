package de.hymods.flexiiqol;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import de.hymods.flexiiqol.command.FullbrightCommand;
import de.hymods.flexiiqol.command.QolCommand;
import de.hymods.flexiiqol.fullbright.Fullbright;
import de.hymods.flexiiqol.spike.Probe;
import de.hymods.flexiiqol.spike.ProbeChainSystem;
import de.hymods.flexiiqol.zoom.Glide;
import de.hymods.flexiiqol.zoom.GlideSystem;
import de.hymods.flexiiqol.zoom.UseEntityGuard;
import de.hymods.flexiiqol.zoom.ZoomAssets;
import de.hymods.flexiiqol.zoom.ZoomBindingSystem;
import de.hymods.flexiiqol.zoom.ZoomSpec;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class FlexiiQolPlugin extends JavaPlugin {
    private final AtomicReference<QolConfig> config = new AtomicReference<>(new QolConfig());
    /** The zoom R carries, once its root interaction has loaded. */
    private volatile ZoomSpec zoom;
    private ZoomAssets zoomAssets;
    private ZoomBindingSystem zoomBindings;
    private Glide glide;
    private Fullbright fullbright;
    private Probe probe;

    public FlexiiQolPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        try {
            this.config.set(QolConfig.load(this.configFile()));
        } catch (IOException | RuntimeException e) {
            this.getLogger().at(Level.WARNING).withCause(e).log("Cannot read %s, using the defaults", this.configFile());
        }
        this.zoomAssets = new ZoomAssets(this.getIdentifier().toString(), this.getDataDirectory().resolve("generated"));
        this.zoomBindings = new ZoomBindingSystem(() -> this.config().zoom.enabled && this.zoom != null);
        this.glide = new Glide();
        this.glide.set(Glide.Setting.of(this.config().zoom.glide, false));
        this.fullbright = new Fullbright(this.getLogger(), this.getDataDirectory().resolve("fullbright.json"));
        this.probe = new Probe(this.getLogger());
        this.getEntityStoreRegistry().registerSystem(this.zoomBindings);
        this.getEntityStoreRegistry().registerSystem(new UseEntityGuard());
        this.getEntityStoreRegistry().registerSystem(new GlideSystem(this.glide));
        this.getEntityStoreRegistry().registerSystem(new ProbeChainSystem(this.probe));
        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            UUID player = event.getPlayerRef().getUuid();
            this.zoomBindings.forget(player);
            this.glide.forget(player);
            this.fullbright.forget(player);
            this.probe.watch(player, false);
        });
        this.getCommandRegistry().registerCommand(new QolCommand(this));
        this.getCommandRegistry().registerCommand(new FullbrightCommand(this.fullbright));
    }

    @Override
    protected void start() {
        try {
            this.loadZoom(this.config().zoom.spec());
        } catch (IOException | RuntimeException e) {
            this.getLogger().at(Level.WARNING).withCause(e).log("Zoom root did not load, R stays as it is");
        }
        this.fullbright.start();
        this.fullbright.configure(this.config().fullbright.enabled, this.config().fullbright.level);
        this.probe.register();
    }

    @Override
    protected void shutdown() {
        if (this.fullbright != null) {
            this.fullbright.stop();
        }
        if (this.probe != null) {
            this.probe.close();
        }
        if (this.glide != null) {
            this.glide.resetAll(Universe.get().getPlayers());
        }
    }

    public QolConfig config() {
        return this.config.get();
    }

    /** The zoom R carries, or null if its root interaction did not load. */
    public ZoomSpec zoom() {
        return this.zoom;
    }

    public ZoomBindingSystem zoomBindings() {
        return this.zoomBindings;
    }

    public Glide glide() {
        return this.glide;
    }

    public Fullbright fullbright() {
        return this.fullbright;
    }

    public Probe probe() {
        return this.probe;
    }

    public Path configFile() {
        return this.getDataDirectory().resolve("config.json");
    }

    /** Writes and loads the zoom R carries; the server sends it to everyone online right away. */
    public void loadZoom(ZoomSpec spec) throws IOException {
        this.zoomAssets.load(ZoomAssets.ROOT, spec);
        this.zoom = spec;
    }

    /** {@link #loadZoom} on the scheduler, away from the world and command threads. */
    public CompletableFuture<ZoomSpec> loadZoomLater(ZoomSpec spec) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.loadZoom(spec);
                return spec;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, HytaleServer.SCHEDULED_EXECUTOR);
    }

    /**
     * Reads {@code config.json} again and applies it. Glide and fullbright change at once, the zoom
     * once the returned future completes.
     */
    public CompletableFuture<ZoomSpec> reload() throws IOException {
        QolConfig fresh = QolConfig.load(this.configFile());
        this.config.set(fresh);
        this.glide.set(Glide.Setting.of(fresh.zoom.glide, false));
        this.fullbright.configure(fresh.fullbright.enabled, fresh.fullbright.level);
        return this.loadZoomLater(fresh.zoom.spec());
    }
}
