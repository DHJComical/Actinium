package com.dhj.actinium.render.terrain.compile.pipeline;

import com.dhj.actinium.debug.ShaderRegressionDebug;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.model.pipeline.VertexBufferConsumer;
import net.minecraftforge.registries.IRegistryDelegate;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.impl.model.light.LightMode;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.LightPipelineProvider;
import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadOrientation;
import org.embeddedt.embeddium.impl.render.chunk.ChunkColorWriter;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BakedQuadGroupAnalyzer;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.embeddedt.embeddium.api.shader.vertex.BlockRenderContext;
import org.embeddedt.embeddium.api.shader.vertex.ContextAwareChunkVertexEncoder;
import org.embeddedt.embeddium.api.shader.vertex.ExtendedDataHelper;
import org.embeddedt.embeddium.api.shader.ShaderProvider;
import org.embeddedt.embeddium.api.shader.ShaderProviderHolder;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import com.dhj.actinium.runtime.ActiniumRuntime;
import com.dhj.actinium.world.cloned.ActiniumBlockAccess;
import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import com.dhj.actinium.render.terrain.compile.light.LightDataCache;
import com.dhj.actinium.render.terrain.compile.light.VintageDiffuseProvider;
import com.dhj.actinium.mixin.vintage.core.terrain.AccessorBlockColors;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class VintageBlockRenderer {
    private final BlockModelShapes shapes;
    private final VintageChunkBuildContext context;
    private final VertexBufferConsumer consumer;
    private final LightPipelineProvider lighters;
    private final Map<IRegistryDelegate<Block>, IBlockColor> blockColors;

    private final QuadLightData quadLightData = new QuadLightData();
    private final int[] quadColors = new int[4];
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();
    private final ArrayList<ContextAwareChunkVertexEncoder> usedContextEncoders = new ArrayList<>(4);
    private final BlockRenderContext blockRenderContext = new BlockRenderContext();
    private final boolean useRenderPassOptimization;

    private IBlockState currentState;
    private ActiniumBlockAccess currentBlockAccess;
    private int currentMetadata;
    private int currentShaderMetadata;
    private int currentShaderBlockId;
    private BlockRenderLayer currentRenderLayer;

    private final BakedQuadGroupAnalyzer analyzer = new BakedQuadGroupAnalyzer();

    private int currentQuadRenderingFlags;


    public VintageBlockRenderer(VintageChunkBuildContext context, LightDataCache cache) {
        this.shapes = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes();
        this.consumer = new VertexBufferConsumer();
        this.context = context;
        this.lighters = new LightPipelineProvider(cache, VintageDiffuseProvider.INSTANCE, false);
        this.blockColors = ((AccessorBlockColors)Minecraft.getMinecraft().getBlockColors()).getBlockColorMap();
        this.useRenderPassOptimization = ActiniumRuntime.options().performance.useRenderPassOptimization;
    }

    public void resetSharedState() {
    }

    public void renderBlock(IBlockState state, BlockPos pos, ActiniumBlockAccess blockAccess, BlockRenderLayer layer) {
        this.renderBlock(state, pos, blockAccess, layer, true);
    }

    public void renderBlock(IBlockState state, BlockPos pos, ActiniumBlockAccess blockAccess, BlockRenderLayer layer, boolean allowRenderPassOptimization) {
        int defaultFlags = BakedQuadGroupAnalyzer.USE_ALL_THINGS;
        if (!this.useRenderPassOptimization || !allowRenderPassOptimization) {
            defaultFlags &= ~BakedQuadGroupAnalyzer.USE_RENDER_PASS_OPTIMIZATION;
        }
        this.analyzer.setDefaultRenderingFlags(defaultFlags);
        this.usedContextEncoders.clear();

        if (blockAccess.getWorldType() != WorldType.DEBUG_ALL_BLOCK_STATES) {
            state = state.getActualState(blockAccess, pos);
        }
        var model = this.shapes.getModelForState(state);
        this.currentBlockAccess = blockAccess;
        this.currentMetadata = state.getBlock().getMetaFromState(state);
        this.currentShaderMetadata = applyShaderStateBits(state, pos, blockAccess, this.currentMetadata);
        state = state.getBlock().getExtendedState(state, blockAccess, pos);
        this.currentState = state;
        this.currentShaderBlockId = resolveShaderBlockId(state, pos);
        this.currentRenderLayer = layer;

        var buffers = this.context.buffers;
        var material = buffers.getRenderPassConfiguration().getMaterialForRenderType(layer);
        var buffer = buffers.get(material);

        long rand = MathHelper.getPositionRandom(pos);

        IBlockColor colorProvider = this.blockColors.get(state.getBlock().delegate);

        var offset = this.currentState.getOffset(blockAccess, pos);

        var aoEnabled = Minecraft.isAmbientOcclusionEnabled() && state.getLightValue(blockAccess, pos) == 0 && model.isAmbientOcclusion(state);

        var lighter = this.lighters.getLighter(aoEnabled ? LightMode.SMOOTH : LightMode.FLAT);

        for (var dir : EnumFacing.VALUES) {
            var quads = model.getQuads(state, dir, rand);

            if (quads.isEmpty() || !state.shouldSideBeRendered(blockAccess, pos, dir)) {
                continue;
            }

            this.currentQuadRenderingFlags = this.analyzer.getFlagsForRendering(VintageDiffuseProvider.fromEnumFacing(dir), BakedQuadView.ofList(quads));
            renderQuadList(buffer, buffers, material, pos, dir, lighter, colorProvider, offset, quads);
        }

        var quads = model.getQuads(state, null, rand);

        if (!quads.isEmpty()) {
            this.currentQuadRenderingFlags = this.analyzer.getFlagsForRendering(ModelQuadFacing.UNASSIGNED, BakedQuadView.ofList(quads));
            renderQuadList(buffer, buffers, material, pos, null, lighter, colorProvider, offset, quads);
        }

        this.usedContextEncoders.forEach(ContextAwareChunkVertexEncoder::finishRenderingBlock);
        this.usedContextEncoders.clear();
        this.blockRenderContext.reset();
        this.currentState = null;
        this.currentBlockAccess = null;
        this.currentShaderMetadata = 0;
        this.currentShaderBlockId = -1;
        this.currentRenderLayer = null;
    }

    private QuadLightData getVertexLight(LightPipeline lighter, BlockPos pos, EnumFacing cullFace, BakedQuadView quad) {
        QuadLightData light = this.quadLightData;
        boolean applyDirectionalShading = quad.hasShade() && !BlockRenderingSettings.INSTANCE.shouldDisableDirectionalShading();
        lighter.calculate(quad, pos.getX(), pos.getY(), pos.getZ(), light, VintageDiffuseProvider.fromEnumFacingOrUnassigned(cullFace), quad.getLightFace(), applyDirectionalShading, true);

        return light;
    }

    private int[] getVertexColors(BlockPos pos, IBlockColor colorProvider, BakedQuadView quad) {
        final int[] vertexColors = this.quadColors;

        if (colorProvider != null && quad.hasColor()) {
            // Force full alpha on all colors
            Arrays.fill(vertexColors, 0xFF000000 | ColorARGB.toABGR(colorProvider.colorMultiplier(this.currentState, this.currentBlockAccess, pos, quad.getColorIndex())));
        } else {
            Arrays.fill(vertexColors, 0xFFFFFFFF);
        }

        return vertexColors;
    }

    private void renderQuadList(ChunkModelBuilder defaultBuffer, ChunkBuildBuffers buffers,
                                Material material, BlockPos pos, EnumFacing cullFace,
                                LightPipeline lighter, IBlockColor colorProvider, Vec3d offset,
                                List<BakedQuad> quads) {
        int localX = pos.getX() & 15, localY = pos.getY() & 15, localZ = pos.getZ() & 15;
        var config = buffers.getRenderPassConfiguration();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            var quad = quads.get(i);
            var format = quad.getFormat();
            if (format != DefaultVertexFormats.ITEM && format != DefaultVertexFormats.BLOCK && format.getElement(0).getUsage() != VertexFormatElement.EnumUsage.POSITION) {
                throw new IllegalStateException("Vertex format does not have POSITION as first element: " + format);
            }

            BakedQuadView quadView = (BakedQuadView)quad;

            // Run through our light pipeline
            var light = this.getVertexLight(lighter, pos, cullFace, quadView);
            var colors = this.getVertexColors(pos, colorProvider, quadView);

            ModelQuadOrientation orientation = ModelQuadOrientation.NORMAL;

            var quadMaterial = BakedQuadGroupAnalyzer.chooseOptimalMaterial(this.currentQuadRenderingFlags, material, config, BakedQuadView.of(quad));
            ChunkModelBuilder buffer = (quadMaterial == material) ? defaultBuffer : buffers.get(quadMaterial);
            this.prepareEncoder(buffer, pos);

            this.writeGeometry(localX, localY, localZ, buffer, offset, quadMaterial,
                    quadView, colors, light, orientation);

            TextureAtlasSprite sprite = (TextureAtlasSprite)quadView.celeritas$getSprite();

            if (sprite.hasAnimationMetadata() && buffer.getSectionContextBundle() instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
                //noinspection unchecked
                ((Collection<TextureAtlasSprite>)mcData.animatedSprites).add(sprite);
            }
        }
    }

    private void prepareEncoder(ChunkModelBuilder builder, BlockPos pos) {
        if (!(builder.getEncoder() instanceof ContextAwareChunkVertexEncoder encoder)) {
            return;
        }

        if (this.usedContextEncoders.contains(encoder)) {
            return;
        }

        Block block = this.currentState.getBlock();
        byte lightValue = (byte) this.currentState.getLightValue(this.currentBlockAccess, pos);
        boolean isFluid = this.currentState.getMaterial() == net.minecraft.block.material.Material.WATER
                || this.currentState.getMaterial() == net.minecraft.block.material.Material.LAVA;
        int blockId = Block.getIdFromBlock(block);
        short renderType = isFluid
                ? ExtendedDataHelper.FLUID_RENDER_TYPE
                : ExtendedDataHelper.BLOCK_RENDER_TYPE;

        this.blockRenderContext.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, blockId, renderType, lightValue);
        ShaderRegressionDebug.logTerrainState(
                "prepare-encoder",
                block,
                pos,
                this.currentRenderLayer,
                this.currentMetadata,
                this.currentShaderMetadata,
                blockId,
                this.currentShaderBlockId,
                renderType,
                lightValue
        );
        if (isFluid) {
            encoder.prepareToRenderFluid(this.blockRenderContext, block, this.currentShaderMetadata, lightValue);
        } else {
            encoder.prepareToRenderBlock(this.blockRenderContext, block, this.currentShaderMetadata, renderType, lightValue);
            if (this.currentShaderBlockId != -1) {
                this.blockRenderContext.blockId = this.currentShaderBlockId;
            }
        }
        this.usedContextEncoders.add(encoder);
    }

    private int applyShaderStateBits(IBlockState state, BlockPos pos, ActiniumBlockAccess blockAccess, int metadata) {
        if (BlockRenderingSettings.INSTANCE.hasSnowyEntries()
                && BlockRenderingSettings.INSTANCE.getSnowyBlocks().contains(state.getBlock())
                && isSnowCovered(blockAccess, pos)) {
            return metadata | net.coderbot.iris.block_rendering.BlockMaterialMapping.SNOWY_META_BIT;
        }

        return metadata;
    }

    private int resolveShaderBlockId(IBlockState state, BlockPos pos) {
        Block block = state.getBlock();
        ShaderProvider provider = ShaderProviderHolder.getProvider();
        int providerId = provider != null ? provider.getBlockStateId(block, this.currentShaderMetadata) : Block.getIdFromBlock(block);
        int shaderBlockId = providerId;
        int nbtBlockId = -1;

        if (BlockRenderingSettings.INSTANCE.getBlockNbtMap() != null && block.hasTileEntity(state)) {
            TileEntity tileEntity = this.currentBlockAccess.getTileEntity(pos);
            nbtBlockId = BlockRenderingSettings.INSTANCE.resolveBlockNbtId(block, tileEntity);
            if (nbtBlockId != -1) {
                shaderBlockId = nbtBlockId;
            }
        }

        ShaderRegressionDebug.logTerrainIdResolution(
                block,
                pos,
                this.currentMetadata,
                this.currentShaderMetadata,
                providerId,
                -1,
                nbtBlockId,
                shaderBlockId
        );

        return shaderBlockId;
    }

    private boolean isSnowCovered(ActiniumBlockAccess blockAccess, BlockPos pos) {
        Block topBlock = blockAccess.getBlockState(pos.up()).getBlock();
        return topBlock == Blocks.SNOW_LAYER || topBlock == Blocks.SNOW;
    }

    private void writeGeometry(int localX, int localY, int localZ, ChunkModelBuilder builder,
                               Vec3d offset,
                               Material material,
                               BakedQuadView quad,
                               int[] colors,
                               QuadLightData light,
                               ModelQuadOrientation orientation)
    {
        var vertices = this.vertices;
        ChunkColorWriter colorWriter = BlockRenderingSettings.INSTANCE.shouldUseSeparateAo()
                ? ChunkColorWriter.SEPARATE_AO
                : ChunkColorWriter.EMBEDDIUM;

        ModelQuadFacing normalFace = quad.getNormalFace();

        int vanillaNormal = normalFace.getPackedNormal();
        int trueNormal = quad.getComputedFaceNormal();

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = orientation.getVertexIndex(dstIndex);

            var out = vertices[dstIndex];
            out.x = localX + quad.getX(srcIndex) + (float) offset.x;
            out.y = localY + quad.getY(srcIndex) + (float) offset.y;
            out.z = localZ + quad.getZ(srcIndex) + (float) offset.z;

            out.color = colorWriter.writeColor(ModelQuadUtil.mixARGBColors(colors[srcIndex], quad.getColor(srcIndex)), light.br[srcIndex]);

            out.u = quad.getTexU(srcIndex);
            out.v = quad.getTexV(srcIndex);

            out.light = ModelQuadUtil.mergeBakedLight(quad.getLight(srcIndex), quad.getVanillaLightEmission(), light.lm[srcIndex]);

            out.vanillaNormal = vanillaNormal;
            out.trueNormal = trueNormal;
        }

        var vertexBuffer = builder.getVertexBuffer(normalFace);
        vertexBuffer.push(vertices, material);
    }
}

