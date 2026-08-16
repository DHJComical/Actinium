package com.dhj.actinium.compat.cavebiomes;

/**
 * Pure section-index arithmetic for worlds with an extended height range.
 *
 * <p>Issue #43: CaveBiomesAPI (dependency of Caves Not Cliffs [Backported]) reshapes
 * {@code Chunk.storageArrays} so that storage slot {@code i} holds world Y
 * {@code i * 16 + minY} instead of the vanilla {@code i * 16}. Vanilla-style
 * render paths index storage arrays with {@code sectionY} ({@code worldY >> 4}),
 * which lands four sections short in the API's default {@code -64..320} range and
 * causes the underground content to be meshed at surface positions. Render paths
 * must translate the semantic section Y to the API's storage index before reading
 * {@code Chunk.storageArrays}.</p>
 *
 * <p>This class deliberately has no dependency on {@code Loader}, CaveBiomesAPI or
 * any Minecraft class, so the mapping can be unit-tested directly and used by
 * hot paths without triggering Forge initialization.</p>
 */
final class CaveBiomesHeightMapping {
    private CaveBiomesHeightMapping() {
    }

    /**
     * The minimum section used by the API ({@code minY / 16}, {@code -4} for the
     * default {@code -64..320} range). When the range is vanilla ({@code minSection == 0})
     * the mapping is the identity, matching the unmodified layout.
     */
    static int minSection(int minY) {
        return minY / 16;
    }

    /**
     * Translates a semantic section Y ({@code worldY >> 4}, the coordinate the
     * renderer still works in) into an index into {@code Chunk.storageArrays}.
     *
     * @param minSection the API's minimum section ({@code -4} for {@code -64..320},
     *                   {@code 0} for the vanilla range); equality with {@code 0}
     *                   selects the identity mapping
     * @param sectionY   semantic section Y (may be negative for extended worlds)
     * @return the storage array index; the caller is responsible for clamping it to
     *         the array length returned by {@code Chunk#getBlockStorageArray()}
     */
    static int storageSectionIndex(int minSection, int sectionY) {
        return minSection == 0 ? sectionY : sectionY - minSection;
    }
}