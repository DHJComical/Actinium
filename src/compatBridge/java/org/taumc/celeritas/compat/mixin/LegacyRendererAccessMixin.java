package org.taumc.celeritas.compat.mixin;

import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import com.dhj.actinium.render.terrain.compile.pipeline.VintageBlockRenderer;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.taumc.celeritas.compat.LegacyRendererAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements the bridge-private renderer access contract without changing Actinium's public ABI.
 */
@Mixin(value = VintageBlockRenderer.class, remap = false)
public abstract class LegacyRendererAccessMixin implements LegacyRendererAccess {
    @Override
    @Accessor("shapes")
    public abstract BlockModelShapes actiniumLegacy$getShapes();

    @Override
    @Accessor("context")
    public abstract VintageChunkBuildContext actiniumLegacy$getContext();

    @Override
    @Accessor("lighters")
    public abstract LightPipelineProvider actiniumLegacy$getLighters();

    @Override
    @Accessor("blockColors")
    public abstract Map<IRegistryDelegate<Block>, IBlockColor> actiniumLegacy$getBlockColors();

    @Override
    @Accessor("usedContextEncoders")
    public abstract ArrayList<ContextAwareChunkVertexEncoder> actiniumLegacy$getUsedContextEncoders();

    @Override
    @Accessor("blockRenderContext")
    public abstract BlockRenderContext actiniumLegacy$getBlockRenderContext();

    @Override
    @Accessor("useRenderPassOptimization")
    public abstract boolean actiniumLegacy$getUseRenderPassOptimization();

    @Override
    @Accessor("analyzer")
    public abstract BakedQuadGroupAnalyzer actiniumLegacy$getAnalyzer();

    @Override
    @Accessor("currentState")
    public abstract void actiniumLegacy$setCurrentState(IBlockState state);

    @Override
    @Accessor("currentBlockAccess")
    public abstract void actiniumLegacy$setCurrentBlockAccess(ActiniumBlockAccess blockAccess);

    @Override
    @Accessor("currentMetadata")
    public abstract void actiniumLegacy$setCurrentMetadata(int metadata);

    @Override
    @Accessor("currentShaderMetadata")
    public abstract void actiniumLegacy$setCurrentShaderMetadata(int metadata);

    @Override
    @Accessor("currentShaderMetadata")
    public abstract int actiniumLegacy$getCurrentShaderMetadata();

    @Override
    @Accessor("currentShaderBlockId")
    public abstract void actiniumLegacy$setCurrentShaderBlockId(int blockId);

    @Override
    @Accessor("currentRenderLayer")
    public abstract void actiniumLegacy$setCurrentRenderLayer(BlockRenderLayer layer);

    @Override
    @Accessor("currentQuadRenderingFlags")
    public abstract void actiniumLegacy$setCurrentQuadRenderingFlags(int flags);

    @Override
    @Invoker("applyShaderStateBits")
    public abstract int actiniumLegacy$applyShaderStateBits(IBlockState state, BlockPos pos,
                                                            ActiniumBlockAccess blockAccess, int metadata);

    @Override
    @Invoker("resolveShaderBlockId")
    public abstract int actiniumLegacy$resolveShaderBlockId(IBlockState state, BlockPos pos);

    @Override
    @Invoker("renderQuadList")
    public abstract void actiniumLegacy$renderQuadList(ChunkModelBuilder defaultBuffer, ChunkBuildBuffers buffers,
                                                       Material material, BlockPos pos, EnumFacing cullFace,
                                                       LightPipeline lighter, IBlockColor colorProvider, Vec3d offset,
                                                       List<BakedQuad> quads);
}
