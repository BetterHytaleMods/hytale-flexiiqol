package de.hymods.flexiiqol.spike;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.HytaleServer;
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
import de.hymods.flexiiqol.fullbright.ChunkResend;
import de.hymods.flexiiqol.zoom.Glide;
import de.hymods.flexiiqol.zoom.ZoomBindings;
import de.hymods.flexiiqol.zoom.ZoomSpec;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Feasibility test (plan step 1a). From a player it acts on that player, from the console on every
 * online player.
 * <ul>
 *   <li>{@code zoom base}: bind the zoom root from the jar</li>
 *   <li>{@code zoom [mag=4 sens=0.35 in=0.15 out=0.25 persp=first|third|none overlay=none|spyglass|sniper|reticle1..3
 *   click=end|block dof=on|off item=ignore|cancel|fail|finish]}: change the live zoom, load it and bind it</li>
 *   <li>{@code unzoom}: remove the binding</li>
 *   <li>{@code input Ability3|Ability4|Pick|...}: the input later {@code zoom} calls bind (default Ability3 = R)</li>
 *   <li>{@code glide <lerp> [always] | glide off}: gliding server camera while zooming (or always)</li>
 *   <li>{@code light <0-15> | light off}: fullbright through rewritten chunk light, sends the loaded sections again</li>
 *   <li>{@code resend}: send the loaded sections again</li>
 *   <li>{@code probe on|off|counts}: log what the client sends</li>
 *   <li>{@code status}</li>
 * </ul>
 */
public final class SpikeCommand extends CommandBase {
    public static final String BASE_ROOT = "Root_FlexiiQOL_Zoom_Base";
    public static final String LIVE_ROOT = "Root_FlexiiQOL_Zoom_Live";

    private final FlexiiQolPlugin plugin;
    private final RequiredArg<String> argsArg = this.withRequiredArg("args", "flexiiqol.commands.qol.spike.args.desc", ArgTypes.GREEDY_STRING);
    private final AtomicReference<ZoomSpec> live = new AtomicReference<>(ZoomSpec.defaults());
    private final AtomicReference<InteractionType> input = new AtomicReference<>(InteractionType.Ability3);

    public SpikeCommand(FlexiiQolPlugin plugin) {
        super("spike", "flexiiqol.commands.qol.spike.desc");
        this.plugin = plugin;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        List<String> args = Arrays.stream(this.argsArg.get(context).trim().split("\\s+")).filter(s -> !s.isEmpty()).toList();
        if (args.isEmpty()) {
            reply(context, "modes: zoom, unzoom, input, glide, light, resend, probe, status");
            return;
        }
        List<PlayerRef> targets = targets(context);
        List<String> rest = args.subList(1, args.size());
        try {
            switch (args.getFirst().toLowerCase(Locale.ROOT)) {
                case "zoom" -> this.zoom(context, targets, rest);
                case "unzoom" -> {
                    InteractionType type = this.input.get();
                    targets.forEach(p -> Players.onWorld(p, (store, ref) -> ZoomBindings.unbind(store, ref, type)));
                    reply(context, "unbound " + type + " for " + names(targets));
                }
                case "input" -> {
                    InteractionType type = InteractionType.valueOf(only(rest, "input needs a type, e.g. Ability3"));
                    this.input.set(type);
                    reply(context, "zoom binds " + type + " from now on");
                }
                case "glide" -> this.glide(context, targets, rest);
                case "light" -> this.light(context, targets, rest);
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
        InteractionType type = this.input.get();
        if (settings.size() == 1 && settings.getFirst().equals("base")) {
            this.bind(context, targets, type, BASE_ROOT, "the jar's zoom");
            return;
        }
        ZoomSpec spec = this.live.get();
        for (String setting : settings) {
            int eq = setting.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException("settings look like mag=4, not " + setting);
            }
            spec = spec.with(setting.substring(0, eq), setting.substring(eq + 1));
        }
        ZoomSpec chosen = spec;
        long started = System.nanoTime();
        CompletableFuture.supplyAsync(() -> {
            try {
                return this.plugin.zoomAssets().load(LIVE_ROOT, chosen);
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }, HytaleServer.SCHEDULED_EXECUTOR).whenComplete((rootId, error) -> {
            if (error != null) {
                reply(context, "zoom: not loaded: " + error.getMessage());
                return;
            }
            this.live.set(chosen);
            reply(context, "zoom: loaded in %d ms: %s".formatted((System.nanoTime() - started) / 1_000_000, chosen.describe()));
            this.bind(context, targets, type, rootId, "the live zoom");
        });
    }

    private void bind(CommandContext context, List<PlayerRef> targets, InteractionType type, String rootId, String what) {
        targets.forEach(p -> Players.onWorld(p, (store, ref) -> ZoomBindings.bind(store, ref, type, rootId)));
        reply(context, "bound " + what + " (" + rootId + ") to " + type + " for " + names(targets));
    }

    private void glide(CommandContext context, List<PlayerRef> targets, List<String> rest) {
        String first = rest.isEmpty() ? "" : rest.getFirst();
        Glide.Setting setting;
        if (first.equals("off")) {
            setting = null;
        } else {
            float lerp;
            try {
                lerp = Float.parseFloat(first);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("glide needs a lerp between 0 and 1, or off");
            }
            setting = new Glide.Setting(lerp, rest.size() > 1 && rest.get(1).equals("always"));
        }
        targets.forEach(p -> this.plugin.glide().set(p.getUuid(), setting));
        reply(context, (setting == null ? "glide off" : "glide " + setting.lerp() + (setting.always() ? " always" : " while zooming"))
                + " for " + names(targets));
    }

    private void light(CommandContext context, List<PlayerRef> targets, List<String> rest) {
        String first = only(rest, "light needs a level 0-15 or off");
        for (PlayerRef p : targets) {
            if (first.equals("off")) {
                this.plugin.fullbright().clear(p.getUuid());
            } else {
                int level;
                try {
                    level = Integer.parseInt(first);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("light needs a level 0-15 or off");
                }
                this.plugin.fullbright().set(p.getUuid(), level);
            }
            long started = System.nanoTime();
            ChunkResend.all(p).thenAccept(n -> reply(context, "light %s for %s, resending %d section(s) (queued in %d ms)".formatted(
                    first, p.getUsername(), n, (System.nanoTime() - started) / 1_000_000)));
        }
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
        reply(context, "live zoom: " + this.live.get().describe() + ", binds " + this.input.get() + "; loaded roots: base "
                + (RootInteraction.getAssetMap().getAsset(BASE_ROOT) != null) + ", live " + (RootInteraction.getAssetMap().getAsset(LIVE_ROOT) != null));
        reply(context, "fullbright: " + this.plugin.fullbright().players() + " player(s), " + this.plugin.fullbright().stats());
        for (PlayerRef p : targets) {
            Players.onWorld(p, (store, ref) -> {
                String bound = ZoomBindings.bound(store, ref, this.input.get());
                Glide.Setting glide = this.plugin.glide().setting(p.getUuid());
                reply(context, "%s: %s on %s, glide %s, light %s".formatted(p.getUsername(), bound == null ? "no zoom" : bound,
                        this.input.get(), glide == null ? "off" : glide, this.plugin.fullbright().isOn(p.getUuid()) ? "on" : "off"));
            });
        }
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
