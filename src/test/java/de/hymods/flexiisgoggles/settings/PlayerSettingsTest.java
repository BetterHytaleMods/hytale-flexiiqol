package de.hymods.flexiisgoggles.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSettingsTest {
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void picksSurviveSavingAndLoading(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("players.json");
        PlayerSettings settings = new PlayerSettings(file);
        settings.update(ALICE, choice -> choice.withZoom(1.5f).withGlide(0).withNightVision(false).withBrightness(2));
        settings.update(BOB, choice -> choice.withBrightness(5));
        settings.save();

        PlayerSettings again = new PlayerSettings(file);
        assertTrue(again.load());
        assertEquals(new PlayerSettings.Choice(1.5f, 0, false, 2), again.get(ALICE));
        assertEquals(new PlayerSettings.Choice(null, null, null, 5), again.get(BOB));
        assertEquals(2, again.size());
        assertEquals(PlayerSettings.Choice.NONE, again.get(UUID.randomUUID()));
    }

    @Test
    void valuesOutOfRangeAreClampedOrSnapped() {
        PlayerSettings.Choice choice = PlayerSettings.Choice.NONE.withZoom(20f).withGlide(-3).withBrightness(15);
        assertEquals(16f, choice.zoom());
        assertEquals(0, choice.glide());
        assertEquals(PlayerSettings.BRIGHTNESS_MAX, choice.brightness());
        assertNull(choice.nightVision());
        assertEquals(5f, PlayerSettings.Choice.NONE.withZoom(5.2f).zoom());
    }

    @Test
    void anEmptyChoiceIsDropped(@TempDir Path dir) {
        PlayerSettings settings = new PlayerSettings(dir.resolve("players.json"));
        settings.update(ALICE, choice -> choice.withZoom(3f));
        settings.update(ALICE, choice -> PlayerSettings.Choice.NONE);
        assertEquals(0, settings.size());
    }

    @Test
    void brokenEntriesAndValuesAreSkipped(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("players.json");
        Files.writeString(file, """
                {"players": {
                  "not-a-uuid": {"zoom": 3},
                  "%s": {"zoom": "six", "glide": 4.4, "nightVision": "yes", "brightness": 2},
                  "%s": 7
                }}""".formatted(ALICE, BOB));
        PlayerSettings settings = new PlayerSettings(file);
        assertTrue(settings.load());
        assertEquals(new PlayerSettings.Choice(null, 4, null, 2), settings.get(ALICE));
        assertEquals(PlayerSettings.Choice.NONE, settings.get(BOB));

        Files.writeString(file, "{ broken");
        assertFalse(new PlayerSettings(file).load());
        assertTrue(new PlayerSettings(dir.resolve("missing.json")).load());
    }
}
