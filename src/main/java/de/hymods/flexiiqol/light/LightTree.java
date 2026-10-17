package de.hymods.flexiiqol.light;

/**
 * The light of one chunk section (32 × 32 × 32 blocks) as the server sends it in {@code SetChunk}.
 * <p>
 * Layout: one byte "has light" (0 means no light at all), then a tree of nodes. A node is a mask
 * byte followed by its 8 octants in order; for every set mask bit the octant is a nested node,
 * otherwise a 16-bit little-endian value that holds for the whole octant. The root node splits the
 * section into octants of 16³, four nested levels reach single blocks. A value packs four 4-bit
 * channels: red (bits 0–3), green (4–7), blue (8–11) and sky (12–15).
 * <p>
 * Fullbright raises red, green and blue to a minimum level and leaves sky alone: the client lights
 * a block with the brighter of block light and sun light, so block light at 15 is white at any time
 * of day, while sky light at 15 would follow the sun and go dark at night. The tree keeps its shape,
 * so the result has the same size.
 */
public final class LightTree {
    /** The root plus four levels down to single blocks. */
    private static final int MAX_DEPTH = 4;

    private LightTree() {
    }

    /**
     * A copy of {@code light} with red, green and blue at least {@code level}. Missing or empty light
     * becomes a uniform section.
     *
     * @throws IllegalArgumentException if the bytes are not a light tree
     */
    public static byte[] brighten(byte[] light, int level) {
        checkLevel(level);
        if (light == null || light.length == 0 || light[0] == 0) {
            return uniform(level);
        }
        byte[] out = light.clone();
        int end = walk(out, 1, level, 0);
        if (end != out.length) {
            throw new IllegalArgumentException("light tree ends at " + end + " of " + out.length + " bytes");
        }
        return out;
    }

    /** One value for the whole section: red, green and blue at {@code level}, no sky light. */
    public static byte[] uniform(int level) {
        checkLevel(level);
        int value = raise(0, level);
        byte[] out = new byte[2 + 8 * 2];
        out[0] = 1;
        out[1] = 0;
        for (int i = 0; i < 8; i++) {
            out[2 + 2 * i] = (byte) value;
            out[3 + 2 * i] = (byte) (value >> 8);
        }
        return out;
    }

    /** Raises red, green and blue of one packed value to at least {@code level}. */
    static int raise(int value, int level) {
        int red = Math.max(value & 0xF, level);
        int green = Math.max(value >> 4 & 0xF, level);
        int blue = Math.max(value >> 8 & 0xF, level);
        return value & 0xF000 | blue << 8 | green << 4 | red;
    }

    private static int walk(byte[] data, int offset, int level, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("light tree deeper than a section");
        }
        if (offset >= data.length) {
            throw new IllegalArgumentException("light tree cut off at " + offset);
        }
        int mask = data[offset] & 0xFF;
        offset++;
        for (int i = 0; i < 8; i++) {
            if ((mask >> i & 1) == 1) {
                offset = walk(data, offset, level, depth + 1);
            } else {
                if (offset + 1 >= data.length) {
                    throw new IllegalArgumentException("light tree cut off at " + offset);
                }
                int value = data[offset] & 0xFF | (data[offset + 1] & 0xFF) << 8;
                value = raise(value, level);
                data[offset] = (byte) value;
                data[offset + 1] = (byte) (value >> 8);
                offset += 2;
            }
        }
        return offset;
    }

    private static void checkLevel(int level) {
        if (level < 0 || level > 15) {
            throw new IllegalArgumentException("light level must be 0 to 15");
        }
    }
}
