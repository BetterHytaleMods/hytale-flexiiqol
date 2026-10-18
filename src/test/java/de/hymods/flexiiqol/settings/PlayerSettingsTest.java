package de.hymods.flexiiqol.settings;

import de.hymods.flexiiqol.QolConfig;
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
    void untouchedSettingsFollowTheConfig() {
        QolConfig config = new QolConfig();
        config.zoom.magnification = 5;
        config.zoom.glide = 0.3f;
        config.fullbright.level = 4;
        PlayerSettings.Values values = PlayerSettings.Values.of(config, PlayerSettings.Choice.NONE);
        assertEquals(new PlayerSettings.Values(5, 3, false, 4), values);
        assertEquals(0.3f, values.glideAmount(), 1e-6f);

        PlayerSettings.Values picked = PlayerSettings.Values.of(config, PlayerSettings.Choice.NONE.withZoom(7).withFullbright(true));
        assertEquals(new PlayerSettings.Values(7, 3, true, 4), picked);
    }

    @Test
    void picksSurviveSavingAndLoading(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("players.json");
        PlayerSettings settings = new PlayerSettings(file);
        settings.update(ALICE, choice -> choice.withZoom(6).withGlide(0).withFullbright(true).withBrightness(2));
        settings.update(BOB, choice -> choice.withBrightness(5));
        settings.save();

        PlayerSettings again = new PlayerSettings(file);
        assertTrue(again.load());
        assertEquals(new PlayerSettings.Choice(6, 0, true, 2), again.get(ALICE));
        assertEquals(new PlayerSettings.Choice(null, null, null, 5), again.get(BOB));
        assertEquals(2, again.size());
        assertEquals(PlayerSettings.Choice.NONE, again.get(UUID.randomUUID()));
    }

    @Test
    void valuesOutOfRangeAreClamped() {
        PlayerSettings.Choice choice = PlayerSettings.Choice.NONE.withZoom(20).withGlide(-3).withBrightness(15);
        assertEquals(PlayerSettings.ZOOM_MAX, choice.zoom());
        assertEquals(0, choice.glide());
        assertEquals(PlayerSettings.BRIGHTNESS_MAX, choice.brightness());
        assertNull(choice.fullbright());
    }

    @Test
    void anEmptyChoiceIsDropped(@TempDir Path dir) throws Exception {
        PlayerSettings settings = new PlayerSettings(dir.resolve("players.json"));
        settings.update(ALICE, choice -> choice.withZoom(3));
        settings.update(ALICE, choice -> PlayerSettings.Choice.NONE);
        assertEquals(0, settings.size());
    }

    @Test
    void brokenEntriesAndValuesAreSkipped(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("players.json");
        Files.writeString(file, """
                {"players": {
                  "not-a-uuid": {"zoom": 3},
                  "%s": {"zoom": "six", "glide": 4.4, "fullbright": "yes", "brightness": 2},
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
