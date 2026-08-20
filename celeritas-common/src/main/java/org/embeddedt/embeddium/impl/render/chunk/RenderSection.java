package org.embeddedt.embeddium.impl.render.chunk;

import lombok.Getter;
import lombok.Setter;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentQuadAnalyzer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * The render state object for a chunk section. This contains all the graphics state for each render pass along with
 * data about the render in the chunk visibility graph.
 */
public class RenderSection extends AbstractSection {
    // Render Region State
    private final RenderRegion region;

    // We must use EVERYTHING here for the default visibility encoding, so that ContextBundle.empty() would correspond
    // to the data generated for an empty section.
    public static final BuiltRenderSectionData EMPTY_DATA = new BuiltRenderSectionData();

    static {
        EMPTY_DATA.hasBlockGeometry = false;
        EMPTY_DATA.visibilityData = VisibilityEncoding.EVERYTHING;
    }

    // Rendering State
    private BuiltRenderSectionData contextData;

    // Packed encoding of visibility data, visual flags, pending update type,
    // and build-in-flight state. The lattice mirror consumes this word through
    // metadataSink, so all writes must go through writeMetadata().
    private long packedMetadata;

    // Installed after construction so the initial metadata write can happen
    // before the section is attached to the render-list lattice.
    @Nullable
    private MetadataSink metadataSink;

    /**
     * Receives packed metadata changes so the graph-search lattice can mirror
     * the authoritative state held by this section.
     */
    public interface MetadataSink {
        /** Publishes the section's current packed metadata to its lattice mirror. */
        void onMetadataChanged(RenderSection section);
    }

    /**
     * A mapping from translucent render passes to the sort state for that particular pass (which contains data needed
     * to perform a resort of the geometry as the camera moves). Will be empty for sections without any translucent
     * render passes.
     */
    @Getter
    @NotNull
    private Map<TerrainRenderPass, TranslucentQuadAnalyzer.SortState> translucencySortStates = Collections.emptyMap();

    @Getter
    private TranslucentQuadAnalyzer.Level highestSortingLevel = TranslucentQuadAnalyzer.Level.NONE;

    @Getter
    @Setter
    private boolean needsDynamicTranslucencySorting;

    // Pending Update State

    // The in-flight build job for this section, if one exists. Serves two purposes:
    //   1. Cancellation: allows delete() to abort a queued or executing build early.
    //   2. Deduplication hint: VisibleChunkCollector skips re-queuing a section whose build
    //      is already in flight. submitRebuildTasks() performs the authoritative type check.
    @Nullable
    private CancellationToken buildCancellationToken = null;

    private int lastBuiltFrame = -1;
    private int lastSubmittedFrame = -1;
    private int lastSubmittedBuildFrame = -1;

    // Lifetime state
    private boolean disposed;

    @Getter
    @Setter
    private long lastBuildDurationNanos;

    // Used by the translucency sorter, to determine when a section needs sorting again
    public double lastCameraX, lastCameraY, lastCameraZ;

    public RenderSection(RenderRegion region, int chunkX, int chunkY, int chunkZ) {
        super(chunkX, chunkY, chunkZ);

        this.region = region;

        this.contextData = null;
        this.updateCachedContextDataFlags();
    }

    /**
     * Deletes all data attached to this render and drops any pending tasks. This should be used when the render falls
     * out of view or otherwise needs to be destroyed. After the render has been destroyed, the object can no longer
     * be used.
     */
    public void delete() {
        if (this.buildCancellationToken != null) {
            this.buildCancellationToken.setCancelled();
            this.setBuildCancellationToken(null);
        }

        this.setInfo(null);
        this.disposed = true;
    }

    public boolean setInfo(@Nullable BuiltRenderSectionData info) {
        boolean changed = !Objects.equals(info, this.contextData);
        if (changed) {
            if (this.contextData == null) {
                this.getRegion().updateSectionLoadTime(this);
            }
            this.contextData = info;
            this.updateCachedContextDataFlags();
        }
        return changed;
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    public boolean isBuilt() {
        return this.contextData != null;
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public @Nullable BuiltRenderSectionData getBuiltContext() {
        return this.contextData;
    }

    public void updateCachedContextDataFlags() {
        int flags = this.contextData != null ? this.contextData.getVisualBitmaskForSection() : 0;
        long visibilityData = this.contextData != null ? this.contextData.visibilityData : VisibilityEncoding.NULL;

        this.writeMetadata(PackedSectionMetadata.withVisibilityData(
                PackedSectionMetadata.withVisualsFlags(this.packedMetadata, flags), visibilityData));
    }

    /** Writes packed metadata and publishes the new value to the lattice mirror when attached. */
    private void writeMetadata(long packed) {
        this.packedMetadata = packed;
        if (this.metadataSink != null) {
            this.metadataSink.onMetadataChanged(this);
        }
    }

    /** Returns the authoritative packed metadata word mirrored by the lattice. */
    public long getPackedMetadata() {
        return this.packedMetadata;
    }

    /** Installs the lattice callback used for subsequent packed metadata changes. */
    public void setMetadataSink(@Nullable MetadataSink sink) {
        this.metadataSink = sink;
    }

    /** Returns the visual-presence flags cached in the packed section metadata. */
    public int getVisualsServiceFlags() {
        return PackedSectionMetadata.getVisualsFlags(this.packedMetadata);
    }

    /** Returns the visibility graph data cached in the packed section metadata. */
    public long getVisibilityData() {
        return PackedSectionMetadata.getVisibilityData(this.packedMetadata);
    }

    public boolean hasAnythingToRender() {
        return this.getVisualsServiceFlags() != 0;
    }

    public void setTranslucencySortStates(@NotNull Map<TerrainRenderPass, TranslucentQuadAnalyzer.SortState> sortStates) {
        this.translucencySortStates = Map.copyOf(sortStates);

        TranslucentQuadAnalyzer.Level level = TranslucentQuadAnalyzer.Level.NONE;
        boolean needsDynamicSorting = false;

        if (!sortStates.isEmpty()) {
            // Find highest level among all sort states
            for (TranslucentQuadAnalyzer.SortState state : sortStates.values()) {
                level = state.level().ordinal() > level.ordinal() ? state.level() : level;
                needsDynamicSorting |= state.requiresDynamicSorting();
            }
        }

        this.highestSortingLevel = level;
        this.needsDynamicTranslucencySorting = needsDynamicSorting;
    }

    public @Nullable CancellationToken getBuildCancellationToken() {
        return this.buildCancellationToken;
    }

    public void setBuildCancellationToken(@Nullable CancellationToken token) {
        this.buildCancellationToken = token;
        this.writeMetadata(PackedSectionMetadata.withBuildInFlight(this.packedMetadata, token != null));
    }

    public @Nullable ChunkUpdateType getPendingUpdate() {
        return PackedSectionMetadata.getPendingUpdate(this.packedMetadata);
    }

    public void setPendingUpdate(@Nullable ChunkUpdateType type) {
        this.writeMetadata(PackedSectionMetadata.withPendingUpdate(this.packedMetadata, type));
    }

    /**
     * Request a type of chunk update for this render section. This may "upgrade" an existing pending update for the
     * section.
     * @param type the chunk update
     * @return true if the section's chunk update type has changed
     */
    public boolean requestUpdate(ChunkUpdateType type) {
        type = ChunkUpdateType.getPromotionUpdateType(this.getPendingUpdate(), type);

        if (type != null) {
            this.setPendingUpdate(type);
            return true;
        } else {
            return false;
        }
    }

    public int getLastBuiltFrame() {
        return this.lastBuiltFrame;
    }

    public void setLastBuiltFrame(int lastBuiltFrame) {
        this.lastBuiltFrame = lastBuiltFrame;
    }

    public int getLastSubmittedFrame() {
        return this.lastSubmittedFrame;
    }

    public void setLastSubmittedFrame(int lastSubmittedFrame) {
        this.lastSubmittedFrame = lastSubmittedFrame;
    }

    public int getLastSubmittedBuildFrame() {
        return this.lastSubmittedBuildFrame;
    }

    public void setLastSubmittedBuildFrame(int lastSubmittedBuildFrame) {
        this.lastSubmittedBuildFrame = lastSubmittedBuildFrame;
    }
}
