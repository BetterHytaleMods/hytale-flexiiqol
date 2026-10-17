package de.hymods.flexiiqol.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.hymods.flexiiqol.fullbright.Fullbright;

import javax.annotation.Nonnull;

/** {@code /fullbright} for every player: switches fullbright on or off for oneself. */
public final class FullbrightCommand extends AbstractPlayerCommand {
    private static final String HINT_COLOR = "#aaaaaa";

    private final Fullbright fullbright;

    public FullbrightCommand(Fullbright fullbright) {
        super("fullbright", "flexiiqol.commands.fullbright.desc");
        this.fullbright = fullbright;
        this.requireNoPermission();
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (!this.fullbright.isAvailable()) {
            context.sendMessage(Message.translation("flexiiqol.fullbright.unavailable").color(HINT_COLOR));
            return;
        }
        boolean on = this.fullbright.toggle(playerRef);
        context.sendMessage(Message.translation(on ? "flexiiqol.fullbright.on" : "flexiiqol.fullbright.off").color(HINT_COLOR));
    }
}
