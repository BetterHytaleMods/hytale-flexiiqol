package de.hymods.flexiiqol.light;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LightTreeTest {

    @Test
    void uniformSectionHasBlockLightAndNoSky() {
        byte[] uniform = LightTree.uniform(15);
        assertEquals(18, uniform.length);
        assertEquals(1, uniform[0]);
        assertEquals(0, uniform[1]);
        for (int i = 0; i < 8; i++) {
            assertEquals(0x0FFF, value(uniform, 2 + 2 * i));
        }
    }

    @Test
    void skyOnlySectionKeepsItsSkyAndGetsBlockLight() {
        byte[] sky = new Tree().node(0, 0xF000, 0xF000, 0xF000, 0xF000, 0xF000, 0xF000, 0xF000, 0xF000).bytes();
        byte[] bright = LightTree.brighten(sky, 15);
        assertEquals(sky.length, bright.length);
        for (int i = 0; i < 8; i++) {
            assertEquals(0xFFFF, value(bright, 2 + 2 * i));
        }
    }

    @Test
    void nestedTreeKeepsItsShapeAndRaisesEveryValue() {
        // Root: octant 0 is a nested node, the other seven are plain values.
        Tree tree = new Tree().mask(0b0000_0001)
                .node(0, 0x0000, 0x0123, 0x0FFF, 0xA000, 0x0F00, 0x00F0, 0x000F, 0x5555)
                .values(0x0000, 0x7000, 0x0C0C, 0x0000, 0x0000, 0x0000, 0x0000);
        byte[] light = tree.bytes();
        byte[] bright = LightTree.brighten(light, 12);

        assertEquals(light.length, bright.length);
        int[] expectedNested = {0x0CCC, 0x0CCC, 0x0FFF, 0xACCC, 0x0FCC, 0x0CFC, 0x0CCF, 0x5CCC};
        for (int i = 0; i < 8; i++) {
            assertEquals(expectedNested[i], value(bright, 3 + 2 * i), "nested value " + i);
        }
        int[] expectedRoot = {0x0CCC, 0x7CCC, 0x0CCC, 0x0CCC, 0x0CCC, 0x0CCC, 0x0CCC};
        for (int i = 0; i < 7; i++) {
            assertEquals(expectedRoot[i], value(bright, 19 + 2 * i), "root value " + (i + 1));
        }
    }

    @Test
    void leavesTheInputAlone() {
        byte[] light = new Tree().node(0, 0, 0, 0, 0, 0, 0, 0, 0).bytes();
        byte[] copy = light.clone();
        LightTree.brighten(light, 15);
        assertArrayEquals(copy, light);
    }

    @Test
    void levelZeroChangesNothing() {
        byte[] light = new Tree().node(0, 0x0123, 0xF000, 0, 0, 0, 0, 0, 0x0FFF).bytes();
        assertArrayEquals(light, LightTree.brighten(light, 0));
    }

    @Test
    void missingOrEmptyLightBecomesUniform() {
        assertArrayEquals(LightTree.uniform(13), LightTree.brighten(null, 13));
        assertArrayEquals(LightTree.uniform(13), LightTree.brighten(new byte[]{0}, 13));
        assertArrayEquals(LightTree.uniform(13), LightTree.brighten(new byte[0], 13));
    }

    @Test
    void rejectsBrokenTrees() {
        byte[] whole = new Tree().node(0, 1, 2, 3, 4, 5, 6, 7, 8).bytes();
        byte[] cut = java.util.Arrays.copyOf(whole, whole.length - 1);
        assertThrows(IllegalArgumentException.class, () -> LightTree.brighten(cut, 15));
        byte[] longer = java.util.Arrays.copyOf(whole, whole.length + 2);
        assertThrows(IllegalArgumentException.class, () -> LightTree.brighten(longer, 15));
        // Every node says "nested" for its first octant: deeper than a section can be.
        ByteArrayOutputStream deep = new ByteArrayOutputStream();
        deep.write(1);
        for (int depth = 0; depth < 7; depth++) {
            deep.write(0b0000_0001);
        }
        assertThrows(IllegalArgumentException.class, () -> LightTree.brighten(deep.toByteArray(), 15));
        assertThrows(IllegalArgumentException.class, () -> LightTree.brighten(whole, 16));
    }

    @Test
    void raiseKeepsSkyAndBrighterChannels() {
        assertEquals(0xFAAA, LightTree.raise(0xF000, 10));
        assertEquals(0x0FFF, LightTree.raise(0x0FFF, 10));
        assertEquals(0x3ABA, LightTree.raise(0x3AB1, 10));
    }

    private static int value(byte[] data, int offset) {
        return data[offset] & 0xFF | (data[offset + 1] & 0xFF) << 8;
    }

    /** Writes light trees in the packet layout, starting with "has light". */
    private static final class Tree {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Tree() {
            this.out.write(1);
        }

        Tree mask(int mask) {
            this.out.write(mask);
            return this;
        }

        /** A node without nested octants. */
        Tree node(int mask, int... values) {
            this.out.write(mask);
            return this.values(values);
        }

        Tree values(int... values) {
            for (int value : values) {
                this.out.write(value & 0xFF);
                this.out.write(value >> 8 & 0xFF);
            }
            return this;
        }

        byte[] bytes() {
            return this.out.toByteArray();
        }
    }
}
