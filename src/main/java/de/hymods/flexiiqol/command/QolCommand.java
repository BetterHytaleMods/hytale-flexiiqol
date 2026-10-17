package de.hymods.flexiiqol.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.hymods.flexiiqol.FlexiiQolPlugin;
import de.hymods.flexiiqol.QolConfig;
import de.hymods.flexiiqol.spike.SpikeCommand;
import de.hymods.flexiiqol.zoom.Glide;
import de.hymods.flexiiqol.zoom.ZoomSpec;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * {@code /qol status|reload|spike} for operators and the console. Commands need a permission unless
 * they say otherwise, so normal players cannot use any of these.
 */
public final class QolCommand extends AbstractCommandCollection {

    public QolCommand(FlexiiQolPlugin plugin) {
        super("qol", "flexiiqol.commands.qol.desc");
        this.addSubCommand(new Status(plugin));
        this.addSubCommand(new Reload(plugin));
        this.addSubCommand(new SpikeCommand(plugin));
    }

    /** One line each for the zoom and fullbright. */
    public static String[] describe(FlexiiQolPlugin plugin) {
        QolConfig config = plugin.config();
        ZoomSpec zoom = plugin.zoom();
        Glide.Setting glide = plugin.glide().setting();
        return new String[]{
                "FlexiiQOL zoom %s: %s, glide %s".formatted(
                        config.zoom.enabled ? "on" : "off",
                        zoom == null ? "root not loaded" : zoom.describe(),
                        glide == null ? "off" : glide.glide() + (glide.always() ? " always" : "")),
                "FlexiiQOL fullbright %s: level %d, %d player(s) with it on, %s".formatted(
                        plugin.fullbright().isAvailable() ? "on" : "off", config.fullbright.level,
                        plugin.fullbright().players(), plugin.fullbright().stats())
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
                this.plugin.reload().whenComplete((zoom, error) -> {
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
