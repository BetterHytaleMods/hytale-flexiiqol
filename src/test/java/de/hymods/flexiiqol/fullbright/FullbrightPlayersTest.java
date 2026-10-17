package de.hymods.flexiiqol.fullbright;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullbrightPlayersTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void togglesAndKeepsItAcrossARestart(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("fullbright.json");
        FullbrightPlayers players = new FullbrightPlayers(file);
        assertTrue(players.load(), "no file yet means nobody");
        assertTrue(players.toggle(A));
        players.set(B, true);
        assertFalse(players.toggle(B));
        players.save();

        FullbrightPlayers again = new FullbrightPlayers(file);
        assertTrue(again.load());
        assertTrue(again.isOn(A));
        assertFalse(again.isOn(B));
        assertEquals(1, again.size());
    }

    @Test
    void skipsBrokenEntriesAndReportsABrokenFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("fullbright.json");
        Files.writeString(file, "{\"players\": [\"" + A + "\", \"not a uuid\", 7, {}]}");
        FullbrightPlayers players = new FullbrightPlayers(file);
        assertTrue(players.load());
        assertTrue(players.isOn(A));
        assertEquals(1, players.size());

        Files.writeString(file, "{\"players\": [");
        assertFalse(players.load());
        assertEquals(0, players.size());
        players.set(B, true);
        players.save();
        assertTrue(Files.readString(file).contains(B.toString()), "the next save replaces a broken file");
    }
}
