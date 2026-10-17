package de.hymods.flexiiqol;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import de.hymods.flexiiqol.command.QolCommand;
import de.hymods.flexiiqol.fullbright.FullbrightFilter;
import de.hymods.flexiiqol.spike.Probe;
import de.hymods.flexiiqol.spike.ProbeChainSystem;
import de.hymods.flexiiqol.zoom.Glide;
import de.hymods.flexiiqol.zoom.GlideSystem;
import de.hymods.flexiiqol.zoom.ZoomAssets;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class FlexiiQolPlugin extends JavaPlugin {
    private final AtomicReference<QolConfig> config = new AtomicReference<>(new QolConfig());
    private ZoomAssets zoomAssets;
    private Glide glide;
    private FullbrightFilter fullbright;
    private Probe probe;

    public FlexiiQolPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        try {
            this.reloadConfig();
        } catch (IOException | RuntimeException e) {
            this.getLogger().at(Level.WARNING).withCause(e).log("Cannot read %s, using the defaults", this.configFile());
        }
        this.zoomAssets = new ZoomAssets(this.getIdentifier().toString(), this.getDataDirectory().resolve("generated"));
        this.glide = new Glide();
        this.fullbright = new FullbrightFilter(this.getLogger());
        this.probe = new Probe(this.getLogger());
        this.getEntityStoreRegistry().registerSystem(new GlideSystem(this.glide));
        this.getEntityStoreRegistry().registerSystem(new ProbeChainSystem(this.probe));
        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            UUID player = event.getPlayerRef().getUuid();
            this.glide.forget(player);
            this.fullbright.clear(player);
            this.probe.watch(player, false);
        });
        this.getCommandRegistry().registerCommand(new QolCommand(this));
    }

    @Override
    protected void start() {
        this.fullbright.register();
        this.probe.register();
    }

    @Override
    protected void shutdown() {
        if (this.fullbright != null) {
            this.fullbright.close();
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

    public ZoomAssets zoomAssets() {
        return this.zoomAssets;
    }

    public Glide glide() {
        return this.glide;
    }

    public FullbrightFilter fullbright() {
        return this.fullbright;
    }

    public Probe probe() {
        return this.probe;
    }

    public Path configFile() {
        return this.getDataDirectory().resolve("config.json");
    }

    public void reloadConfig() throws IOException {
        this.config.set(QolConfig.load(this.configFile()));
    }
}
