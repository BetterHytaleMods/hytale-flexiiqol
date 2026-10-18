package de.hymods.flexiiqol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QolConfigTest {

    @Test
    void writesTheDefaultsWhenThereIsNoFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.json");
        QolConfig config = QolConfig.load(file);
        assertEquals(4, config.zoom.magnification);
        assertEquals(0.5f, config.zoom.glide);
        assertEquals(3, config.fullbright.level);
        String written = Files.readString(file);
        assertTrue(written.contains("\"zoom\": {"), written);
        assertTrue(written.contains("\"magnification\": 4,"), written);
    }

    @Test
    void keepsDefaultsForMissingFieldsAndClampsTheRest(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{\"zoom\": {\"magnification\": 99, \"sensitivity\": -1, \"glide\": 3}, \"fullbright\": {\"level\": 20}}");
        QolConfig config = QolConfig.load(file);
        assertEquals(12, config.zoom.magnification);
        assertEquals(0.05f, config.zoom.sensitivity);
        assertEquals(1.8f, config.zoom.glide);
        assertEquals(6, config.fullbright.level);
        assertEquals(0.15f, config.zoom.inLerp);
        assertTrue(Files.readString(file).contains("\"outLerp\": 0.25"), "missing fields are written back");
    }

    @Test
    void glideComesInTenths(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{\"zoom\": {\"glide\": 0.36}}");
        assertEquals(0.4f, QolConfig.load(file).zoom.glide);
    }

    @Test
    void theMouseSlowsDownWithTheZoom() {
        QolConfig.Zoom zoom = new QolConfig().zoom;
        assertEquals(0.35f, zoom.spec(4).sensitivity(), 1e-6f);
        assertEquals(0.35f * 4f / 6f, zoom.spec(6).sensitivity(), 1e-6f);
        assertEquals(0.7f, zoom.spec(2).sensitivity(), 1e-6f);
        assertEquals(12, zoom.spec(12).magnification());
        assertEquals(0.35f * 4f / 12f, zoom.spec(12).sensitivity(), 1e-6f);
    }

    @Test
    void missingOrNullSectionsGetTheirDefaults(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{\"zoom\": null}");
        QolConfig config = QolConfig.load(file);
        assertTrue(config.zoom.enabled);
        assertTrue(config.fullbright.enabled);
        assertEquals(0.5f, config.zoom.glide);
    }
}
