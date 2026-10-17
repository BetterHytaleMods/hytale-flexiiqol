package de.hymods.flexiiqol.zoom;

import org.junit.jupiter.api.Test;

import static de.hymods.flexiiqol.zoom.ZoomRules.Hold.BLOCK;
import static de.hymods.flexiiqol.zoom.ZoomRules.Hold.OFF;
import static de.hymods.flexiiqol.zoom.ZoomRules.Hold.RUNE;
import static de.hymods.flexiiqol.zoom.ZoomRules.Hold.TAKEN;
import static de.hymods.flexiiqol.zoom.ZoomRules.Hold.ZOOM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertNull(Glide.Setting.of(0f, false));
        assertEquals(0.3f, Glide.Setting.of(0.7f, false).lerp(), 1e-6f);
        assertEquals(0.05f, Glide.Setting.of(2f, false).lerp(), 1e-6f);
        assertEquals(0.7f, Glide.Setting.of(0.7f, true).glide(), 1e-6f);
        assertThrows(IllegalArgumentException.class, () -> new Glide.Setting(1f, false));
    }
}
