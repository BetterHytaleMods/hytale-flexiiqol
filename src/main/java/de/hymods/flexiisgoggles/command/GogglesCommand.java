package de.hymods.flexiisgoggles.command;

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
import de.hymods.flexiisgoggles.FlexiisGogglesPlugin;
import de.hymods.flexiisgoggles.GogglesConfig;
import de.hymods.flexiisgoggles.settings.PlayerSettings;
import de.hymods.flexiisgoggles.ui.SettingsPage;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * {@code /goggles} opens the settings page, for every player. {@code /goggles status} and {@code /goggles reload}
 * keep the permission every command needs by default, so they are for operators and the console.
 */
public final class GogglesCommand extends AbstractPlayerCommand {
    private final FlexiisGogglesPlugin plugin;

    public GogglesCommand(FlexiisGogglesPlugin plugin) {
        super("goggles", "flexiisgoggles.commands.goggles.desc");
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
    public static String[] describe(FlexiisGogglesPlugin plugin) {
        GogglesConfig config = plugin.config();
        return new String[]{
                "FlexiisGoggles zoom %s: %s, default %dx (%s), glide default %s".formatted(
                        config.zoom.enabled ? "on" : "off",
                        plugin.zoomLoaded() ? "steps %d-%dx loaded".formatted(PlayerSettings.ZOOM_MIN, PlayerSettings.ZOOM_MAX) : "roots not loaded",
                        config.zoom.magnification, config.zoom.spec(config.zoom.magnification).describe(), config.zoom.glide),
                "FlexiisGoggles fullbright %s: default brightness %d, on for %d player(s) online; %d player(s) with own settings".formatted(
                        config.fullbright.enabled ? "on" : "off", config.fullbright.level, plugin.fullbright().active(),
                        plugin.players().size())
        };
    }

    private static final class Status extends CommandBase {
        private final FlexiisGogglesPlugin plugin;

        Status(FlexiisGogglesPlugin plugin) {
            super("status", "flexiisgoggles.commands.goggles.status.desc");
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
        private final FlexiisGogglesPlugin plugin;

        Reload(FlexiisGogglesPlugin plugin) {
            super("reload", "flexiisgoggles.commands.goggles.reload.desc");
            this.plugin = plugin;
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            try {
                this.plugin.reload().whenComplete((ignored, error) -> {
                    if (error != null) {
                        context.sendMessage(Message.raw("FlexiisGoggles: config reloaded, but the zoom did not load: " + error.getMessage()));
                        return;
                    }
                    context.sendMessage(Message.raw("FlexiisGoggles: config reloaded"));
                    for (String line : describe(this.plugin)) {
                        context.sendMessage(Message.raw(line));
                    }
                });
            } catch (IOException | RuntimeException e) {
                context.sendMessage(Message.raw("FlexiisGoggles: config not reloaded, keeping the old one: " + e.getMessage()));
            }
        }
    }
}
