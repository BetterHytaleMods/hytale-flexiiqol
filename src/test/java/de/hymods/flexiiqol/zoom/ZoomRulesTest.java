package de.hymods.flexiiqol.zoom;

import org.junit.jupiter.api.Test;

import static de.hymods.flexiiqol.zoom.ZoomRules.Hold.BLOCK;
import static de.hymods.flexiiqol.zoom.ZoomRules.Hold.OFF;
import static de.hymods.flexiiqol.zoom.ZoomRules.Hold.RUNE;
import static de.hymods.flexiiqol.zoom.ZoomRules.Hold.TAKEN;
import static de.hymods.flexiiqol.zoom.ZoomRules.Hold.ZOOM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ZoomRulesTest {

    @Test
    void zoomsUnlessTheGameNeedsR() {
        assertEquals(ZOOM, ZoomRules.decide(true, false, false, false));
        assertEquals(BLOCK, ZoomRules.decide(true, false, true, false));
        assertEquals(RUNE, ZoomRules.decide(true, false, false, true));
        assertEquals(TAKEN, ZoomRules.decide(true, true, false, false));
    }

    @Test
    void theStrongestReasonWins() {
        assertEquals(OFF, ZoomRules.decide(false, true, true, true));
        assertEquals(TAKEN, ZoomRules.decide(true, true, true, true));
        assertEquals(RUNE, ZoomRules.decide(true, false, true, true));
    }

    @Test
    void glideTrailsMoreTheHigherItIs() {
        assertNull(Glide.lerp(0f));
        assertEquals(0.5f, Glide.lerp(0.5f), 1e-6f);
        assertEquals(0.1f, Glide.lerp(0.9f), 1e-6f);
        assertEquals(0.05f, Glide.lerp(1.8f), 1e-6f);
        assertEquals(0.075f, Glide.lerp(1.2f), 1e-6f);
        assertEquals(0.05f, Glide.lerp(3f), 1e-6f);
    }
}
