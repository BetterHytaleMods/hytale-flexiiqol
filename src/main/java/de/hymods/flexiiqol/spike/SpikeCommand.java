package de.hymods.flexiiqol.spike;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import de.hymods.flexiiqol.FlexiiQolPlugin;
import de.hymods.flexiiqol.Players;
import de.hymods.flexiiqol.command.QolCommand;
import de.hymods.flexiiqol.fullbright.ChunkResend;
import de.hymods.flexiiqol.zoom.Glide;
import de.hymods.flexiiqol.zoom.ZoomAssets;
import de.hymods.flexiiqol.zoom.ZoomBindingSystem;
import de.hymods.flexiiqol.zoom.ZoomBindings;
import de.hymods.flexiiqol.zoom.ZoomRules;
import de.hymods.flexiiqol.zoom.ZoomSpec;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Feasibility test (plan step 1a). Changes here last until the next restart or {@code /qol reload};
 * {@code config.json} stays as it is. From a player it acts on that player, from the console on every
 * online player.
 * <ul>
 *   <li>{@code zoom [mag=4 sens=0.35 in=0.15 out=0.25 persp=first|third|none overlay=none|spyglass|sniper|reticle1..3
 *   click=end|block dof=on|off item=ignore|cancel|fail|finish]}: change the zoom on R</li>
 *   <li>{@code zoom base}: put the zoom shipped in the jar on R instead, until {@code zoom} again</li>
 *   <li>{@code bind <input>} / {@code unbind <input>}: the zoom on another input as well, e.g. Ability4 or Pick</li>
 *   <li>{@code glide <0-0.95> [always] | glide off}: gliding camera while zooming (or always)</li>
 *   <li>{@code light <1-15> | light off}: fullbright at that level for the targets</li>
 *   <li>{@code resend}: send the loaded sections again</li>
 *   <li>{@code probe on|off|counts}: log what the client sends</li>
 *   <li>{@code status}</li>
 * </ul>
 */
public final class SpikeCommand extends CommandBase {
    public static final String BASE_ROOT = "Root_FlexiiQOL_Zoom_Base";

    private final FlexiiQolPlugin plugin;
    private final RequiredArg<String> argsArg = this.withRequiredArg("args", "flexiiqol.commands.qol.spike.args.desc", ArgTypes.GREEDY_STRING);

    public SpikeCommand(FlexiiQolPlugin plugin) {
        super("spike", "flexiiqol.commands.qol.spike.desc");
        this.plugin = plugin;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        List<String> args = Arrays.stream(this.argsArg.get(context).trim().split("\\s+")).filter(s -> !s.isEmpty()).toList();
        if (args.isEmpty()) {
            reply(context, "modes: zoom, bind, unbind, glide, light, resend, probe, status");
            return;
        }
        List<PlayerRef> targets = targets(context);
        List<String> rest = args.subList(1, args.size());
        try {
            switch (args.getFirst().toLowerCase(Locale.ROOT)) {
                case "zoom" -> this.zoom(context, targets, rest);
                case "bind" -> this.bind(context, targets, input(only(rest, "bind needs an input, e.g. Ability4")), true);
                case "unbind" -> this.bind(context, targets, input(only(rest, "unbind needs an input, e.g. Ability4")), false);
                case "glide" -> this.glide(context, rest);
                case "light" -> this.light(context, targets, only(rest, "light needs a level 1-15 or off"));
                case "resend" -> targets.forEach(p -> ChunkResend.all(p).thenAccept(n -> reply(context, "resending " + n + " section(s) to " + p.getUsername())));
                case "probe" -> this.probe(context, targets, only(rest, "probe needs on, off or counts"));
                case "status" -> this.status(context, targets);
                default -> reply(context, "unknown mode " + args.getFirst());
            }
        } catch (IllegalArgumentException e) {
            reply(context, "spike: " + e.getMessage());
        }
    }

    private void zoom(CommandContext context, List<PlayerRef> targets, List<String> settings) {
        if (settings.size() == 1 && settings.getFirst().equals("base")) {
            targets.forEach(p -> Players.onWorld(p, (store, ref) -> {
                ZoomBindings.unbind(store, ref, ZoomBindingSystem.INPUT);
                ZoomBindings.bind(store, ref, ZoomBindingSystem.INPUT, BASE_ROOT);
            }));
            reply(context, "R carries the jar's zoom (" + BASE_ROOT + ") for " + names(targets) + "; /qol spike zoom goes back");
            return;
        }
        ZoomSpec spec = this.plugin.zoom() != null ? this.plugin.zoom() : this.plugin.config().zoom.spec();
        for (String setting : settings) {
            int eq = setting.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException("settings look like mag=4, not " + setting);
            }
            spec = spec.with(setting.substring(0, eq), setting.substring(eq + 1));
        }
        long started = System.nanoTime();
        this.plugin.loadZoomLater(spec).whenComplete((loaded, error) -> {
            if (error != null) {
                reply(context, "zoom: not loaded: " + error.getMessage());
                return;
            }
            reply(context, "zoom: loaded in %d ms: %s".formatted((System.nanoTime() - started) / 1_000_000, loaded.describe()));
            // Players on the jar's zoom go back to the configured one.
            targets.forEach(p -> Players.onWorld(p, (store, ref) -> {
                if (BASE_ROOT.equals(ZoomBindings.bound(store, ref, ZoomBindingSystem.INPUT))) {
                    ZoomBindings.unbind(store, ref, ZoomBindingSystem.INPUT);
                }
            }));
        });
    }

    private void bind(CommandContext context, List<PlayerRef> targets, InteractionType input, boolean on) {
        if (input == ZoomBindingSystem.INPUT) {
            throw new IllegalArgumentException(input + " is R, which the zoom manages itself");
        }
        targets.forEach(p -> Players.onWorld(p, (store, ref) -> {
            if (!on) {
                ZoomBindings.unbind(store, ref, input);
            } else if (!ZoomBindings.bind(store, ref, input, ZoomAssets.ROOT)) {
                reply(context, input + " is taken for " + p.getUsername());
            }
        }));
        reply(context, (on ? "zoom also on " : "zoom off ") + input + " for " + names(targets));
    }

    private void glide(CommandContext context, List<String> rest) {
        String first = rest.isEmpty() ? "" : rest.getFirst();
        Glide.Setting setting;
        if (first.equals("off")) {
            setting = null;
        } else {
            float glide;
            try {
                glide = Float.parseFloat(first);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("glide needs an amount above 0 and up to 0.95, or off");
            }
            if (!(glide > 0f && glide <= 0.95f)) {
                throw new IllegalArgumentException("glide needs an amount above 0 and up to 0.95, or off");
            }
            setting = Glide.Setting.of(glide, rest.size() > 1 && rest.get(1).equals("always"));
        }
        this.plugin.glide().set(setting);
        reply(context, setting == null ? "glide off" : "glide " + setting.glide() + (setting.always() ? " always" : " while zooming"));
    }

    private void light(CommandContext context, List<PlayerRef> targets, String value) {
        if (value.equals("off")) {
            targets.forEach(p -> this.plugin.fullbright().set(p, false));
            reply(context, "fullbright off for " + names(targets));
            return;
        }
        int level;
        try {
            level = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("light needs a level 1-15 or off");
        }
        if (level < 1 || level > 15) {
            throw new IllegalArgumentException("light needs a level 1-15 or off");
        }
        this.plugin.fullbright().configure(true, level);
        targets.forEach(p -> this.plugin.fullbright().set(p, true));
        reply(context, "fullbright at level " + level + " for " + names(targets) + ", sending their sections again");
    }

    private void probe(CommandContext context, List<PlayerRef> targets, String mode) {
        switch (mode) {
            case "on", "off" -> {
                targets.forEach(p -> this.plugin.probe().watch(p.getUuid(), mode.equals("on")));
                reply(context, "probe " + mode + " for " + names(targets));
            }
            case "counts" -> reply(context, "probe counts " + this.plugin.probe().counts());
            default -> throw new IllegalArgumentException("probe needs on, off or counts");
        }
    }

    private void status(CommandContext context, List<PlayerRef> targets) {
        for (String line : QolCommand.describe(this.plugin)) {
            reply(context, line);
        }
        reply(context, "loaded roots: zoom " + (RootInteraction.getAssetMap().getAsset(ZoomAssets.ROOT) != null)
                + ", base " + (RootInteraction.getAssetMap().getAsset(BASE_ROOT) != null));
        for (PlayerRef p : targets) {
            Players.onWorld(p, (store, ref) -> {
                String bound = ZoomBindings.bound(store, ref, ZoomBindingSystem.INPUT);
                ZoomRules.Hold hold = this.plugin.zoomBindings().hold(p.getUuid());
                reply(context, "%s: R %s (%s), glide camera %s, fullbright %s".formatted(p.getUsername(),
                        bound == null ? "free" : bound, hold == null ? "not checked yet" : hold,
                        this.plugin.glide().isActive(p.getUuid()) ? "active" : "idle",
                        this.plugin.fullbright().isOn(p.getUuid()) ? "on" : "off"));
            });
        }
    }

    private static InteractionType input(String name) {
        for (InteractionType type : InteractionType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown input " + name);
    }

    private static List<PlayerRef> targets(CommandContext context) {
        if (context.isPlayer()) {
            PlayerRef self = Universe.get().getPlayer(context.sender().getUuid());
            return self == null ? List.of() : List.of(self);
        }
        return List.copyOf(Universe.get().getPlayers());
    }

    private static String only(List<String> rest, String problem) {
        if (rest.size() != 1) {
            throw new IllegalArgumentException(problem);
        }
        return rest.getFirst();
    }

    private static String names(List<PlayerRef> players) {
        return players.isEmpty() ? "nobody (no player online)" : String.join(", ", players.stream().map(PlayerRef::getUsername).toList());
    }

    private static void reply(CommandContext context, String text) {
        context.sendMessage(Message.raw(text));
    }
}
