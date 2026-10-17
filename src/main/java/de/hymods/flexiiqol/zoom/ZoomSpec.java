package de.hymods.flexiiqol.zoom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.Map;

/**
 * The values of one zoom and the root interaction that carries them. Hytale's client-side zoom
 * ({@code Effects.Zoom}) runs while a {@code Charging} step is held; {@code AllowIndefiniteHold}
 * without a {@code Next} entry ends the chain, and with it the zoom, the moment the key is released.
 */
public record ZoomSpec(
        float magnification,
        float sensitivity,
        float inLerp,
        float outLerp,
        String perspective,
        String overlay,
        boolean endOnClick,
        boolean depthOfField,
        String onItemChange) {

    /** Overlays the client ships, under {@code Common/ScreenEffects/}. */
    private static final Map<String, String> OVERLAYS = Map.of(
            "none", "",
            "spyglass", "ScreenEffects/Spyglass.png",
            "sniper", "ScreenEffects/Sniper.png",
            "reticle1", "ScreenEffects/reticle_1.png",
            "reticle2", "ScreenEffects/reticle_2.png",
            "reticle3", "ScreenEffects/reticle_3.png");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ZoomSpec {
        if (!(magnification > 1f && magnification <= 32f)) {
            throw new IllegalArgumentException("magnification must be above 1 and at most 32");
        }
        if (!(sensitivity > 0f && sensitivity <= 1f)) {
            throw new IllegalArgumentException("sensitivity must be above 0 and at most 1");
        }
        if (!(inLerp > 0f && inLerp <= 1f) || !(outLerp > 0f && outLerp <= 1f)) {
            throw new IllegalArgumentException("in and out must be above 0 and at most 1");
        }
        if (!perspective.equals("None") && !perspective.equals("FirstPerson") && !perspective.equals("ThirdPerson")) {
            throw new IllegalArgumentException("perspective must be None, FirstPerson or ThirdPerson");
        }
        if (!OVERLAYS.containsValue(overlay)) {
            throw new IllegalArgumentException("overlay must be one of " + String.join(", ", OVERLAYS.keySet()));
        }
        if (!onItemChange.equals("Cancel") && !onItemChange.equals("Fail") && !onItemChange.equals("Finish")
                && !onItemChange.equals("Ignore")) {
            throw new IllegalArgumentException("item must be Cancel, Fail, Finish or Ignore");
        }
    }

    public static ZoomSpec defaults() {
        return new ZoomSpec(4f, 0.35f, 0.15f, 0.25f, "FirstPerson", "", true, false, "Ignore");
    }

    /**
     * One value changed by name, for {@code /qol spike zoom mag=4 sens=0.3 ...}.
     *
     * @throws IllegalArgumentException for an unknown name or a value out of range
     */
    public ZoomSpec with(String name, String value) {
        String v = value.trim();
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "mag" -> new ZoomSpec(number(name, v), sensitivity, inLerp, outLerp, perspective, overlay, endOnClick, depthOfField, onItemChange);
            case "sens" -> new ZoomSpec(magnification, number(name, v), inLerp, outLerp, perspective, overlay, endOnClick, depthOfField, onItemChange);
            case "in" -> new ZoomSpec(magnification, sensitivity, number(name, v), outLerp, perspective, overlay, endOnClick, depthOfField, onItemChange);
            case "out" -> new ZoomSpec(magnification, sensitivity, inLerp, number(name, v), perspective, overlay, endOnClick, depthOfField, onItemChange);
            case "persp" -> new ZoomSpec(magnification, sensitivity, inLerp, outLerp, perspective(v), overlay, endOnClick, depthOfField, onItemChange);
            case "overlay" -> new ZoomSpec(magnification, sensitivity, inLerp, outLerp, perspective, overlay(v), endOnClick, depthOfField, onItemChange);
            case "click" -> new ZoomSpec(magnification, sensitivity, inLerp, outLerp, perspective, overlay, click(v), depthOfField, onItemChange);
            case "dof" -> new ZoomSpec(magnification, sensitivity, inLerp, outLerp, perspective, overlay, endOnClick, flag(name, v), onItemChange);
            case "item" -> new ZoomSpec(magnification, sensitivity, inLerp, outLerp, perspective, overlay, endOnClick, depthOfField, capitalized(v));
            default -> throw new IllegalArgumentException("unknown setting " + name
                    + " (mag, sens, in, out, persp, overlay, click, dof, item)");
        };
    }

    /** The root interaction as JSON, with the zoom step inline. */
    public String toRootJson() {
        JsonObject zoom = new JsonObject();
        zoom.addProperty("MagnificationMultiplier", magnification);
        zoom.addProperty("MouseSensitivityMultiplier", sensitivity);
        if (!overlay.isEmpty()) {
            zoom.addProperty("OverlayTexture", overlay);
            zoom.addProperty("OverlayFade", true);
        }
        zoom.addProperty("DepthOfField", depthOfField);
        zoom.addProperty("ForcePerspective", perspective);
        zoom.addProperty("InLerp", inLerp);
        zoom.addProperty("OutLerp", outLerp);

        JsonObject effects = new JsonObject();
        effects.add("Zoom", zoom);

        JsonObject hold = new JsonObject();
        hold.addProperty("Type", "Charging");
        hold.addProperty("AllowIndefiniteHold", true);
        hold.addProperty("DisplayProgress", false);
        hold.addProperty("CancelOnOtherClick", endOnClick);
        hold.addProperty("OnItemChangeBehavior", onItemChange);
        hold.add("Effects", effects);

        JsonObject cooldown = new JsonObject();
        cooldown.addProperty("Cooldown", 0f);
        JsonObject rules = new JsonObject();
        rules.add("BlockedBy", new JsonArray());
        JsonArray interactions = new JsonArray();
        interactions.add(hold);

        JsonObject root = new JsonObject();
        root.addProperty("RequireNewClick", true);
        root.add("Cooldown", cooldown);
        root.add("Rules", rules);
        root.add("Interactions", interactions);
        return GSON.toJson(root) + "\n";
    }

    public String describe() {
        return String.format(Locale.ROOT, "%sx, sens %s, in %s, out %s, %s, overlay %s, click %s, dof %s, item %s",
                trim(magnification), trim(sensitivity), trim(inLerp), trim(outLerp), perspective,
                overlay.isEmpty() ? "none" : overlay, endOnClick ? "ends zoom" : "blocked", depthOfField, onItemChange);
    }

    private static float number(String name, String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " needs a number, not " + value);
        }
    }

    private static boolean flag(String name, String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "on", "true", "yes" -> true;
            case "off", "false", "no" -> false;
            default -> throw new IllegalArgumentException(name + " needs on or off, not " + value);
        };
    }

    private static boolean click(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "end" -> true;
            case "block" -> false;
            default -> throw new IllegalArgumentException("click needs end or block, not " + value);
        };
    }

    private static String perspective(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "none" -> "None";
            case "first" -> "FirstPerson";
            case "third" -> "ThirdPerson";
            default -> throw new IllegalArgumentException("persp needs none, first or third, not " + value);
        };
    }

    private static String overlay(String value) {
        String path = OVERLAYS.get(value.toLowerCase(Locale.ROOT));
        if (path == null) {
            throw new IllegalArgumentException("overlay needs one of " + String.join(", ", OVERLAYS.keySet()));
        }
        return path;
    }

    private static String capitalized(String value) {
        return value.isEmpty() ? value : value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String trim(float value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
    }
}
