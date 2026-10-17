package de.hymods.flexiiqol.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.hymods.flexiiqol.FlexiiQolPlugin;
import de.hymods.flexiiqol.spike.SpikeCommand;

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

    private static final class Status extends CommandBase {
        private final FlexiiQolPlugin plugin;

        Status(FlexiiQolPlugin plugin) {
            super("status", "flexiiqol.commands.qol.status.desc");
            this.plugin = plugin;
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            context.sendMessage(Message.raw("FlexiiQOL zoom %s (%s), fullbright %s (level %d, %d player(s), %s), config %s".formatted(
                    this.plugin.config().zoomEnabled ? "on" : "off", this.plugin.config().zoomSpec().describe(),
                    this.plugin.config().fullbrightEnabled ? "on" : "off", this.plugin.config().fullbrightLevel,
                    this.plugin.fullbright().players(), this.plugin.fullbright().stats(), this.plugin.configFile())));
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
                this.plugin.reloadConfig();
                context.sendMessage(Message.raw("FlexiiQOL: config reloaded"));
            } catch (IOException | RuntimeException e) {
                context.sendMessage(Message.raw("FlexiiQOL: config not reloaded, keeping the old one: " + e.getMessage()));
            }
        }
    }
}
