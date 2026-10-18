package de.hymods.flexiisgoggles.ui;

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
import de.hymods.flexiisgoggles.FlexiisGogglesPlugin;
import de.hymods.flexiisgoggles.goggles.Effective;
import de.hymods.flexiisgoggles.settings.PlayerSettings;
import de.hymods.flexiisgoggles.zoom.ZoomSteps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /goggles}: zoom strength, glide and night vision, each player for themselves and as far as
 * the goggles they wear allow. The sliders end at the goggles' strength, sections the goggles don't
 * have stay hidden. A change applies at once and is saved; the systems pick it up on their next tick.
 * While a page update is unconfirmed, the server drops the player's input, so releasing a slider
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
        /** Zoom, Glide, Brightness or NightVision. */
        private String type;
        private Float value;
        private Boolean on;
    }

    private final FlexiisGogglesPlugin plugin;
    /** Each value label as last sent, so a slider that stays on its step sends nothing. */
    private final Map<String, String> labels = new HashMap<>();

    public SettingsPage(@Nonnull PlayerRef playerRef, @Nonnull FlexiisGogglesPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, Event.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commands, @Nonnull UIEventBuilder events,
                      @Nonnull Store<EntityStore> store) {
        commands.append("FlexiisGoggles/Settings.ui");
        Effective effective = this.plugin.effective(this.playerRef.getUuid());

        int zoomMax = effective.zoomCap() > 0f ? ZoomSteps.maxIndex(effective.zoomCap()) : 0;
        commands.set("#ZoomSection.Visible", effective.zoomCap() > 0f);
        // A slider needs a range; with a single strength the row stays hidden.
        commands.set("#ZoomStrengthRow.Visible", zoomMax > 0);
        if (zoomMax > 0) {
            commands.set("#Zoom.Max", zoomMax);
            commands.set("#Zoom.Value", ZoomSteps.index(effective.zoom()));
        }
        commands.set("#Glide.Value", effective.glide());
        this.zoomLabel(commands, effective.zoom());
        this.glideLabel(commands, effective.glide());

        int brightnessMax = effective.brightnessCap();
        commands.set("#NightVisionSection.Visible", brightnessMax > 0);
        commands.set("#BrightnessRow.Visible", brightnessMax > PlayerSettings.BRIGHTNESS_MIN);
        commands.set("#NightVision #CheckBox.Value", effective.nightVision());
        if (brightnessMax > PlayerSettings.BRIGHTNESS_MIN) {
            commands.set("#Brightness.Max", brightnessMax);
            commands.set("#Brightness.Value", effective.brightness());
        }
        this.brightnessLabel(commands, effective.brightness());

        slider(events, "#Zoom", "Zoom");
        slider(events, "#Glide", "Glide");
        slider(events, "#Brightness", "Brightness");
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NightVision #CheckBox",
                new EventData().append("Type", "NightVision").append("@On", "#NightVision #CheckBox.Value"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Event event) {
        UUID player = this.playerRef.getUuid();
        Effective now = this.plugin.effective(player);
        UICommandBuilder commands = new UICommandBuilder();
        boolean changed = switch (event.type == null ? "" : event.type) {
            case "Zoom" -> {
                if (event.value == null || !Float.isFinite(event.value) || now.zoomCap() <= 0f) {
                    yield false;
                }
                int max = ZoomSteps.maxIndex(now.zoomCap());
                int index = Math.clamp(Math.round(event.value), 0, max);
                boolean snapped = Math.round(event.value) > max;
                if (snapped) {
                    commands.set("#Zoom.Value", index);
                }
                float zoom = ZoomSteps.value(index);
                this.plugin.choose(player, choice -> choice.withZoom(zoom));
                yield this.zoomLabel(commands, zoom) || snapped;
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
                int cap = now.brightnessCap();
                int brightness = step(event.value, PlayerSettings.BRIGHTNESS_MIN, Math.max(cap, PlayerSettings.BRIGHTNESS_MIN));
                if (brightness < 0 || cap <= 0) {
                    yield false;
                }
                boolean snapped = Math.round(event.value) > cap;
                if (snapped) {
                    commands.set("#Brightness.Value", brightness);
                }
                this.plugin.choose(player, choice -> choice.withBrightness(brightness));
                yield this.brightnessLabel(commands, brightness) || snapped;
            }
            case "NightVision" -> {
                if (event.on != null) {
                    boolean on = event.on;
                    this.plugin.choose(player, choice -> choice.withNightVision(on));
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

    private boolean zoomLabel(UICommandBuilder commands, float zoom) {
        String text = ZoomSteps.label(zoom) + "x";
        return this.label(commands, "#ZoomValue", text, Message.raw(text));
    }

    private boolean glideLabel(UICommandBuilder commands, int glide) {
        if (glide == 0) {
            return this.label(commands, "#GlideValue", "off", Message.translation("flexiisgoggles.settings.off"));
        }
        String text = String.format(Locale.ROOT, "%.1f", glide / 10f);
        return this.label(commands, "#GlideValue", text, Message.raw(text));
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
