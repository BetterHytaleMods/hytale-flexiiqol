package de.hymods.flexiiqol.zoom;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoomSpecTest {

    @Test
    void rootHoldsTheZoomWhileTheKeyIsDown() {
        JsonObject root = JsonParser.parseString(ZoomSpec.defaults().toRootJson()).getAsJsonObject();
        assertTrue(root.get("RequireNewClick").getAsBoolean());
        assertEquals(0f, root.getAsJsonObject("Cooldown").get("Cooldown").getAsFloat());
        assertEquals(0, root.getAsJsonObject("Rules").getAsJsonArray("BlockedBy").size());

        JsonObject hold = root.getAsJsonArray("Interactions").get(0).getAsJsonObject();
        assertEquals("Charging", hold.get("Type").getAsString());
        assertTrue(hold.get("AllowIndefiniteHold").getAsBoolean());
        assertFalse(hold.get("DisplayProgress").getAsBoolean());
        assertFalse(hold.has("Next"), "a Next entry would keep the zoom after release");
        assertTrue(hold.get("CancelOnOtherClick").getAsBoolean());
        assertEquals("Ignore", hold.get("OnItemChangeBehavior").getAsString());

        JsonObject zoom = hold.getAsJsonObject("Effects").getAsJsonObject("Zoom");
        assertEquals(4f, zoom.get("MagnificationMultiplier").getAsFloat());
        assertEquals(0.35f, zoom.get("MouseSensitivityMultiplier").getAsFloat());
        assertEquals("FirstPerson", zoom.get("ForcePerspective").getAsString());
        assertFalse(zoom.has("OverlayTexture"));
    }

    @Test
    void theJarsZoomMatchesTheDefaults() throws Exception {
        try (InputStream in = ZoomSpecTest.class.getResourceAsStream("/Server/Item/RootInteractions/FlexiiQOL/Root_FlexiiQOL_Zoom_Base.json")) {
            assertNotNull(in);
            assertEquals(JsonParser.parseString(ZoomSpec.defaults().toRootJson()),
                    JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        }
    }

    @Test
    void settingsChangeOneValueEach() {
        ZoomSpec spec = ZoomSpec.defaults()
                .with("mag", "6").with("sens", "0.25").with("in", "0.1").with("out", "0.3")
                .with("persp", "none").with("overlay", "spyglass").with("click", "block").with("dof", "on").with("item", "cancel");
        assertEquals(new ZoomSpec(6f, 0.25f, 0.1f, 0.3f, "None", "ScreenEffects/Spyglass.png", false, true, "Cancel"), spec);

        JsonObject hold = JsonParser.parseString(spec.toRootJson()).getAsJsonObject().getAsJsonArray("Interactions").get(0).getAsJsonObject();
        assertFalse(hold.get("CancelOnOtherClick").getAsBoolean());
        JsonObject zoom = hold.getAsJsonObject("Effects").getAsJsonObject("Zoom");
        assertEquals("ScreenEffects/Spyglass.png", zoom.get("OverlayTexture").getAsString());
        assertTrue(zoom.get("DepthOfField").getAsBoolean());
    }

    @Test
    void rejectsValuesTheClientCannotUse() {
        ZoomSpec spec = ZoomSpec.defaults();
        assertThrows(IllegalArgumentException.class, () -> spec.with("mag", "1"));
        assertThrows(IllegalArgumentException.class, () -> spec.with("mag", "four"));
        assertThrows(IllegalArgumentException.class, () -> spec.with("sens", "0"));
        assertThrows(IllegalArgumentException.class, () -> spec.with("in", "1.5"));
        assertThrows(IllegalArgumentException.class, () -> spec.with("overlay", "../../x"));
        assertThrows(IllegalArgumentException.class, () -> spec.with("persp", "side"));
        assertThrows(IllegalArgumentException.class, () -> spec.with("item", "drop"));
        assertThrows(IllegalArgumentException.class, () -> spec.with("fov", "90"));
    }
}
