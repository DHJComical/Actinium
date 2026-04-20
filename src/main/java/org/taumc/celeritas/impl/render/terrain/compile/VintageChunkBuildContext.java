package org.taumc.celeritas.impl.render.terrain.compile;

import com.dhj.actinium.block_rendering.ActiniumBlockRenderingSettings;
import com.dhj.actinium.celeritas.ActiniumShaders;
import com.dhj.actinium.celeritas.buffer.ActiniumBufferBuilderExtension;
import com.dhj.actinium.celeritas.buffer.ActiniumVanillaQuadContext;
import com.dhj.actinium.celeritas.vertices.BlockRenderContext;
import com.dhj.actinium.celeritas.vertices.ContextAwareChunkVertexEncoder;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.vertices.ActiniumExtendedDataHelper;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.QuadUtil;
import org.lwjgl.opengl.GL11;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.impl.extensions.TextureMapExtension;
import org.taumc.celeritas.impl.render.terrain.compile.light.LightDataCache;
import org.taumc.celeritas.impl.render.terrain.compile.pipeline.VintageBlockRenderer;
import org.taumc.celeritas.impl.world.WorldSlice;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public class VintageChunkBuildContext extends ChunkBuildContext {
    public static final BlockRenderLayer[] LAYERS = BlockRenderLayer.values();
    private final TextureMapExtension textureAtlas;
    private final net.minecraft.client.renderer.BufferBuilder[] worldRenderers = new net.minecraft.client.renderer.BufferBuilder[LAYERS.length];
    private final boolean[] usedWorldRenderers = new boolean[LAYERS.length];
    @Getter
    private int offX, offY, offZ;
    @Getter
    private final WorldSlice worldSlice;
    @Getter
    private final VintageBlockRenderer blockRenderer;
    private final RenderPassConfiguration<?> renderPassConfiguration;
    private final LightDataCache lightDataCache;
    private final boolean useRenderPassOptimization;
    private final BlockRenderContext vanillaBlockRenderContext = new BlockRenderContext();
    private static boolean loggedVanillaWaterMapping;
    private static boolean loggedVanillaWaterQuad;
    private static boolean loggedVanillaTranslucentCopySummary;

    public VintageChunkBuildContext(WorldClient world, RenderPassConfiguration renderPassConfiguration) {
        super(renderPassConfiguration);
        this.renderPassConfiguration = renderPassConfiguration;
        this.worldSlice = new WorldSlice(world);
        this.lightDataCache = new LightDataCache(this.worldSlice);
        this.blockRenderer = new VintageBlockRenderer(this, lightDataCache);
        this.textureAtlas = (TextureMapExtension) Minecraft.getMinecraft().getTextureMapBlocks();
        this.useRenderPassOptimization = CeleritasVintage.options().performance.useRenderPassOptimization;
    }

    public void setupTranslation(int x, int y, int z) {
        this.lightDataCache.reset(x, y, z);

        this.offX = x;
        this.offY = y;
        this.offZ = z;
    }

    public net.minecraft.client.renderer.BufferBuilder getBufferForLayer(BlockRenderLayer layer) {
        var builder = this.worldRenderers[layer.ordinal()];
        if (builder == null) {
            builder = new net.minecraft.client.renderer.BufferBuilder(131072);
            this.worldRenderers[layer.ordinal()] = builder;
        }
        if (!this.usedWorldRenderers[layer.ordinal()]) {
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            builder.setTranslation(-this.offX, -this.offY, -this.offZ);
            this.usedWorldRenderers[layer.ordinal()] = true;
        }
        return builder;
    }

    public void convertVanillaDataToCeleritasData(ChunkBuildBuffers buffers) {
        var renderers = this.worldRenderers;
        var used = this.usedWorldRenderers;
        for (int i = 0; i < renderers.length; i++) {
            if(!used[i]) {
                continue;
            }
            var bufferBuilder = Objects.requireNonNull(renderers[i]);
            bufferBuilder.finishDrawing();
            used[i] = false;
            ByteBuffer rawBuffer = bufferBuilder.getByteBuffer();
            List<ActiniumVanillaQuadContext> quadContexts = bufferBuilder instanceof ActiniumBufferBuilderExtension extension
                    ? extension.actinium$consumeQuadContexts()
                    : Collections.emptyList();
            var material = buffers.getRenderPassConfiguration().getMaterialForRenderType(LAYERS[i]);
            copyBlockData(rawBuffer, buffers, material, quadContexts);
        }
    }

    public void beginVanillaBlockRender(BufferBuilder buffer, BlockPos pos, IBlockState state) {
        if (!(buffer instanceof ActiniumBufferBuilderExtension extension)) {
            return;
        }

        int metadata = state.getBlock().getMetaFromState(state);
        int shaderBlockId = ActiniumBlockRenderingSettings.INSTANCE.getBlockStateId(state.getBlock(), metadata);
        short renderType = state.getMaterial() == net.minecraft.block.material.Material.WATER
                ? ActiniumExtendedDataHelper.FLUID_RENDER_TYPE
                : ActiniumExtendedDataHelper.BLOCK_RENDER_TYPE;

        if (!loggedVanillaWaterMapping && renderType == ActiniumExtendedDataHelper.FLUID_RENDER_TYPE && ActiniumShaderPackManager.isDebugEnabled()) {
            loggedVanillaWaterMapping = true;
            ActiniumShaders.logger().info("[DEBUG] Vanilla water shader block mapping: block='{}', metadata={}, shaderBlockId={}, renderType={}",
                    Block.REGISTRY.getNameForObject(state.getBlock()),
                    metadata,
                    shaderBlockId,
                    renderType);
        }

        extension.actinium$setActiveQuadContext(new ActiniumVanillaQuadContext(
                pos.getX() & 15,
                pos.getY() & 15,
                pos.getZ() & 15,
                shaderBlockId,
                renderType,
                (byte) state.getLightValue(this.worldSlice, pos)
        ));
    }

    public void beginVanillaFluidRender(BufferBuilder buffer, BlockPos pos, IBlockState state) {
        if (!(buffer instanceof ActiniumBufferBuilderExtension extension)) {
            return;
        }

        extension.actinium$setActiveQuadContext(new ActiniumVanillaQuadContext(
                pos.getX() & 15,
                pos.getY() & 15,
                pos.getZ() & 15,
                ActiniumBlockRenderingSettings.INSTANCE.getBlockStateId(state.getBlock(), state.getBlock().getMetaFromState(state)),
                ActiniumExtendedDataHelper.FLUID_RENDER_TYPE,
                (byte) state.getLightValue(this.worldSlice, pos)
        ));
    }

    public void endVanillaRender(BufferBuilder buffer) {
        if (buffer instanceof ActiniumBufferBuilderExtension extension) {
            extension.actinium$setActiveQuadContext(null);
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.worldSlice.reset();
        for (int i = 0; i < LAYERS.length; i++) {
            if (this.usedWorldRenderers[i]) {
                this.worldRenderers[i].finishDrawing();
                if (this.worldRenderers[i] instanceof ActiniumBufferBuilderExtension extension) {
                    extension.actinium$consumeQuadContexts();
                }
                this.usedWorldRenderers[i] = false;
            }
        }
    }

    private Material selectMaterial(Material material, TextureAtlasSprite sprite) {
        if (sprite != null && sprite.getClass() == TextureAtlasSprite.class && !sprite.hasAnimationMetadata() && this.useRenderPassOptimization) {
            var transparencyLevel = ((SpriteTransparencyLevel.Holder)sprite).embeddium$getTransparencyLevel();
            if (transparencyLevel == SpriteTransparencyLevel.OPAQUE) {
                // Downgrade to solid
                return this.renderPassConfiguration.defaultSolidMaterial();
            } else if (material == this.renderPassConfiguration.defaultTranslucentMaterial() && transparencyLevel != SpriteTransparencyLevel.TRANSLUCENT) {
                // Downgrade to cutout
                return this.renderPassConfiguration.defaultCutoutMippedMaterial();
            }
        }
        return material;
    }

    private static final int BLOCK_VERTEX_FORMAT_SIZE;

    static {
        var format = DefaultVertexFormats.BLOCK;
        int size = 0;
        for (int i = 0; i < format.getElementCount(); i++) {
            size += format.getElement(i).getSize();
        }
        BLOCK_VERTEX_FORMAT_SIZE = size;
    }

    private void copyBlockData(ByteBuffer source, ChunkBuildBuffers buffers, Material material, List<ActiniumVanillaQuadContext> quadContexts) {
        int vsize = BLOCK_VERTEX_FORMAT_SIZE;
        int numQuads = source.limit() / (vsize * 4);
        long ptr = LWJGL.memAddress(source);
        var quad = ChunkVertexEncoder.Vertex.uninitializedQuad();
        var animatedSpritesList = ((MinecraftBuiltRenderSectionData<TextureAtlasSprite, TileEntity>)buffers.getSectionContextBundle()).animatedSprites;

        if (!loggedVanillaTranslucentCopySummary && material == this.renderPassConfiguration.defaultTranslucentMaterial()) {
            loggedVanillaTranslucentCopySummary = true;
            ActiniumShaders.logger().info(
                    "[DEBUG] Vanilla translucent quad copy summary: sourcePass='{}', numQuads={}, quadContexts={}",
                    material.pass,
                    numQuads,
                    quadContexts.size()
            );
        }

        for(int q = 0; q < numQuads; q++) {
            float uSum = 0, vSum = 0;
            for(int v = 0; v < 4; v++) {
                var vertex = quad[v];
                vertex.x = LWJGL.memGetFloat(ptr);
                vertex.y = LWJGL.memGetFloat(ptr + 4);
                vertex.z = LWJGL.memGetFloat(ptr + 8);
                vertex.color = LWJGL.memGetInt(ptr + 12);
                vertex.u = LWJGL.memGetFloat(ptr + 16);
                vertex.v = LWJGL.memGetFloat(ptr + 20);
                uSum += vertex.u;
                vSum += vertex.v;
                vertex.light = LWJGL.memGetInt(ptr + 24);
                ptr += vsize;
            }
            TextureAtlasSprite sprite = this.textureAtlas.celeritas$findFromUV(uSum * 0.25f, vSum * 0.25f);
            if (sprite != null && sprite.hasAnimationMetadata()) {
                animatedSpritesList.add(sprite);
            }
            int trueNormal = QuadUtil.calculateNormal(quad);
            for (int v = 0; v < 4; v++) {
                var vertex = quad[v];
                vertex.vanillaNormal = trueNormal;
                vertex.trueNormal = trueNormal;
            }
            ModelQuadFacing facing = QuadUtil.findNormalFace(trueNormal);
            ActiniumVanillaQuadContext quadContext = q < quadContexts.size() ? quadContexts.get(q) : null;
            boolean isFluidQuad = quadContext != null && quadContext.renderType() == ActiniumExtendedDataHelper.FLUID_RENDER_TYPE;
            Material optimizedMaterial = selectMaterial(material, sprite);
            Material correctMaterial = isFluidQuad ? material : optimizedMaterial;

            if (isFluidQuad && !loggedVanillaWaterQuad) {
                loggedVanillaWaterQuad = true;
                String spriteName = sprite != null ? sprite.getIconName() : "<null>";
                ActiniumShaders.logger().info(
                        "[DEBUG] Vanilla water quad conversion: sourcePass='{}', optimizedPass='{}', finalPass='{}', sprite='{}', blockStateId={}, renderType={}, lightValue={}, vertexLight=[0x{}, 0x{}, 0x{}, 0x{}]",
                        material.pass,
                        optimizedMaterial.pass,
                        correctMaterial.pass,
                        spriteName,
                        quadContext.blockStateId(),
                        quadContext.renderType(),
                        quadContext.lightValue() & 0xFF,
                        Integer.toHexString(quad[0].light),
                        Integer.toHexString(quad[1].light),
                        Integer.toHexString(quad[2].light),
                        Integer.toHexString(quad[3].light)
                );
            }

            ChunkModelBuilder builder = buffers.get(correctMaterial);
            ContextAwareChunkVertexEncoder encoder = this.prepareVanillaEncoder(builder, quadContext);
            builder.getVertexBuffer(facing).push(quad, correctMaterial);
            if (encoder != null) {
                encoder.finishRenderingBlock();
            }
        }
    }

    private ContextAwareChunkVertexEncoder prepareVanillaEncoder(ChunkModelBuilder builder, ActiniumVanillaQuadContext quadContext) {
        if (!(builder.getEncoder() instanceof ContextAwareChunkVertexEncoder encoder) || quadContext == null) {
            return null;
        }

        this.vanillaBlockRenderContext.set(
                quadContext.localPosX(),
                quadContext.localPosY(),
                quadContext.localPosZ(),
                quadContext.blockStateId(),
                quadContext.renderType(),
                quadContext.lightValue()
        );
        encoder.prepareToRenderVanilla(this.vanillaBlockRenderContext);
        return encoder;
    }
}
