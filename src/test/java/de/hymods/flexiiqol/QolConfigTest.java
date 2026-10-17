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
        assertEquals(4f, config.zoomMagnification);
        assertEquals(15, config.fullbrightLevel);
        assertTrue(Files.readString(file).contains("\"zoomMagnification\": 4.0"));
    }

    @Test
    void keepsDefaultsForMissingFieldsAndClampsTheRest(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{\"zoomMagnification\": 99, \"zoomSensitivity\": -1, \"fullbrightLevel\": 20}");
        QolConfig config = QolConfig.load(file);
        assertEquals(16f, config.zoomMagnification);
        assertEquals(0.05f, config.zoomSensitivity);
        assertEquals(15, config.fullbrightLevel);
        assertEquals(0.15f, config.zoomInLerp);
        assertTrue(Files.readString(file).contains("\"zoomOutLerp\": 0.25"), "missing fields are written back");
        assertEquals(16f, config.zoomSpec().magnification());
    }
}
