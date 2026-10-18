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

/** {@code /nightvision} ({@code /nv}) for every player: switches night vision on or off, while goggles with it are on. */
public final class NightVisionCommand extends AbstractPlayerCommand {
    static final String HINT_COLOR = "#aaaaaa";

    private final FlexiisGogglesPlugin plugin;

    public NightVisionCommand(FlexiisGogglesPlugin plugin) {
        super("nightvision", "flexiisgoggles.commands.nightvision.desc");
        this.plugin = plugin;
        this.addAliases("nv");
        this.requireNoPermission();
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (!this.plugin.config().nightVision.enabled) {
            context.sendMessage(Message.translation("flexiisgoggles.nightvision.unavailable").color(HINT_COLOR));
            return;
        }
        UUID player = playerRef.getUuid();
        if (this.plugin.ability(player).nightVisionCap() == 0) {
            context.sendMessage(Message.translation("flexiisgoggles.nightvision.none").color(HINT_COLOR));
            return;
        }
        boolean on = !this.plugin.effective(player).nightVision();
        this.plugin.choose(player, choice -> choice.withNightVision(on));
        context.sendMessage(Message.translation(on ? "flexiisgoggles.nightvision.on" : "flexiisgoggles.nightvision.off").color(HINT_COLOR));
    }
}
