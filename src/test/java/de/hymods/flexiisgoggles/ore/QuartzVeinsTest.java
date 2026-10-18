package de.hymods.flexiisgoggles.ore;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuartzVeinsTest {
    private static final long SEED = 1790940680530L;

    @Test
    void veinsStayInTheChunkAndInTheQuartzHeights() {
        for (int x = -5; x < 5; x++) {
            for (int z = -5; z < 5; z++) {
                List<int[]> blocks = QuartzVeins.blocks(SEED, x, z);
                assertTrue(blocks.size() >= 2 * QuartzVeins.VEINS && blocks.size() <= 5 * QuartzVeins.VEINS, "blocks: " + blocks.size());
                for (int[] b : blocks) {
                    assertTrue(b[0] >= 0 && b[0] < 32 && b[2] >= 0 && b[2] < 32, Arrays.toString(b));
                    assertTrue(b[1] >= QuartzVeins.BOTTOM && b[1] <= QuartzVeins.TOP, Arrays.toString(b));
                }
            }
        }
    }

    @Test
    void theSameChunkAlwaysGetsTheSameVeins() {
        assertEquals(Arrays.deepToString(QuartzVeins.blocks(SEED, 3, -7).toArray()), Arrays.deepToString(QuartzVeins.blocks(SEED, 3, -7).toArray()));
        assertNotEquals(Arrays.deepToString(QuartzVeins.blocks(SEED, 3, -7).toArray()), Arrays.deepToString(QuartzVeins.blocks(SEED, 4, -7).toArray()));
    }

    @Test
    void aboutAsMuchAsANewChunk() {
        int total = 0, chunks = 0;
        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 20; z++) {
                total += QuartzVeins.blocks(SEED, x, z).size();
                chunks++;
            }
        }
        double perChunk = total / (double) chunks;
        assertEquals(280, perChunk, 20, "vein blocks per chunk; in ordinary stone about half of them land in stone");
    }
}
