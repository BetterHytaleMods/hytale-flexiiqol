package de.hymods.flexiisgoggles.zoom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Locale;

/**
 * One zoom step and the root interaction that carries it. Hytale's client-side zoom
 * ({@code Effects.Zoom}) runs while a {@code Charging} step is held; {@code AllowIndefiniteHold}
 * without a {@code Next} entry ends the chain, and with it the zoom, the moment the key is released.
 * The view goes to first person, the held item is hidden, and a click ends the zoom.
 */
public record ZoomSpec(int magnification, float sensitivity, float inLerp, float outLerp) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ZoomSpec {
        if (magnification < 2 || magnification > 32) {
            throw new IllegalArgumentException("magnification must be 2 to 32");
        }
        if (!(sensitivity > 0f && sensitivity <= 1f)) {
            throw new IllegalArgumentException("sensitivity must be above 0 and at most 1");
        }
        if (!(inLerp > 0f && inLerp <= 1f) || !(outLerp > 0f && outLerp <= 1f)) {
            throw new IllegalArgumentException("in and out must be above 0 and at most 1");
        }
    }

    /** The root interaction as JSON, with the zoom step inline. */
    public String toRootJson() {
        JsonObject zoom = new JsonObject();
        zoom.addProperty("MagnificationMultiplier", magnification);
        zoom.addProperty("MouseSensitivityMultiplier", sensitivity);
        zoom.addProperty("DepthOfField", false);
        zoom.addProperty("ForcePerspective", "FirstPerson");
        zoom.addProperty("InLerp", inLerp);
        zoom.addProperty("OutLerp", outLerp);

        JsonObject effects = new JsonObject();
        effects.addProperty("HideFirstPersonHeldItem", true);
        effects.add("Zoom", zoom);

        JsonObject hold = new JsonObject();
        hold.addProperty("Type", "Charging");
        hold.addProperty("AllowIndefiniteHold", true);
        hold.addProperty("DisplayProgress", false);
        hold.addProperty("CancelOnOtherClick", true);
        hold.addProperty("OnItemChangeBehavior", "Ignore");
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
        return String.format(Locale.ROOT, "%dx, sens %.2f, in %s, out %s", magnification, sensitivity, trim(inLerp), trim(outLerp));
    }

    private static String trim(float value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
    }
}
