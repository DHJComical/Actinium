package org.embeddedt.embeddium.impl.render.chunk;

import org.jetbrains.annotations.Nullable;

/**
 * Packs the per-section state consumed by visibility traversal and render-list
 * collection into one {@code long}.
 *
 * <p>The lattice traversal and render-list collector copy the visibility graph,
 * render-presence flags, pending update and build state as one value instead of
 * loading them from several section fields. {@code RenderSection} remains the
 * authoritative owner, while {@code SectionLattice} mirrors this word for
 * traversal.
 *
 * <p>Bit layout:
 * <pre>
 * 63                 53 52 51       49 48      46 45                0
 * +--------------------+--+-----------+----------+-------------------+
 * |       spare        |IF| pending   | visuals  | visibility graph  |
 * +--------------------+--+-----------+----------+-------------------+
 * </pre>
 * Bits {@code 0..45} contain the visibility encoding, bits {@code 46..48}
 * contain visual flags, bits {@code 49..51} contain the pending update type,
 * and bit {@code 52} records whether a build is in flight.
 *
 * <p>The visibility graph must be masked with {@link #VISIBILITY_MASK} before
 * it is passed to {@code VisibilityEncoding#getConnections(long)}, because
 * that method folds high bits into its direction result.
 */
public final class PackedSectionMetadata {
    private PackedSectionMetadata() {
    }

    // VisibilityEncoding uses six entries in each byte row; the final row has
    // two padding bits, so the graph occupies 6 * 8 - 2 bits.
    private static final int VISIBILITY_BITS = 46;
    public static final long VISIBILITY_MASK = (1L << VISIBILITY_BITS) - 1;

    // The three visual-presence flags occupy bits 46..48.
    private static final int VISUALS_FLAGS_SHIFT = 46;
    private static final long VISUALS_FLAGS_MASK = 0b111L;

    /** Metadata fields that can change the result of a lattice search. */
    public static final long GRAPH_INPUT_MASK = VISIBILITY_MASK | (VISUALS_FLAGS_MASK << VISUALS_FLAGS_SHIFT);

    // Zero means no pending update; otherwise the enum ordinal is stored + 1.
    private static final int PENDING_UPDATE_SHIFT = 49;
    private static final long PENDING_UPDATE_MASK = 0b111L;

    // Bit 52 records that a cancellation token is attached to a build.
    private static final int BUILD_IN_FLIGHT_BIT = 52;
    private static final long BUILD_IN_FLIGHT_FLAG = 1L << BUILD_IN_FLIGHT_BIT;

    /** Returns only the visibility graph portion of the packed metadata. */
    public static long getVisibilityData(long packed) {
        return packed & VISIBILITY_MASK;
    }

    /** Replaces visibility data while preserving the other section state. */
    public static long withVisibilityData(long packed, long visibilityData) {
        return (packed & ~VISIBILITY_MASK) | (visibilityData & VISIBILITY_MASK);
    }

    /** Returns the visual-presence flags stored in the packed metadata. */
    public static int getVisualsFlags(long packed) {
        return (int) ((packed >>> VISUALS_FLAGS_SHIFT) & VISUALS_FLAGS_MASK);
    }

    /** Replaces visual-presence flags while preserving the other section state. */
    public static long withVisualsFlags(long packed, int flags) {
        return (packed & ~(VISUALS_FLAGS_MASK << VISUALS_FLAGS_SHIFT))
                | (((long) flags & VISUALS_FLAGS_MASK) << VISUALS_FLAGS_SHIFT);
    }

    /**
     * Decodes the pending update type.
     *
     * @return the pending update type, or {@code null} when no update is pending
     */
    public static @Nullable ChunkUpdateType getPendingUpdate(long packed) {
        int encoded = (int) ((packed >>> PENDING_UPDATE_SHIFT) & PENDING_UPDATE_MASK);
        return encoded == 0 ? null : ChunkUpdateType.VALUES[encoded - 1];
    }

    /** Replaces the pending update type while preserving the other section state. */
    public static long withPendingUpdate(long packed, @Nullable ChunkUpdateType type) {
        long encoded = type == null ? 0L : (type.ordinal() + 1L);
        return (packed & ~(PENDING_UPDATE_MASK << PENDING_UPDATE_SHIFT))
                | (encoded << PENDING_UPDATE_SHIFT);
    }

    /** Returns whether a section build currently has a cancellation token. */
    public static boolean isBuildInFlight(long packed) {
        return (packed & BUILD_IN_FLIGHT_FLAG) != 0;
    }

    /** Sets the build-in-flight flag while preserving the other section state. */
    public static long withBuildInFlight(long packed, boolean inFlight) {
        return inFlight ? (packed | BUILD_IN_FLIGHT_FLAG) : (packed & ~BUILD_IN_FLIGHT_FLAG);
    }

    /*
     * Compact metadata passed from the lattice search to the render-list
     * collector. The compact word starts at the visual flags and retains only
     * the fields consumed by that collector.
     */
    private static final int COMPACT_META_SHIFT = VISUALS_FLAGS_SHIFT;
    private static final long COMPACT_META_FIELDS =
            (VISUALS_FLAGS_MASK << VISUALS_FLAGS_SHIFT)
                    | (PENDING_UPDATE_MASK << PENDING_UPDATE_SHIFT)
                    | BUILD_IN_FLIGHT_FLAG;
    private static final int COMPACT_META_MASK = (int) (COMPACT_META_FIELDS >>> COMPACT_META_SHIFT);
    private static final int COMPACT_PENDING_SHIFT = PENDING_UPDATE_SHIFT - COMPACT_META_SHIFT;
    private static final int COMPACT_BUILD_IN_FLIGHT_BIT = BUILD_IN_FLIGHT_BIT - COMPACT_META_SHIFT;

    /** Extracts the collector fields into a compact integer. */
    public static int toCompactMeta(long packed) {
        return (int) (packed >>> COMPACT_META_SHIFT) & COMPACT_META_MASK;
    }

    /** Returns visual-presence flags from compact collector metadata. */
    public static int getCompactVisualsFlags(int meta) {
        return meta & (int) VISUALS_FLAGS_MASK;
    }

    /** Returns the pending update type from compact collector metadata. */
    public static @Nullable ChunkUpdateType getCompactPendingUpdate(int meta) {
        int encoded = (meta >>> COMPACT_PENDING_SHIFT) & (int) PENDING_UPDATE_MASK;
        return encoded == 0 ? null : ChunkUpdateType.VALUES[encoded - 1];
    }

    /** Returns whether compact collector metadata marks a build in flight. */
    public static boolean isCompactBuildInFlight(int meta) {
        return (meta & (1 << COMPACT_BUILD_IN_FLIGHT_BIT)) != 0;
    }
}
