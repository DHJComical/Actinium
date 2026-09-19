package com.gtnewhorizons.angelica.glsm.recording.commands;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AttribLayoutKeyTest {

    @Test
    void singleFloat3AttribHasZeroOffsetAndTightStride() {
        final AttribLayoutKey key = new AttribLayoutKey(
            new int[]{0}, new int[]{3}, new int[]{GL11.GL_FLOAT}, new boolean[]{false});

        assertEquals(0, key.offset(0));
        assertEquals(12, key.stride());
    }

    @Test
    void float3ThenUbyte4AreTightlyPacked() {
        final AttribLayoutKey key = new AttribLayoutKey(
            new int[]{0, 1}, new int[]{3, 4}, new int[]{GL11.GL_FLOAT, GL11.GL_UNSIGNED_BYTE}, new boolean[]{false, true});

        assertEquals(0, key.offset(0));
        assertEquals(12, key.offset(1));
        assertEquals(16, key.stride());
    }

    @Test
    void ubyteBeforeFloatAlignsTheFloatOffset() {
        // 1 byte at offset 0, then a float3 must align up to offset 4.
        final AttribLayoutKey key = new AttribLayoutKey(
            new int[]{0, 1}, new int[]{1, 3}, new int[]{GL11.GL_UNSIGNED_BYTE, GL11.GL_FLOAT}, new boolean[]{false, false});

        assertEquals(0, key.offset(0));
        assertEquals(4, key.offset(1));
        assertEquals(16, key.stride());
    }

    @Test
    void shortAlignmentAndStrideRoundToMaxAlign() {
        // ubyte at 0 (base 1), ushort2 aligns to 2 → offset 2, base 6; stride rounds to 2.
        final AttribLayoutKey key = new AttribLayoutKey(
            new int[]{0, 1}, new int[]{1, 2}, new int[]{GL11.GL_UNSIGNED_BYTE, GL11.GL_UNSIGNED_SHORT}, new boolean[]{false, false});

        assertEquals(0, key.offset(0));
        assertEquals(2, key.offset(1));
        assertEquals(6, key.stride());
    }

    @Test
    void equalArraysProduceEqualKeysAndHashes() {
        final AttribLayoutKey a = new AttribLayoutKey(
            new int[]{0, 2}, new int[]{3, 2}, new int[]{GL11.GL_FLOAT, GL11.GL_UNSIGNED_SHORT}, new boolean[]{false, true});
        final AttribLayoutKey b = new AttribLayoutKey(
            new int[]{0, 2}, new int[]{3, 2}, new int[]{GL11.GL_FLOAT, GL11.GL_UNSIGNED_SHORT}, new boolean[]{false, true});

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differingMembersBreakEquality() {
        final AttribLayoutKey base = new AttribLayoutKey(
            new int[]{0, 2}, new int[]{3, 2}, new int[]{GL11.GL_FLOAT, GL11.GL_UNSIGNED_SHORT}, new boolean[]{false, true});

        assertNotEquals(base, new AttribLayoutKey(
            new int[]{0, 3}, new int[]{3, 2}, new int[]{GL11.GL_FLOAT, GL11.GL_UNSIGNED_SHORT}, new boolean[]{false, true}));
        assertNotEquals(base, new AttribLayoutKey(
            new int[]{0, 2}, new int[]{4, 2}, new int[]{GL11.GL_FLOAT, GL11.GL_UNSIGNED_SHORT}, new boolean[]{false, true}));
        assertNotEquals(base, new AttribLayoutKey(
            new int[]{0, 2}, new int[]{3, 2}, new int[]{GL11.GL_FLOAT, GL11.GL_SHORT}, new boolean[]{false, true}));
        assertNotEquals(base, new AttribLayoutKey(
            new int[]{0, 2}, new int[]{3, 2}, new int[]{GL11.GL_FLOAT, GL11.GL_UNSIGNED_SHORT}, new boolean[]{false, false}));
    }
}
