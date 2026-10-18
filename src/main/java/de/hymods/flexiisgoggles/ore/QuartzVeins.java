package de.hymods.flexiisgoggles.ore;

import java.util.ArrayList;
import java.util.List;

/**
 * Where quartz goes into a chunk that was generated before the mod (plain numbers, testable without a
 * server): small veins scattered through the heights the world generator uses for quartz, fixed by the
 * seed and the chunk. Only stone turns into ore, so about as much quartz ends up in the chunk as the world
 * generator puts into new ones.
 */
public final class QuartzVeins {
    static final int BOTTOM = 30;
    static final int TOP = 105;
    /**
     * Vein starts per chunk: in ordinary stone this leaves about as much quartz as the world generator
     * puts into a new chunk (both about 140 blocks, measured 18.10.), less where there is little stone.
     */
    static final int VEINS = 80;
    private static final long SALT = 0x7f4a7c159e3779b9L;

    private QuartzVeins() {
    }

    /** The blocks of all veins of a chunk, as {x, y, z} inside the chunk; each vein two to five blocks in a row. */
    public static List<int[]> blocks(long seed, int chunkX, int chunkZ) {
        List<int[]> blocks = new ArrayList<>();
        long chunk = hash(seed ^ SALT, chunkX, chunkZ);
        for (int vein = 0; vein < VEINS; vein++) {
            long h = hash(chunk, vein, 0);
            int x = (int) (unit(hash(h, 1, 0)) * 32);
            int y = BOTTOM + (int) (unit(hash(h, 2, 0)) * (TOP - BOTTOM + 1));
            int z = (int) (unit(hash(h, 3, 0)) * 32);
            int size = 2 + (int) (unit(hash(h, 4, 0)) * 4);
            blocks.add(new int[]{x, y, z});
            for (int step = 1; step < size; step++) {
                switch ((int) (unit(hash(h, 10 + step, 1)) * 6)) {
                    case 0 -> x = Math.min(31, x + 1);
                    case 1 -> x = Math.max(0, x - 1);
                    case 2 -> y = Math.min(TOP, y + 1);
                    case 3 -> y = Math.max(BOTTOM, y - 1);
                    case 4 -> z = Math.min(31, z + 1);
                    default -> z = Math.max(0, z - 1);
                }
                blocks.add(new int[]{x, y, z});
            }
        }
        return blocks;
    }

    /** A fixed pseudo-random number for a place. */
    private static long hash(long seed, int a, int b) {
        long h = seed + a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }
}
