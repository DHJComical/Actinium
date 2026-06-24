package net.coderbot.iris.celeritas.vertices;

import net.coderbot.iris.debug.IrisGlDebug;
import net.coderbot.iris.vertices.NormalHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockStaticLiquid;
import org.embeddedt.embeddium.api.util.NormI8;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.joml.Vector3f;
import org.embeddedt.embeddium.api.shader.ShaderProvider;
import org.embeddedt.embeddium.api.shader.ShaderProviderHolder;
import org.embeddedt.embeddium.api.shader.vertex.BlockRenderContext;
import org.embeddedt.embeddium.api.shader.vertex.ContextAwareChunkVertexEncoder;
import org.embeddedt.embeddium.api.shader.vertex.ExtendedDataHelper;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

public class ExtendedChunkVertexEncoder implements ContextAwareChunkVertexEncoder {
    private static final int MID_TEX_OFFSET = ExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("mc_midTexCoord").getPointer();
    private static final int TANGENT_OFFSET = ExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("at_tangent").getPointer();
    private static final int NORMAL_OFFSET = ExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("iris_Normal").getPointer();
    private static final int MC_ENTITY_OFFSET = ExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("mc_Entity").getPointer();
    private static final int MID_BLOCK_OFFSET = ExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("at_midBlock").getPointer();

    private final ChunkVertexEncoder baseEncoder = ExtendedChunkVertexType.BASE_TYPE.createEncoder();
    private final LwjglQuadView quad = new LwjglQuadView();
    private final Vector3f normal = new Vector3f();

    private BlockRenderContext context;
    private int vertexCount;
    private float uSum;
    private float vSum;

    @Override
    public void prepareToRenderBlock(BlockRenderContext ctx, Block block, int metadata, short renderType, byte lightValue) {
        this.context = ctx;
        ctx.blockId = resolveBlockStateId(block, metadata);
        ctx.renderType = renderType;
        ctx.lightValue = lightValue;
    }

    @Override
    public void prepareToRenderFluid(BlockRenderContext ctx, Block block, int metadata, byte lightValue) {
        this.context = ctx;
        ctx.blockId = resolveFluidBlockStateId(block, metadata);
        ctx.renderType = ExtendedDataHelper.FLUID_RENDER_TYPE;
        ctx.lightValue = lightValue;
    }

    @Override
    public void prepareToRenderVanilla(BlockRenderContext ctx) {
        this.context = ctx;
    }

    @Override
    public void finishRenderingBlock() {
        if (this.context != null) {
            this.context.reset();
            this.context = null;
        }
        this.vertexCount = 0;
        this.uSum = 0.0f;
        this.vSum = 0.0f;
    }

    @Override
    public long write(long ptr, Material material, Vertex vertex, int sectionIndex) {
        this.uSum += vertex.u;
        this.vSum += vertex.v;
        this.vertexCount++;

        BlockRenderContext ctx = this.context;
        if (ctx == null) {
            ctx = new BlockRenderContext();
        }

        this.baseEncoder.write(ptr, material, vertex, sectionIndex);

        LWJGL.memPutInt(ptr + MC_ENTITY_OFFSET, ((ctx.blockId + 1) << 1) | (ctx.renderType & 1));
        if (this.vertexCount == 1) {
            IrisGlDebug.logTerrainMaterialSample(
                    "encoder",
                    ctx.blockId,
                    ctx.renderType,
                    ctx.lightValue,
                    ctx.localPosX,
                    ctx.localPosY,
                    ctx.localPosZ,
                    vertex.u,
                    vertex.v
            );
        }

        int midBlock = ExtendedDataHelper.computeMidBlock(vertex.x, vertex.y, vertex.z, ctx.localPosX, ctx.localPosY, ctx.localPosZ);
        LWJGL.memPutInt(ptr + MID_BLOCK_OFFSET, midBlock);
        LWJGL.memPutByte(ptr + MID_BLOCK_OFFSET + 3L, ctx.lightValue);

        if (this.vertexCount == 4) {
            this.vertexCount = 0;

            int midUV = ExtendedChunkVertexType.encodeMidTexture(this.uSum * 0.25f, this.vSum * 0.25f);
            LWJGL.memPutInt(ptr + MID_TEX_OFFSET, midUV);
            LWJGL.memPutInt(ptr + MID_TEX_OFFSET - ExtendedChunkVertexType.STRIDE, midUV);
            LWJGL.memPutInt(ptr + MID_TEX_OFFSET - (ExtendedChunkVertexType.STRIDE * 2L), midUV);
            LWJGL.memPutInt(ptr + MID_TEX_OFFSET - (ExtendedChunkVertexType.STRIDE * 3L), midUV);

            this.quad.setup(ptr, ExtendedChunkVertexType.STRIDE);
            NormalHelper.computeFaceNormal(this.normal, this.quad);
            int packedNormal = NormI8.pack(this.normal);
            LWJGL.memPutInt(ptr + NORMAL_OFFSET, packedNormal);
            LWJGL.memPutInt(ptr + NORMAL_OFFSET - ExtendedChunkVertexType.STRIDE, packedNormal);
            LWJGL.memPutInt(ptr + NORMAL_OFFSET - (ExtendedChunkVertexType.STRIDE * 2L), packedNormal);
            LWJGL.memPutInt(ptr + NORMAL_OFFSET - (ExtendedChunkVertexType.STRIDE * 3L), packedNormal);

            int tangent = NormalHelper.computeTangent(this.normal.x(), this.normal.y(), this.normal.z(), this.quad);
            LWJGL.memPutInt(ptr + TANGENT_OFFSET, tangent);
            LWJGL.memPutInt(ptr + TANGENT_OFFSET - ExtendedChunkVertexType.STRIDE, tangent);
            LWJGL.memPutInt(ptr + TANGENT_OFFSET - (ExtendedChunkVertexType.STRIDE * 2L), tangent);
            LWJGL.memPutInt(ptr + TANGENT_OFFSET - (ExtendedChunkVertexType.STRIDE * 3L), tangent);

            this.uSum = 0.0f;
            this.vSum = 0.0f;
        }

        return ptr + ExtendedChunkVertexType.STRIDE;
    }

    private static int resolveBlockStateId(Block block, int metadata) {
        ShaderProvider provider = ShaderProviderHolder.getProvider();
        return provider != null ? provider.getBlockStateId(block, metadata) : Block.getIdFromBlock(block);
    }

    private static int resolveFluidBlockStateId(Block block, int metadata) {
        int blockStateId = resolveBlockStateId(block, metadata);
        if (blockStateId != -1) {
            return blockStateId;
        }

        Block counterpart = liquidCounterpart(block);
        return counterpart != null ? resolveBlockStateId(counterpart, metadata) : -1;
    }

    private static Block liquidCounterpart(Block block) {
        if (block instanceof BlockStaticLiquid) {
            return Block.getBlockById(Block.getIdFromBlock(block) - 1);
        }

        if (block instanceof BlockDynamicLiquid) {
            return Block.getBlockById(Block.getIdFromBlock(block) + 1);
        }

        return null;
    }
}

