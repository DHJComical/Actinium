package org.taumc.celeritas.compat;

import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import com.dhj.actinium.world.cloned.ActiniumBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.registries.IRegistryDelegate;
import org.embeddedt.embeddium.api.shader.vertex.BlockRenderContext;
import org.embeddedt.embeddium.api.shader.vertex.ContextAwareChunkVertexEncoder;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.LightPipelineProvider;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BakedQuadGroupAnalyzer;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Private renderer boundary exposed only inside the optional legacy compatibility jar.
 */
public interface LegacyRendererAccess {
    /**
     * Returns the model lookup used by the live renderer.
     */
    BlockModelShapes actiniumLegacy$getShapes();

    /**
     * Returns the live chunk-build context that owns output buffers.
     */
    VintageChunkBuildContext actiniumLegacy$getContext();

    /**
     * Returns the live lighting pipeline provider.
     */
    LightPipelineProvider actiniumLegacy$getLighters();

    /**
     * Returns the live block-color registry.
     */
    Map<IRegistryDelegate<Block>, IBlockColor> actiniumLegacy$getBlockColors();

    /**
     * Returns encoders prepared during the current block render.
     */
    ArrayList<ContextAwareChunkVertexEncoder> actiniumLegacy$getUsedContextEncoders();

    /**
     * Returns the shader context reset after each rendered block.
     */
    BlockRenderContext actiniumLegacy$getBlockRenderContext();

    /**
     * Reports whether render-pass optimization is globally enabled.
     */
    boolean actiniumLegacy$getUseRenderPassOptimization();

    /**
     * Returns the quad analyzer used to select render materials.
     */
    BakedQuadGroupAnalyzer actiniumLegacy$getAnalyzer();

    /**
     * Updates the state consumed by private geometry helpers.
     */
    void actiniumLegacy$setCurrentState(IBlockState state);

    /**
     * Updates the block access consumed by private geometry helpers.
     */
    void actiniumLegacy$setCurrentBlockAccess(ActiniumBlockAccess blockAccess);

    /**
     * Updates vanilla metadata for shader diagnostics.
     */
    void actiniumLegacy$setCurrentMetadata(int metadata);

    /**
     * Updates shader-adjusted metadata for encoder preparation.
     */
    void actiniumLegacy$setCurrentShaderMetadata(int metadata);

    /**
     * Returns shader-adjusted metadata for compatibility diagnostics.
     */
    int actiniumLegacy$getCurrentShaderMetadata();

    /**
     * Updates the shader block ID used by context-aware encoders.
     */
    void actiniumLegacy$setCurrentShaderBlockId(int blockId);

    /**
     * Updates the active render layer used by diagnostics.
     */
    void actiniumLegacy$setCurrentRenderLayer(BlockRenderLayer layer);

    /**
     * Updates material-selection flags for the next quad list.
     */
    void actiniumLegacy$setCurrentQuadRenderingFlags(int flags);

    /**
     * Applies the live renderer's shader metadata transformation.
     */
    int actiniumLegacy$applyShaderStateBits(IBlockState state, BlockPos pos,
                                            ActiniumBlockAccess blockAccess, int metadata);

    /**
     * Resolves the live shader block ID for a prepared state.
     */
    int actiniumLegacy$resolveShaderBlockId(IBlockState state, BlockPos pos);

    /**
     * Emits a quad list through the live renderer's private geometry path.
     */
    void actiniumLegacy$renderQuadList(ChunkModelBuilder defaultBuffer, ChunkBuildBuffers buffers,
                                       Material material, BlockPos pos, EnumFacing cullFace,
                                       LightPipeline lighter, IBlockColor colorProvider, Vec3d offset,
                                       List<BakedQuad> quads);
}
