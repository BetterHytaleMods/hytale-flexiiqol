package de.hymods.flexiisgoggles.goggles;

import de.hymods.flexiisgoggles.GogglesConfig;
import de.hymods.flexiisgoggles.settings.PlayerSettings.Choice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectiveTest {

    private static GogglesConfig config() {
        GogglesConfig config = new GogglesConfig();
        config.goggles.clear();
        return config;
    }

    @Test
    void withoutGogglesThereIsNothing() {
        GogglesConfig config = config();
        Effective.Ability ability = Effective.Ability.of(config, null);
        assertFalse(ability.any());
        Effective effective = Effective.of(config, Choice.NONE, ability);
        assertEquals(0f, effective.zoom());
        assertEquals(0, effective.nightVisionLevel());
    }

    @Test
    void untouchedPicksFollowTheGogglesFullStrength() {
        GogglesConfig config = config();
        Effective zoom = Effective.of(config, Choice.NONE, Effective.Ability.of(config, Goggles.ZOOM_3));
        assertEquals(5f, zoom.zoom());
        assertEquals(0, zoom.nightVisionLevel());
        assertEquals(5, zoom.glide());

        Effective night = Effective.of(config, Choice.NONE, Effective.Ability.of(config, Goggles.NIGHT_VISION_2));
        assertEquals(0f, night.zoom());
        assertTrue(night.nightVision());
        assertEquals(2, night.nightVisionLevel());
    }

    @Test
    void picksStayWithinTheGoggles() {
        GogglesConfig config = config();
        Choice choice = Choice.NONE.withZoom(12f).withBrightness(6);
        Effective ultra = Effective.of(config, choice.withZoom(3f), Effective.Ability.of(config, Goggles.ULTRA));
        assertEquals(3f, ultra.zoom());
        assertEquals(6, ultra.nightVisionLevel());

        Effective weak = Effective.of(config, choice, Effective.Ability.of(config, Goggles.ZOOM_2));
        assertEquals(3f, weak.zoom());
        Effective dim = Effective.of(config, choice, Effective.Ability.of(config, Goggles.NIGHT_VISION_3));
        assertEquals(3, dim.nightVisionLevel());
        Effective off = Effective.of(config, choice.withNightVision(false), Effective.Ability.of(config, Goggles.NIGHT_VISION_3));
        assertEquals(0, off.nightVisionLevel());
    }

    @Test
    void theConfigCanLowerGoggles() {
        GogglesConfig config = config();
        GogglesConfig.Limits limits = new GogglesConfig.Limits();
        limits.zoom = 8f;
        limits.nightVision = 4;
        config.goggles.put(Goggles.ULTRA.key(), limits);
        Effective ultra = Effective.of(config, Choice.NONE, Effective.Ability.of(config, Goggles.ULTRA));
        assertEquals(8f, ultra.zoom());
        assertEquals(4, ultra.nightVisionLevel());
    }

    @Test
    void withoutTheRequirementEveryoneGetsTheServersDefaults() {
        GogglesConfig config = config();
        config.requireGoggles = false;
        Effective.Ability ability = Effective.Ability.of(config, null);
        assertTrue(ability.any());
        Effective effective = Effective.of(config, Choice.NONE, ability);
        assertEquals(4f, effective.zoom());
        assertFalse(effective.nightVision(), "night vision starts off without goggles");
        assertEquals(3, effective.brightness());
        assertEquals(12f, Effective.of(config, Choice.NONE.withZoom(12f), ability).zoom());
    }

    @Test
    void featuresSwitchedOffByTheServerStayOff() {
        GogglesConfig config = config();
        config.zoom.enabled = false;
        config.nightVision.enabled = false;
        assertFalse(Effective.Ability.of(config, Goggles.ULTRA).any());
    }
}
