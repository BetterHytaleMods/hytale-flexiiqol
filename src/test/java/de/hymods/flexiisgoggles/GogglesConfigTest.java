package de.hymods.flexiisgoggles;

import de.hymods.flexiisgoggles.goggles.Goggles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GogglesConfigTest {

    @Test
    void writesTheDefaultsWhenThereIsNoFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.json");
        GogglesConfig config = GogglesConfig.load(file);
        assertTrue(config.requireGoggles);
        assertEquals(4f, config.zoom.magnification);
        assertEquals(0.5f, config.zoom.glide);
        assertEquals(3, config.nightVision.level);
        assertEquals(16f, config.zoomCap(Goggles.ULTRA));
        assertEquals(5, config.nightVisionCap(Goggles.NIGHT_VISION_4));
        String written = Files.readString(file);
        assertTrue(written.contains("\"requireGoggles\": true"), written);
        assertTrue(written.contains("\"Zoom_1\": {"), written);
        assertTrue(written.contains("\"zoom\": 1.5"), written);
    }

    @Test
    void gogglesCanOnlyBeLowered(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, """
                {"goggles": {
                  "Ultra": {"zoom": 8, "nightVision": 9},
                  "Zoom_2": {"zoom": 20},
                  "Zoom_3": {"zoom": 3.6, "nightVision": 4},
                  "NightVision_1": {"nightVision": 0},
                  "Unknown": {"zoom": 4}
                }}""");
        GogglesConfig config = GogglesConfig.load(file);
        assertEquals(8f, config.zoomCap(Goggles.ULTRA));
        assertEquals(6, config.nightVisionCap(Goggles.ULTRA));
        assertEquals(3f, config.zoomCap(Goggles.ZOOM_2));
        assertEquals(3f, config.zoomCap(Goggles.ZOOM_3));
        assertEquals(0, config.nightVisionCap(Goggles.ZOOM_3));
        assertNull(config.goggles.get("Zoom_3").nightVision);
        assertEquals(1, config.nightVisionCap(Goggles.NIGHT_VISION_1));
        assertEquals(Goggles.values().length, config.goggles.size());
    }

    @Test
    void keepsDefaultsForMissingFieldsAndClampsTheRest(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{\"zoom\": {\"magnification\": 99, \"sensitivity\": -1, \"glide\": 3}, \"nightVision\": {\"level\": 20}}");
        GogglesConfig config = GogglesConfig.load(file);
        assertEquals(16f, config.zoom.magnification);
        assertEquals(0.05f, config.zoom.sensitivity);
        assertEquals(1.8f, config.zoom.glide);
        assertEquals(6, config.nightVision.level);
        assertEquals(0.15f, config.zoom.inLerp);
        assertTrue(Files.readString(file).contains("\"outLerp\": 0.25"), "missing fields are written back");
    }

    @Test
    void glideComesInTenths(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{\"zoom\": {\"glide\": 0.36}}");
        assertEquals(0.4f, GogglesConfig.load(file).zoom.glide);
    }

    @Test
    void theMouseSlowsDownWithTheZoom() {
        GogglesConfig.Zoom zoom = new GogglesConfig().zoom;
        assertEquals(0.35f, zoom.spec(4f).sensitivity(), 1e-6f);
        assertEquals(0.35f * 4f / 6f, zoom.spec(6f).sensitivity(), 1e-6f);
        assertEquals(0.35f * 4f / 1.5f, zoom.spec(1.5f).sensitivity(), 1e-6f);
        assertEquals(0.35f * 4f / 12f, zoom.spec(12f).sensitivity(), 1e-6f);
    }

    @Test
    void missingOrNullSectionsGetTheirDefaults(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{\"zoom\": null, \"goggles\": null}");
        GogglesConfig config = GogglesConfig.load(file);
        assertTrue(config.zoom.enabled);
        assertTrue(config.nightVision.enabled);
        assertEquals(0.5f, config.zoom.glide);
        assertEquals(Goggles.values().length, config.goggles.size());
    }
}
