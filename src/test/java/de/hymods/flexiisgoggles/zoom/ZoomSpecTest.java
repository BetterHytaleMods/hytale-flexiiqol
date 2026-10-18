package de.hymods.flexiisgoggles.zoom;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoomSpecTest {

    @Test
    void theRootHoldsTheZoomUntilReleaseAndHidesTheItem() {
        JsonObject root = JsonParser.parseString(new ZoomSpec(6, 0.25f, 0.15f, 0.25f).toRootJson()).getAsJsonObject();
        assertTrue(root.get("RequireNewClick").getAsBoolean());
        assertEquals(0, root.getAsJsonObject("Rules").getAsJsonArray("BlockedBy").size());

        JsonObject hold = root.getAsJsonArray("Interactions").get(0).getAsJsonObject();
        assertEquals("Charging", hold.get("Type").getAsString());
        assertTrue(hold.get("AllowIndefiniteHold").getAsBoolean());
        assertFalse(hold.has("Next"), "without Next, letting go ends the chain and the zoom");
        assertTrue(hold.get("CancelOnOtherClick").getAsBoolean());

        JsonObject effects = hold.getAsJsonObject("Effects");
        assertTrue(effects.get("HideFirstPersonHeldItem").getAsBoolean());
        JsonObject zoom = effects.getAsJsonObject("Zoom");
        assertEquals(6, zoom.get("MagnificationMultiplier").getAsInt());
        assertEquals(0.25f, zoom.get("MouseSensitivityMultiplier").getAsFloat());
        assertEquals("FirstPerson", zoom.get("ForcePerspective").getAsString());
        assertFalse(zoom.has("OverlayTexture"));
    }

    @Test
    void rejectsValuesTheClientCannotUse() {
        assertThrows(IllegalArgumentException.class, () -> new ZoomSpec(1, 0.35f, 0.15f, 0.25f));
        assertThrows(IllegalArgumentException.class, () -> new ZoomSpec(4, 0f, 0.15f, 0.25f));
        assertThrows(IllegalArgumentException.class, () -> new ZoomSpec(4, 0.35f, 1.5f, 0.25f));
    }

    @Test
    void zoomRootsAreNamedAfterTheirStep() {
        assertEquals("Root_FlexiisGoggles_Zoom_4x", ZoomAssets.root(4));
        assertTrue(ZoomBindings.isZoom(ZoomAssets.root(8)));
        assertFalse(ZoomBindings.isZoom("Root_Spyglass"));
    }
}
