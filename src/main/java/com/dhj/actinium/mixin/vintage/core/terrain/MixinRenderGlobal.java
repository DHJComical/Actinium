package com.dhj.actinium.mixin.vintage.core.terrain;

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.dhj.actinium.compat.dh.DistantHorizonsCompat;
import com.dhj.actinium.compat.ichunutil.PortalViewportProvider;
import net.coderbot.iris.compat.rfp2.Rfp2Compat;
import com.gtnewhorizons.angelica.glsm.shadow.InternalShadowRenderingState;
import com.dhj.actinium.shadows.ShadowRenderingState;
import com.gtnewhorizons.angelica.compat.mojang.Camera;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.framebuffer.MinecraftFramebufferHelper;
import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.coderbot.iris.pipeline.HandRenderer;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import com.gtnewhorizon.gtnhlib.compat.Mods;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.terrain.SimpleWorldRenderer;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.embeddedt.embeddium.api.debug.RenderDebugHooksHolder;
import com.dhj.actinium.render.entity.EntityGatherer;
import com.dhj.actinium.render.entity.EntitySource;
import com.dhj.actinium.render.terrain.ActiniumWorldRenderer;
import com.dhj.actinium.render.terrain.TileEntityGlStateGuard;

import java.util.*;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal implements SimpleWorldRenderer.Provider<ActiniumWorldRenderer> {

    @Shadow
    @Final
    private Map<Integer, DestroyBlockProgress> damagedBlocks;

    @Shadow @Final private Minecraft mc;
    @Shadow
    @Final
    private RenderManager renderManager;
    @Shadow
    private int countEntitiesRendered;

    @Shadow
    protected abstract boolean isOutlineActive(Entity entityIn, Entity viewer, ICamera camera);

    @Shadow
    private WorldClient world;
    @Shadow
    @Final
    private Set<TileEntity> setTileEntities;
    private ActiniumWorldRenderer renderer;

    @Redirect(method = "loadRenderers", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;renderDistanceChunks:I", ordinal = 1))
    private int nullifyBuiltChunkStorage(GameSettings settings) {
        // Do not allow any resources to be allocated
        return 0;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, CallbackInfo ci) {
        this.renderer = new ActiniumWorldRenderer();
    }

    @Override
    public ActiniumWorldRenderer celeritas$getWorldRenderer() {
        return this.renderer;
    }

    @Inject(method = "setWorldAndLoadRenderers", at = @At("RETURN"))
    private void onWorldChanged(WorldClient world, CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setWorld(world);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @reason Redirect to our renderer
     * @author JellySquid
     */
    @Overwrite
    public int getRenderedChunks() {
        return this.renderer.getVisibleChunkCount();
    }

    /**
     * @reason Redirect the check to our renderer
     * @author JellySquid
     */
    @Overwrite
    public boolean hasNoChunkUpdates() {
        return this.renderer.isTerrainRenderComplete();
    }

    @Inject(method = "setDisplayListEntitiesDirty", at = @At("RETURN"))
    private void onTerrainUpdateScheduled(CallbackInfo ci) {
        this.renderer.scheduleTerrainUpdate();
    }

    /**
     * @reason Redirect the chunk layer render passes to our renderer
     * @author JellySquid
     */
    @Overwrite
    public int renderBlockLayer(BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn) {
        boolean renderDistantHorizonsLods = Mods.DISTANTHORIZONS
                && !ShadowRenderingState.areShadowsCurrentlyBeingRendered();

        WorldRenderingPipeline pipeline = null;
        if (Iris.enabled) {
            pipeline = Iris.getPipelineManager().getPipelineNullable();
            if (pipeline != null) {
                if (blockLayerIn == BlockRenderLayer.SOLID) {
                    pipeline.setPhase(WorldRenderingPhase.TERRAIN_SOLID);
                } else if (blockLayerIn == BlockRenderLayer.CUTOUT_MIPPED) {
                    pipeline.setPhase(WorldRenderingPhase.TERRAIN_CUTOUT_MIPPED);
                } else if (blockLayerIn == BlockRenderLayer.CUTOUT) {
                    pipeline.setPhase(WorldRenderingPhase.TERRAIN_CUTOUT);
                } else if (blockLayerIn == BlockRenderLayer.TRANSLUCENT) {
                    if (!ShadowRenderingState.areShadowsCurrentlyBeingRendered()
                            && IrisApiV0Impl.INSTANCE.isShaderPackInUse()) {
                        this.actinium$beginIrisTranslucents(pipeline, (float) partialTicks);
                        if (renderDistantHorizonsLods) {
                            DistantHorizonsCompat.renderDeferredLodsForShaders(this.world, partialTicks);
                        }
                    }
                    pipeline.setPhase(WorldRenderingPhase.TERRAIN_TRANSLUCENT);
                }
            }
        }
        RenderDevice.enterManagedCode();

        RenderHelper.disableStandardItemLighting();

        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.bindTexture(this.mc.getTextureMapBlocks().getGlTextureId());
        GlStateManager.enableTexture2D();

        this.mc.entityRenderer.enableLightmap();

        double d3 = entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * partialTicks;
        double d4 = entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * partialTicks + entityIn.getEyeHeight();
        double d5 = entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * partialTicks;

        long drawStartNanos = RenderDebugHooksHolder.beginRenderGlobalStageTiming();
        try {
            this.renderer.drawChunkLayer(blockLayerIn, d3, d4, d5);
        } finally {
            RenderDebugHooksHolder.recordRenderGlobalStageTiming("terrain-" + blockLayerIn.name().toLowerCase(Locale.ROOT), pass, drawStartNanos);
            RenderDevice.exitManagedCode();
        }

        this.mc.entityRenderer.disableLightmap();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

        if (pipeline != null) {
            pipeline.setPhase(WorldRenderingPhase.NONE);
        }

        return 1;
    }

    /**
     * @reason Redirect the terrain setup phase to our renderer
     * @author JellySquid
     */
    @WrapMethod(method = "setupTerrain")
    private void actinium$setupTerrain(
        Entity entity,
        double tick,
        ICamera camera,
        int frame,
        boolean spectator,
        Operation<Void> original
    ) {
        boolean portalCamera = camera instanceof PortalViewportProvider;
        if (portalCamera) {
            original.call(entity, tick, camera, frame, spectator);
        }

        RenderDevice.enterManagedCode();

        try {
            this.renderer.setPortalCamera(portalCamera);
            this.renderer.setupTerrain(((ViewportProvider)camera).sodium$createViewport(), ActiniumWorldRenderer.captureCameraState(entity, tick),
                    frame, spectator, false);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void markBlocksForUpdate(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        this.renderer.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, important);
    }

    // The following two redirects force light updates to trigger chunk updates and not check vanilla's chunk renderer
    // flags
    @Redirect(method = "updateClouds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;hasNoFreeRenderBuilders()Z"))
    private boolean alwaysHaveBuilders(ChunkRenderDispatcher instance) {
        return false;
    }

    @Redirect(method = "updateClouds", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z", ordinal = 1))
    private boolean alwaysHaveNoTasks(Set instance) {
        return true;
    }

    @Inject(method = "loadRenderers", at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.reload();
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderHelper;enableStandardItemLighting()V", shift = At.Shift.AFTER, ordinal = 1), cancellable = true)
    public void sodium$renderTileEntities(Entity entity, ICamera camera, float partialTicks, CallbackInfo ci) {
        int pass = net.minecraftforge.client.MinecraftForgeClient.getRenderPass();
        boolean renderShadowBlockEntities = !ShadowRenderingState.areShadowsCurrentlyBeingRendered()
                || InternalShadowRenderingState.shouldRenderShadowBlockEntities();

        if (renderShadowBlockEntities) {
            long blockEntityStartNanos = RenderDebugHooksHolder.beginRenderGlobalStageTiming();
            if (Iris.enabled) {
                GbufferPrograms.beginBlockEntities();
                GbufferPrograms.setBlockEntityDefaults();
            }
            this.renderer.renderBlockEntities(new ActiniumWorldRenderer.TileEntityRenderContext(damagedBlocks, partialTicks));
            if (Iris.enabled) {
                GbufferPrograms.endBlockEntities();
            }
            RenderDebugHooksHolder.recordRenderGlobalStageTiming("block-entities-main", pass, blockEntityStartNanos);
        }

        /*
         * Normally, setTileEntities will be empty because we suppress vanilla chunk rendering. However, some mods
         * inject a custom renderer into the set. So we render any TE we find in it.
         * https://github.com/pau101/Fairy-Lights/blob/8a92f770d69be6fa164d24d7a023d828249423bb/src/main/java/com/pau101/fairylights/client/ClientProxy.java#L203
         */
        if (renderShadowBlockEntities) {
            synchronized(this.setTileEntities) {
                if (!this.setTileEntities.isEmpty()) {
                    long setBlockEntityStartNanos = RenderDebugHooksHolder.beginRenderGlobalStageTiming();
                    TileEntityGlStateGuard.push();
                    try {
                        TileEntityRendererDispatcher.instance.preDrawBatch();
                        try {
                            for (var te : this.setTileEntities) {
                                if (te.shouldRenderInPass(pass)) {
                                    TileEntityRendererDispatcher.instance.render(te, partialTicks, -1);
                                }
                            }
                        } finally {
                            // Same leak guard as ActiniumWorldRenderer.renderBlockEntities.
                            TileEntityGlStateGuard.restoreForBatch();
                            TileEntityRendererDispatcher.instance.drawBatch(pass);
                        }
                    } finally {
                        TileEntityGlStateGuard.pop();
                    }
                    RenderDebugHooksHolder.recordRenderGlobalStageTiming("block-entities-set", pass, setBlockEntityStartNanos);
                }
            }
        }

        this.mc.entityRenderer.disableLightmap();
        this.mc.profiler.endSection();
        ci.cancel();
    }

    private void actinium$beginIrisTranslucents(WorldRenderingPipeline pipeline, float partialTicks) {
        MinecraftFramebufferHelper.restoreMinecraftFramebufferBuffers();
        RenderDebugHooksHolder.logWorldPassState("before-begin-hand", pipeline.getPhase().name(), "translucent-prelude");
        pipeline.beginHand();
        RenderDebugHooksHolder.logWorldPassState("after-begin-hand", pipeline.getPhase().name(), "translucent-prelude");
        HandRenderer.INSTANCE.renderSolid(partialTicks, Camera.INSTANCE, this.mc.renderGlobal, pipeline);
        RenderDebugHooksHolder.logWorldPassState("after-hand-solid", pipeline.getPhase().name(), "translucent-prelude");
        this.mc.profiler.endStartSection("iris_pre_translucent");
        pipeline.beginTranslucents();
        RenderDebugHooksHolder.logWorldPassState("after-begin-translucents", pipeline.getPhase().name(), "translucent-prelude");
    }

    /**
     * @reason Replace the debug string
     * @author JellySquid
     */
    @Overwrite
    public String getDebugInfoRenders() {
        return this.renderer.getChunksDebugString();
    }

    private final EntityGatherer celeritas$entityGatherer = new EntityGatherer();

    private List<Entity>[] celeritas$collectedEntities;

    /**
     * @author embeddedt
     * @reason reimplement entity render loop because vanilla's relies on the renderInfos list
     */
    @Inject(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderInfos:Ljava/util/List;", ordinal = 0))
    private void renderEntities(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci,
                                @Local(ordinal = 1) List<Entity> outlineEntityList,
                                @Local(ordinal = 2) List<Entity> multipassEntityList) {
        int pass = net.minecraftforge.client.MinecraftForgeClient.getRenderPass();
        double renderViewX = renderViewEntity.prevPosX + (renderViewEntity.posX - renderViewEntity.prevPosX) * partialTicks;
        double renderViewY = renderViewEntity.prevPosY + (renderViewEntity.posY - renderViewEntity.prevPosY) * partialTicks;
        double renderViewZ = renderViewEntity.prevPosZ + (renderViewEntity.posZ - renderViewEntity.prevPosZ) * partialTicks;
        if (pass == 0 || celeritas$collectedEntities == null) {
            IChunkProvider chunkProvider = this.world.getChunkProvider();

            if (!(chunkProvider instanceof AccessorChunkProviderClient provider)) {
                throw new IllegalStateException("Entity gathering needs the client chunk provider, got "
                        + chunkProvider.getClass().getName());
            }

            // gather() only appends, so the per-pass lists have to be reset before every collection.
            celeritas$entityGatherer.clear();
            celeritas$collectedEntities = celeritas$entityGatherer.gather(
                    actinium$createEntitySource(renderViewEntity, provider.celeritas$getLoadedChunks()));
        }
        EntityPlayerSP player = this.mc.player;
        BlockPos.MutableBlockPos entityBlockPos = new BlockPos.MutableBlockPos();
        ActiniumWorldRenderer worldRenderer = ActiniumWorldRenderer.instance();
        // Apply entity distance scaling
        Entity.setRenderDistanceWeight(MathHelper.clamp((double)this.mc.gameSettings.renderDistanceChunks / 8.0D, 1.0D, 2.5D) * 1);
        boolean irisEntities = Iris.enabled && IrisApiV0Impl.INSTANCE.isShaderPackInUse();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        long entitiesStartNanos = RenderDebugHooksHolder.beginRenderGlobalStageTiming();
        long renderCallNanos = 0L;
        int checkedEntities = 0;
        int visibleEntities = 0;
        int renderedEntities = 0;
        int outlinedEntities = 0;
        int multipassEntities = 0;
        try {
            for(Entity entity : celeritas$collectedEntities[pass]) {
                checkedEntities++;
                if (!this.actinium$shouldRenderShadowEntity(entity, renderViewEntity)) {
                    continue;
                }

                // Do regular vanilla checks for visibility
                if(!this.renderManager.shouldRender(entity, camera, renderViewX, renderViewY, renderViewZ) && !entity.isRidingOrBeingRiddenBy(player)) {
                    continue;
                }

                // Check if any corners of the bounding box are in a visible subchunk
                if(!worldRenderer.isEntityVisible(entity)) {
                    continue;
                }
                visibleEntities++;

                boolean isSleeping = renderViewEntity instanceof EntityLivingBase && ((EntityLivingBase) renderViewEntity).isPlayerSleeping();

                if ((entity != renderViewEntity || this.mc.gameSettings.thirdPersonView != 0 || isSleeping)
                        && (entity.posY < 0.0D || entity.posY >= 256.0D || this.world.isBlockLoaded(entityBlockPos.setPos(entity))))
                {
                    ++this.countEntitiesRendered;
                    long renderCallStartNanos = RenderDebugHooksHolder.beginRenderGlobalStageTiming();
                    this.renderManager.renderEntityStatic(entity, partialTicks, false);
                    if (renderCallStartNanos != 0L) {
                        renderCallNanos += System.nanoTime() - renderCallStartNanos;
                    }
                    renderedEntities++;

                    if (this.isOutlineActive(entity, renderViewEntity, camera))
                    {
                        outlineEntityList.add(entity);
                        outlinedEntities++;
                    }

                    if (this.renderManager.isRenderMultipass(entity)) {
                        multipassEntityList.add(entity);
                        multipassEntities++;
                    }
                }
            }
        } finally {
            RenderDebugHooksHolder.recordRenderGlobalStageTiming("entities", pass, entitiesStartNanos);
            RenderDebugHooksHolder.recordRenderGlobalCounterTiming(
                    "entities-loop-total",
                    pass,
                    entitiesStartNanos == 0L ? 0L : System.nanoTime() - entitiesStartNanos,
                    checkedEntities,
                    visibleEntities,
                    renderedEntities,
                    outlinedEntities,
                    multipassEntities
            );
            RenderDebugHooksHolder.recordRenderGlobalCounterTiming(
                    "entities-render-call",
                    pass,
                    renderCallNanos,
                    checkedEntities,
                    visibleEntities,
                    renderedEntities,
                    outlinedEntities,
                    multipassEntities
            );
        }
    }

    /**
     * Creates the entity source of the current frame: every chunk of the client's loaded chunk map
     * that lies inside the chunk window vanilla's {@code ViewFrustum} covers.
     *
     * <p>That window is the {@code (2 * renderDistanceChunks + 1)} square of chunks centered on the
     * chunk holding {@code floor(viewX) - 1}. Vanilla anchors the window with
     * {@code MathHelper.floor(viewX) - 8} and then snaps a ring of 16-block columns onto it, which
     * leaves the window one block behind the view entity; only chunks inside the window can reach
     * {@code renderInfos}, so scanning exactly them never misses a renderable entity.</p>
     *
     * <p>The loaded chunk map is walked once and each chunk is rejected from its map key, so a chunk
     * outside the window costs a range check and never a hash lookup or a chunk access.</p>
     *
     * @param renderViewEntity the entity the frustum is centered on, i.e. the one {@code setupTerrain}
     *                         feeds to {@code ViewFrustum.updateChunkPositions}
     * @param loadedChunks     the loaded chunk map of the client chunk provider
     * @return the chunk source for {@link EntityGatherer#gather(EntitySource)}
     */
    @Unique
    private EntitySource actinium$createEntitySource(Entity renderViewEntity, Long2ObjectMap<Chunk> loadedChunks) {
        int centerChunkX = MathHelper.intFloorDiv(MathHelper.floor(renderViewEntity.posX) - 1, 16);
        int centerChunkZ = MathHelper.intFloorDiv(MathHelper.floor(renderViewEntity.posZ) - 1, 16);
        int radius = this.mc.gameSettings.renderDistanceChunks;
        int minChunkX = centerChunkX - radius;
        int maxChunkX = centerChunkX + radius;
        int minChunkZ = centerChunkZ - radius;
        int maxChunkZ = centerChunkZ + radius;

        Iterable<Long2ObjectMap.Entry<Chunk>> scopedEntries = Iterables.filter(
                loadedChunks.long2ObjectEntrySet(),
                entry -> {
                    long chunkKey = entry.getLongKey();
                    int chunkX = (int)chunkKey;
                    int chunkZ = (int)(chunkKey >> 32);

                    return chunkX >= minChunkX && chunkX <= maxChunkX && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
                });

        return () -> Iterables.transform(scopedEntries, entry -> entry.getValue());
    }

    private boolean actinium$shouldRenderShadowEntity(Entity entity, Entity renderViewEntity) {
        if (!ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            return true;
        }
        if (Rfp2Compat.isPlayerDummy(entity)) {
            return false;
        }

        boolean renderEntities = InternalShadowRenderingState.shouldRenderShadowEntities();
        boolean renderPlayer = InternalShadowRenderingState.shouldRenderShadowPlayer();
        boolean playerEntity = entity == renderViewEntity
                || (this.mc.player != null && entity.isRidingOrBeingRiddenBy(this.mc.player));

        if (!renderEntities) {
            return renderPlayer && playerEntity;
        }

        return renderPlayer || !playerEntity;
    }
}

