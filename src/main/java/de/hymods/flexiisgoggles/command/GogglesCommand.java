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
import de.hymods.flexiisgoggles.ui.SettingsPage;
import de.hymods.flexiisgoggles.zoom.ZoomSteps;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * {@code /goggles} opens the settings page, for every player wearing goggles. {@code /goggles status}
 * and {@code /goggles reload} keep the permission every command needs by default, so they are for
 * operators and the console.
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
        if (!this.plugin.ability(playerRef.getUuid()).any()) {
            context.sendMessage(Message.translation("flexiisgoggles.goggles.none").color(NightVisionCommand.HINT_COLOR));
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().openCustomPage(ref, store, new SettingsPage(playerRef, this.plugin));
        }
    }

    /** A few lines on the settings and who uses them. */
    public static String[] describe(FlexiisGogglesPlugin plugin) {
        GogglesConfig config = plugin.config();
        return new String[]{
                "FlexiisGoggles: goggles %s, %d player(s) online wearing goggles, %d player(s) with own settings".formatted(
                        config.requireGoggles ? "required" : "not required", plugin.worn().count(), plugin.players().size()),
                "zoom %s: %s, %s, glide default %s".formatted(
                        config.zoom.enabled ? "on" : "off",
                        plugin.zoomLoaded() ? "%d steps %s-%sx loaded".formatted(ZoomSteps.count(), ZoomSteps.label(ZoomSteps.MIN), ZoomSteps.label(ZoomSteps.MAX)) : "roots not loaded",
                        config.zoom.spec(4f).describe(), config.zoom.glide),
                "night vision %s: on for %d player(s) online".formatted(
                        config.nightVision.enabled ? "on" : "off", plugin.nightVision().active())
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
