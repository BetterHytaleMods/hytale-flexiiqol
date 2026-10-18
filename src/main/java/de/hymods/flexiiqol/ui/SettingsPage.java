package de.hymods.flexiiqol.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.hymods.flexiiqol.FlexiiQolPlugin;
import de.hymods.flexiiqol.QolConfig;
import de.hymods.flexiiqol.settings.PlayerSettings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /qol}: zoom strength, glide and fullbright, each player for themselves. A change applies at
 * once and is saved; the systems pick it up on their next tick. The labels next to the sliders follow
 * them. While a page update is unconfirmed, the server drops the player's input, so releasing a slider
 * sends its value once more.
 */
public final class SettingsPage extends InteractiveCustomUIPage<SettingsPage.Event> {

    static final class Event {
        static final BuilderCodec<Event> CODEC = BuilderCodec.builder(Event.class, Event::new)
                .append(new KeyedCodec<>("Type", Codec.STRING), (event, s) -> event.type = s, event -> event.type)
                .add()
                .append(new KeyedCodec<>("@Value", Codec.FLOAT), (event, v) -> event.value = v, event -> event.value)
                .add()
                .append(new KeyedCodec<>("@On", Codec.BOOLEAN), (event, v) -> event.on = v, event -> event.on)
                .add()
                .build();
        /** Zoom, Glide, Brightness or Fullbright. */
        private String type;
        private Float value;
        private Boolean on;
    }

    private final FlexiiQolPlugin plugin;
    /** Each value label as last sent, so a slider that stays on its step sends nothing. */
    private final Map<String, String> labels = new HashMap<>();

    public SettingsPage(@Nonnull PlayerRef playerRef, @Nonnull FlexiiQolPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, Event.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commands, @Nonnull UIEventBuilder events,
                      @Nonnull Store<EntityStore> store) {
        commands.append("FlexiiQOL/Settings.ui");
        QolConfig config = this.plugin.config();
        PlayerSettings.Values values = this.plugin.values(this.playerRef.getUuid());
        commands.set("#ZoomSection.Visible", config.zoom.enabled);
        commands.set("#FullbrightSection.Visible", config.fullbright.enabled);
        commands.set("#Zoom.Value", values.zoom());
        commands.set("#Glide.Value", values.glide());
        commands.set("#Fullbright #CheckBox.Value", values.fullbright());
        commands.set("#Brightness.Value", values.brightness());
        this.zoomLabel(commands, values.zoom());
        this.glideLabel(commands, values.glide());
        this.brightnessLabel(commands, values.brightness());
        slider(events, "#Zoom", "Zoom");
        slider(events, "#Glide", "Glide");
        slider(events, "#Brightness", "Brightness");
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#Fullbright #CheckBox",
                new EventData().append("Type", "Fullbright").append("@On", "#Fullbright #CheckBox.Value"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Event event) {
        UUID player = this.playerRef.getUuid();
        UICommandBuilder commands = new UICommandBuilder();
        boolean changed = switch (event.type == null ? "" : event.type) {
            case "Zoom" -> {
                int zoom = step(event.value, PlayerSettings.ZOOM_MIN, PlayerSettings.ZOOM_MAX);
                if (zoom < 0) {
                    yield false;
                }
                this.plugin.choose(player, choice -> choice.withZoom(zoom));
                yield this.zoomLabel(commands, zoom);
            }
            case "Glide" -> {
                int glide = step(event.value, 0, PlayerSettings.GLIDE_MAX);
                if (glide < 0) {
                    yield false;
                }
                this.plugin.choose(player, choice -> choice.withGlide(glide));
                yield this.glideLabel(commands, glide);
            }
            case "Brightness" -> {
                int brightness = step(event.value, PlayerSettings.BRIGHTNESS_MIN, PlayerSettings.BRIGHTNESS_MAX);
                if (brightness < 0) {
                    yield false;
                }
                this.plugin.choose(player, choice -> choice.withBrightness(brightness));
                yield this.brightnessLabel(commands, brightness);
            }
            case "Fullbright" -> {
                if (event.on != null) {
                    boolean on = event.on;
                    this.plugin.choose(player, choice -> choice.withFullbright(on));
                }
                yield false;
            }
            default -> false;
        };
        if (changed) {
            this.sendUpdate(commands, null, false);
        }
    }

    private static void slider(UIEventBuilder events, String selector, String type) {
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector,
                new EventData().append("Type", type).append("@Value", selector + ".Value"), false);
        events.addEventBinding(CustomUIEventBindingType.MouseButtonReleased, selector,
                new EventData().append("Type", type).append("@Value", selector + ".Value"), false);
    }

    /** The slider's step, or -1 if the value is missing. */
    private static int step(@Nullable Float value, int min, int max) {
        if (value == null || !Float.isFinite(value)) {
            return -1;
        }
        return Math.clamp(Math.round(value), min, max);
    }

    private boolean zoomLabel(UICommandBuilder commands, int zoom) {
        String text = zoom + "x";
        return this.label(commands, "#ZoomValue", text, Message.raw(text));
    }

    private boolean glideLabel(UICommandBuilder commands, int glide) {
        return glide == 0
                ? this.label(commands, "#GlideValue", "off", Message.translation("flexiiqol.settings.off"))
                : this.label(commands, "#GlideValue", String.format(Locale.ROOT, "%.1f", glide / 10f), Message.raw(String.format(Locale.ROOT, "%.1f", glide / 10f)));
    }

    private boolean brightnessLabel(UICommandBuilder commands, int brightness) {
        String text = String.valueOf(brightness);
        return this.label(commands, "#BrightnessValue", text, Message.raw(text));
    }

    /** Sets the label if it shows something else; returns whether it did. */
    private boolean label(UICommandBuilder commands, String selector, String key, Message text) {
        if (key.equals(this.labels.put(selector, key))) {
            return false;
        }
        commands.set(selector + ".TextSpans", text);
        return true;
    }
}
