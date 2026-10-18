package de.hymods.flexiisgoggles.goggles;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GogglesTest {

    @Test
    void gogglesAndTheirHelmsAreRecognised() {
        assertEquals(Goggles.ZOOM_3, Goggles.byItemId("Flexii_Goggles_Zoom_3"));
        assertEquals(Goggles.ZOOM_3, Goggles.byItemId("Flexii_Helm_Scout_3"));
        assertEquals(Goggles.NIGHT_VISION_2, Goggles.byItemId("flexii_helm_nightwatch_2"));
        assertEquals(Goggles.ULTRA, Goggles.byItemId("Flexii_Helm_Overseer"));
        assertNull(Goggles.byItemId("Armor_Iron_Head"));
        assertNull(Goggles.byItemId(null));
    }

    @Test
    void tiersGrowStrongerAndUltraHasTheMost() {
        assertEquals(1.5f, Goggles.ZOOM_1.maxZoom());
        assertEquals(1, Goggles.NIGHT_VISION_1.maxNightVision());
        for (Goggles goggles : Goggles.values()) {
            assertTrue(goggles.maxZoom() <= Goggles.ULTRA.maxZoom());
            assertTrue(goggles.maxNightVision() <= Goggles.ULTRA.maxNightVision());
        }
        assertEquals(0, Goggles.ZOOM_4.maxNightVision());
        assertEquals(0f, Goggles.NIGHT_VISION_4.maxZoom());
        assertEquals(5, Goggles.ULTRA.tier(), "the ultragoggles need the bench's fifth tier");
    }

    @Test
    void onlyTheStrongestNightVisionShowsLumenite() {
        for (Goggles goggles : Goggles.values()) {
            assertEquals(goggles == Goggles.NIGHT_VISION_4 || goggles == Goggles.ULTRA, goggles.revealsLumenite(), goggles.name());
        }
    }

    @Test
    void everyItemIdIsUnique() {
        Set<String> ids = new HashSet<>();
        for (Goggles goggles : Goggles.values()) {
            assertTrue(ids.add(goggles.itemId()));
            assertTrue(ids.add(goggles.helmId()));
        }
        assertEquals(Goggles.NIGHT_VISION_4, Goggles.byKey("nightvision_4"));
    }
}
