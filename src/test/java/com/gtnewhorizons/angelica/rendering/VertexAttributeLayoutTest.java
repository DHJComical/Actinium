package com.gtnewhorizons.angelica.rendering;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormatElement.Usage;

import net.coderbot.iris.gl.shader.ProgramCreator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Make sure we don't ever have collisions again, that was annoying.
 *
 * <p>Locks the fixed-function vertex-attribute location table shared by GTNHLib's vertex formats, the
 * glsm FFP/compat shader generators and the Iris-style reserved attributes: no two distinct
 * (usage, index) pairs — nor any reserved shader attribute — may share a generic attribute location
 * within {@code [0, 15]}. Issue #175 was a drop of the same table's consumers: the vanilla
 * BufferBuilder path resolved UV elements for texture units 2/3 to {@code -1} (only units 0/1 had
 * slots), so their attributes were never written, the FFP shader fell back to the per-draw texcoord
 * constant and third-party map terrain collapsed into flat colour squares. The per-unit lookup below
 * is what that path now resolves through.</p>
 */
class VertexAttributeLayoutTest {

    private static final int MAX_VERTEX_ATTRIBS = 16;

    private final Map<Integer, String> claimed = new HashMap<>();

    private void claim(int location, String owner) {
        assertTrue(
            location >= 0 && location < MAX_VERTEX_ATTRIBS,
            owner + " uses location " + location + ", outside 0.." + (MAX_VERTEX_ATTRIBS - 1));
        final String previous = claimed.put(location, owner);
        assertNull(previous, "location " + location + " claimed by both " + previous + " and " + owner);
    }

    /**
     * The element indices each usage supports. The UV usages are indexed by legacy texture unit:
     * {@code PRIMARY_UV} is unit 0, {@code SECONDARY_UV} feeds units 1..3. {@code PADDING} owns -1 and
     * {@code GENERIC} binds to a caller-chosen slot, so neither takes part in the fixed layout.
     */
    private static int[] supportedIndices(Usage usage) {
        return switch (usage) {
            case SECONDARY_UV -> new int[] { 1, 2, 3 };
            case PADDING, GENERIC -> new int[0];
            default -> new int[] { 0 };
        };
    }

    /**
     * The authoritative, human-readable location table — "who owns slot N". Read this first when a
     * collision resurfaces; every slot number is asserted literally so any drift is obvious at a glance.
     */
    @Test
    void finalAttributeLocationTableIsPinned() {
        assertEquals(0, Usage.POSITION.getAttributeLocation(0), "slot 0 = POSITION");
        assertEquals(1, Usage.COLOR.getAttributeLocation(0), "slot 1 = COLOR");
        assertEquals(2, Usage.PRIMARY_UV.getAttributeLocation(0), "slot 2 = PRIMARY_UV (texture unit 0)");
        assertEquals(3, Usage.SECONDARY_UV.getAttributeLocation(1), "slot 3 = SECONDARY_UV (texture unit 1 / lightmap)");
        assertEquals(4, Usage.NORMAL.getAttributeLocation(0), "slot 4 = NORMAL");
        assertEquals(5, Usage.SECONDARY_UV.getAttributeLocation(2), "slot 5 = SECONDARY_UV (texture unit 2)");
        assertEquals(6, Usage.SECONDARY_UV.getAttributeLocation(3), "slot 6 = SECONDARY_UV (texture unit 3)");
        assertEquals(11, ProgramCreator.MC_ENTITY, "slot 11 = mc_Entity");
        assertEquals(12, ProgramCreator.MC_MID_TEX_COORD, "slot 12 = mc_midTexCoord");
        assertEquals(13, ProgramCreator.AT_TANGENT, "slot 13 = at_tangent");
        assertEquals(14, ProgramCreator.AT_MIDBLOCK, "slot 14 = at_midBlock");
        assertEquals(-1, Usage.PADDING.getAttributeLocation(), "PADDING owns no attribute slot");
        assertEquals(-1, Usage.GENERIC.getAttributeLocation(), "GENERIC binds a caller-chosen slot (no fixed location)");
    }

    @Test
    void reservedAttributeLocationsDoNotOverlap() {
        for (Usage usage : Usage.values()) {
            for (int index : supportedIndices(usage)) {
                claim(usage.getAttributeLocation(index), usage.name() + "[index " + index + "]");
            }
        }

        claim(ProgramCreator.MC_ENTITY, "ProgramCreator.MC_ENTITY");
        claim(ProgramCreator.MC_MID_TEX_COORD, "ProgramCreator.MC_MID_TEX_COORD");
        claim(ProgramCreator.AT_TANGENT, "ProgramCreator.AT_TANGENT");
        claim(ProgramCreator.AT_MIDBLOCK, "ProgramCreator.AT_MIDBLOCK");
    }

    /**
     * The shared unit-to-location table ({@link Usage#uvAttributeLocation}) must agree with the
     * per-usage lookup the shader generators use, so every consumer maps a texture unit to the same slot.
     */
    @Test
    void uvAttributeLocationMatchesUsageLookup() {
        assertEquals(Usage.PRIMARY_UV.getAttributeLocation(0), Usage.uvAttributeLocation(0));
        for (int unit = 1; unit <= 3; unit++) {
            assertEquals(Usage.SECONDARY_UV.getAttributeLocation(unit), Usage.uvAttributeLocation(unit));
        }
    }

    @Test
    void reportsRemainingFreeLocations() {
        reservedAttributeLocationsDoNotOverlap();
        final TreeSet<Integer> free = new TreeSet<>();
        for (int i = 0; i < MAX_VERTEX_ATTRIBS; i++) {
            if (!claimed.containsKey(i)) free.add(i);
        }
        System.out.println("free vertex attribute locations: " + free);
    }
}
