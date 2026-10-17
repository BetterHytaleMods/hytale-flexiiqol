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
        assertEquals(4f, config.zoom.magnification);
        assertEquals(15, config.fullbright.level);
        String written = Files.readString(file);
        assertTrue(written.contains("\"zoom\": {"), written);
        assertTrue(written.contains("\"magnification\": 4.0"), written);
    }

    @Test
    void keepsDefaultsForMissingFieldsAndClampsTheRest(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{\"zoom\": {\"magnification\": 99, \"sensitivity\": -1, \"glide\": 3}, \"fullbright\": {\"level\": 20}}");
        QolConfig config = QolConfig.load(file);
        assertEquals(16f, config.zoom.magnification);
        assertEquals(0.05f, config.zoom.sensitivity);
        assertEquals(0.95f, config.zoom.glide);
        assertEquals(15, config.fullbright.level);
        assertEquals(0.15f, config.zoom.inLerp);
        assertTrue(Files.readString(file).contains("\"outLerp\": 0.25"), "missing fields are written back");
        assertEquals(16f, config.zoom.spec().magnification());
    }

    @Test
    void missingOrNullSectionsGetTheirDefaults(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{\"zoom\": null}");
        QolConfig config = QolConfig.load(file);
        assertTrue(config.zoom.enabled);
        assertTrue(config.fullbright.enabled);
        assertEquals(0.7f, config.zoom.glide);
    }
}
