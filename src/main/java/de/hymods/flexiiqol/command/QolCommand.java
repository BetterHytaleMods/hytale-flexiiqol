package de.hymods.flexiiqol.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.hymods.flexiiqol.FlexiiQolPlugin;
import de.hymods.flexiiqol.QolConfig;
import de.hymods.flexiiqol.settings.PlayerSettings;
import de.hymods.flexiiqol.ui.SettingsPage;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * {@code /qol} opens the settings page, for every player. {@code /qol status} and {@code /qol reload}
 * keep the permission every command needs by default, so they are for operators and the console.
 */
public final class QolCommand extends AbstractPlayerCommand {
    private final FlexiiQolPlugin plugin;

    public QolCommand(FlexiiQolPlugin plugin) {
        super("qol", "flexiiqol.commands.qol.desc");
        this.plugin = plugin;
        this.requireNoPermission();
        this.addSubCommand(new Status(plugin));
        this.addSubCommand(new Reload(plugin));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().openCustomPage(ref, store, new SettingsPage(playerRef, this.plugin));
        }
    }

    /** One line each for the zoom and fullbright. */
    public static String[] describe(FlexiiQolPlugin plugin) {
        QolConfig config = plugin.config();
        return new String[]{
                "FlexiiQOL zoom %s: %s, default %dx (%s), glide default %s".formatted(
                        config.zoom.enabled ? "on" : "off",
                        plugin.zoomLoaded() ? "steps %d-%dx loaded".formatted(PlayerSettings.ZOOM_MIN, PlayerSettings.ZOOM_MAX) : "roots not loaded",
                        config.zoom.magnification, config.zoom.spec(config.zoom.magnification).describe(), config.zoom.glide),
                "FlexiiQOL fullbright %s: default brightness %d, on for %d player(s) online; %d player(s) with own settings".formatted(
                        config.fullbright.enabled ? "on" : "off", config.fullbright.level, plugin.fullbright().active(),
                        plugin.players().size())
        };
    }

    private static final class Status extends CommandBase {
        private final FlexiiQolPlugin plugin;

        Status(FlexiiQolPlugin plugin) {
            super("status", "flexiiqol.commands.qol.status.desc");
            this.plugin = plugin;
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            for (String line : describe(this.plugin)) {
                context.sendMessage(Message.raw(line));
            }
            context.sendMessage(Message.raw("config: " + this.plugin.configFile()));
        }
    }

    private static final class Reload extends CommandBase {
        private final FlexiiQolPlugin plugin;

        Reload(FlexiiQolPlugin plugin) {
            super("reload", "flexiiqol.commands.qol.reload.desc");
            this.plugin = plugin;
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            try {
                this.plugin.reload().whenComplete((ignored, error) -> {
                    if (error != null) {
                        context.sendMessage(Message.raw("FlexiiQOL: config reloaded, but the zoom did not load: " + error.getMessage()));
                        return;
                    }
                    context.sendMessage(Message.raw("FlexiiQOL: config reloaded"));
                    for (String line : describe(this.plugin)) {
                        context.sendMessage(Message.raw(line));
                    }
                });
            } catch (IOException | RuntimeException e) {
                context.sendMessage(Message.raw("FlexiiQOL: config not reloaded, keeping the old one: " + e.getMessage()));
            }
        }
    }
}
