package com.dhj.actinium.compat.dh.mixin;

import com.seibel.distanthorizons.core.dataObjects.transformers.FullDataToRenderDataTransformer;
import com.seibel.distanthorizons.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FullDataToRenderDataTransformer.class, remap = false)
public class MixinFullDataToRenderDataTransformer {
    @Unique
    private static final Object actinium$dhBlockStateCacheLock = new Object();

    @Redirect(
            method = "setRenderColumnView",
            at = @At(value = "INVOKE", target = "Lcom/seibel/distanthorizons/core/wrapperInterfaces/IWrapperFactory;getRendererIgnoredBlocks(Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/ILevelWrapper;)Lit/unimi/dsi/fastutil/objects/ObjectOpenHashSet;"),
            remap = false)
    private static ObjectOpenHashSet<IBlockStateWrapper> actinium$getRendererIgnoredBlocks(IWrapperFactory instance, ILevelWrapper levelWrapper) {
        synchronized (actinium$dhBlockStateCacheLock) {
            return instance.getRendererIgnoredBlocks(levelWrapper);
        }
    }

    @Redirect(
            method = "setRenderColumnView",
            at = @At(value = "INVOKE", target = "Lcom/seibel/distanthorizons/core/wrapperInterfaces/IWrapperFactory;getRendererIgnoredCaveBlocks(Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/ILevelWrapper;)Lit/unimi/dsi/fastutil/objects/ObjectOpenHashSet;"),
            remap = false)
    private static ObjectOpenHashSet<IBlockStateWrapper> actinium$getRendererIgnoredCaveBlocks(IWrapperFactory instance, ILevelWrapper levelWrapper) {
        synchronized (actinium$dhBlockStateCacheLock) {
            return instance.getRendererIgnoredCaveBlocks(levelWrapper);
        }
    }

    @Redirect(
            method = "setRenderColumnView",
            at = @At(value = "INVOKE", target = "Lcom/seibel/distanthorizons/core/wrapperInterfaces/IWrapperFactory;getWaterSubsurfaceReplacementBlocks(Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/ILevelWrapper;)Lit/unimi/dsi/fastutil/objects/ObjectOpenHashSet;"),
            remap = false)
    private static ObjectOpenHashSet<IBlockStateWrapper> actinium$getWaterSubsurfaceReplacementBlocks(IWrapperFactory instance, ILevelWrapper levelWrapper) {
        synchronized (actinium$dhBlockStateCacheLock) {
            return instance.getWaterSubsurfaceReplacementBlocks(levelWrapper);
        }
    }

    @Redirect(
            method = "setRenderColumnView",
            at = @At(value = "INVOKE", target = "Lcom/seibel/distanthorizons/core/wrapperInterfaces/IWrapperFactory;getWaterSurfaceReplacementBlocks(Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/ILevelWrapper;)Lit/unimi/dsi/fastutil/objects/ObjectOpenHashSet;"),
            remap = false)
    private static ObjectOpenHashSet<IBlockStateWrapper> actinium$getWaterSurfaceReplacementBlocks(IWrapperFactory instance, ILevelWrapper levelWrapper) {
        synchronized (actinium$dhBlockStateCacheLock) {
            return instance.getWaterSurfaceReplacementBlocks(levelWrapper);
        }
    }

    @Redirect(
            method = "setRenderColumnView",
            at = @At(value = "INVOKE", target = "Lcom/seibel/distanthorizons/core/wrapperInterfaces/IWrapperFactory;getWaterBlockStateWrapper(Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/ILevelWrapper;)Lcom/seibel/distanthorizons/core/wrapperInterfaces/block/IBlockStateWrapper;"),
            remap = false)
    private static IBlockStateWrapper actinium$getWaterBlockStateWrapper(IWrapperFactory instance, ILevelWrapper levelWrapper) {
        synchronized (actinium$dhBlockStateCacheLock) {
            return instance.getWaterBlockStateWrapper(levelWrapper);
        }
    }
}
