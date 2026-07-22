package org.taumc.celeritas.impl.render.terrain.compile.pipeline;

import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import com.dhj.actinium.render.terrain.compile.light.LightDataCache;
import com.dhj.actinium.render.terrain.compile.light.VintageDiffuseProvider;
import com.dhj.actinium.world.cloned.ActiniumBlockAccess;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldType;
import org.embeddedt.embeddium.impl.model.light.LightMode;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BakedQuadGroupAnalyzer;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.taumc.celeritas.compat.LegacyRendererAccess;
import org.taumc.celeritas.impl.world.cloned.CeleritasBlockAccess;

import java.util.List;

/**
 * Binary-name alias for addons that still declare a Mixin against Celeritas's
 * renderer. Actinium owns the live renderer implementation.
 */
public class VintageBlockRenderer extends ActiniumVintageBlockRenderer {
    private IBlockState currentState;
    private CeleritasBlockAccess currentBlockAccess;

    public VintageBlockRenderer(VintageChunkBuildContext context, LightDataCache cache) {
        super(context, cache);
    }

    /**
     * Retains the original Celeritas method descriptor used by renderer addons.
     */
    public void renderBlock(IBlockState state, BlockPos pos, CeleritasBlockAccess blockAccess, BlockRenderLayer layer) {
        renderBlock(state, pos, blockAccess, layer, true);
    }

    @Override
    public void renderBlock(IBlockState state, BlockPos pos, ActiniumBlockAccess blockAccess, BlockRenderLayer layer) {
        renderBlock(state, pos, blockAccess, layer, true);
    }

    @Override
    public void renderBlock(IBlockState state, BlockPos pos, ActiniumBlockAccess blockAccess, BlockRenderLayer layer,
                            boolean allowRenderPassOptimization) {
        LegacyRendererAccess access = (LegacyRendererAccess) this;
        CeleritasBlockAccess legacyAccess = (CeleritasBlockAccess) blockAccess;
        int defaultFlags = BakedQuadGroupAnalyzer.USE_ALL_THINGS;
        if (!access.actiniumLegacy$getUseRenderPassOptimization() || !allowRenderPassOptimization) {
            defaultFlags &= ~BakedQuadGroupAnalyzer.USE_RENDER_PASS_OPTIMIZATION;
        }
        BakedQuadGroupAnalyzer analyzer = access.actiniumLegacy$getAnalyzer();
        analyzer.setDefaultRenderingFlags(defaultFlags);
        access.actiniumLegacy$getUsedContextEncoders().clear();

        if (legacyAccess.getWorldType() != WorldType.DEBUG_ALL_BLOCK_STATES) {
            state = state.getActualState(legacyAccess, pos);
        }
        var model = access.actiniumLegacy$getShapes().getModelForState(state);
        this.currentBlockAccess = legacyAccess;
        access.actiniumLegacy$setCurrentBlockAccess(legacyAccess);
        int currentMetadata = state.getBlock().getMetaFromState(state);
        access.actiniumLegacy$setCurrentMetadata(currentMetadata);
        int currentShaderMetadata = access.actiniumLegacy$applyShaderStateBits(
                state, pos, legacyAccess, currentMetadata);
        access.actiniumLegacy$setCurrentShaderMetadata(currentShaderMetadata);
        state = state.getBlock().getExtendedState(state, legacyAccess, pos);
        this.currentState = state;
        access.actiniumLegacy$setCurrentState(state);
        access.actiniumLegacy$setCurrentShaderBlockId(access.actiniumLegacy$resolveShaderBlockId(state, pos));
        access.actiniumLegacy$setCurrentRenderLayer(layer);

        ChunkBuildBuffers buffers = access.actiniumLegacy$getContext().buffers;
        Material material = buffers.getRenderPassConfiguration().getMaterialForRenderType(layer);
        ChunkModelBuilder buffer = buffers.get(material);
        long random = MathHelper.getPositionRandom(pos);
        IBlockColor colorProvider = access.actiniumLegacy$getBlockColors().get(state.getBlock().delegate);
        Vec3d offset = state.getOffset(legacyAccess, pos);
        boolean ambientOcclusion = Minecraft.isAmbientOcclusionEnabled()
                && state.getLightValue(legacyAccess, pos) == 0 && model.isAmbientOcclusion(state);
        LightPipeline lighter = access.actiniumLegacy$getLighters().getLighter(
                ambientOcclusion ? LightMode.SMOOTH : LightMode.FLAT);

        for (EnumFacing direction : EnumFacing.VALUES) {
            List<BakedQuad> quads = model.getQuads(state, direction, random);
            if (quads.isEmpty() || !state.shouldSideBeRendered(legacyAccess, pos, direction)) {
                continue;
            }
            access.actiniumLegacy$setCurrentQuadRenderingFlags(analyzer.getFlagsForRendering(
                    VintageDiffuseProvider.fromEnumFacing(direction),
                    BakedQuadView.ofList(quads)));
            renderQuadList(buffer, buffers, material, pos, direction, lighter, colorProvider, offset, quads);
        }

        List<BakedQuad> quads = model.getQuads(state, null, random);
        if (!quads.isEmpty()) {
            access.actiniumLegacy$setCurrentQuadRenderingFlags(analyzer.getFlagsForRendering(
                    ModelQuadFacing.UNASSIGNED, BakedQuadView.ofList(quads)));
            renderQuadList(buffer, buffers, material, pos, null, lighter, colorProvider, offset, quads);
        }

        access.actiniumLegacy$getUsedContextEncoders().forEach(encoder -> encoder.finishRenderingBlock());
        access.actiniumLegacy$getUsedContextEncoders().clear();
        access.actiniumLegacy$getBlockRenderContext().reset();
        this.currentState = null;
        access.actiniumLegacy$setCurrentState(null);
        this.currentBlockAccess = null;
        access.actiniumLegacy$setCurrentBlockAccess(null);
        access.actiniumLegacy$setCurrentShaderMetadata(0);
        access.actiniumLegacy$setCurrentShaderBlockId(-1);
        access.actiniumLegacy$setCurrentRenderLayer(null);
    }

    private void renderQuadList(ChunkModelBuilder defaultBuffer, ChunkBuildBuffers buffers, Material material,
                                BlockPos pos, EnumFacing cullFace, LightPipeline lighter,
                                IBlockColor colorProvider, Vec3d offset, List<BakedQuad> quads) {
        ((LegacyRendererAccess) this).actiniumLegacy$renderQuadList(defaultBuffer, buffers, material, pos, cullFace,
                lighter, colorProvider, offset, quads);
    }
}
