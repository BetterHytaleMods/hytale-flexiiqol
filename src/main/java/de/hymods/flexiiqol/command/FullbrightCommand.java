package de.hymods.flexiiqol.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.hymods.flexiiqol.FlexiiQolPlugin;

import javax.annotation.Nonnull;
import java.util.UUID;

/** {@code /fullbright} for every player: switches fullbright on or off for oneself, at the brightness picked on {@code /qol}. */
public final class FullbrightCommand extends AbstractPlayerCommand {
    private static final String HINT_COLOR = "#aaaaaa";

    private final FlexiiQolPlugin plugin;

    public FullbrightCommand(FlexiiQolPlugin plugin) {
        super("fullbright", "flexiiqol.commands.fullbright.desc");
        this.plugin = plugin;
        this.requireNoPermission();
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (!this.plugin.config().fullbright.enabled) {
            context.sendMessage(Message.translation("flexiiqol.fullbright.unavailable").color(HINT_COLOR));
            return;
        }
        UUID player = playerRef.getUuid();
        boolean on = !this.plugin.values(player).fullbright();
        this.plugin.choose(player, choice -> choice.withFullbright(on));
        context.sendMessage(Message.translation(on ? "flexiiqol.fullbright.on" : "flexiiqol.fullbright.off").color(HINT_COLOR));
    }
}
