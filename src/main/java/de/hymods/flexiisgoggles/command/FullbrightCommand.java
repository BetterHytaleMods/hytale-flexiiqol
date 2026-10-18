package de.hymods.flexiisgoggles.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.hymods.flexiisgoggles.FlexiisGogglesPlugin;

import javax.annotation.Nonnull;
import java.util.UUID;

/** {@code /fullbright} for every player: switches fullbright on or off for oneself, at the brightness picked on {@code /goggles}. */
public final class FullbrightCommand extends AbstractPlayerCommand {
    private static final String HINT_COLOR = "#aaaaaa";

    private final FlexiisGogglesPlugin plugin;

    public FullbrightCommand(FlexiisGogglesPlugin plugin) {
        super("fullbright", "flexiisgoggles.commands.fullbright.desc");
        this.plugin = plugin;
        this.requireNoPermission();
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (!this.plugin.config().fullbright.enabled) {
            context.sendMessage(Message.translation("flexiisgoggles.fullbright.unavailable").color(HINT_COLOR));
            return;
        }
        UUID player = playerRef.getUuid();
        boolean on = !this.plugin.values(player).fullbright();
        this.plugin.choose(player, choice -> choice.withFullbright(on));
        context.sendMessage(Message.translation(on ? "flexiisgoggles.fullbright.on" : "flexiisgoggles.fullbright.off").color(HINT_COLOR));
    }
}
