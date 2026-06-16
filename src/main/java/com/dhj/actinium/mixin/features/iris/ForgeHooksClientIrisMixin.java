package com.dhj.actinium.mixin.features.iris;

import com.dhj.actinium.config.ActiniumRuntimeOptions;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ForgeHooksClient.class, remap = false)
public abstract class ForgeHooksClientIrisMixin {
    @Unique
    private static final ThreadLocal<List<BakedQuad>> actinium$fastLitItemQuads =
            ThreadLocal.withInitial(ArrayList::new);

    @Inject(
        method = "renderLitItem(Lnet/minecraft/client/renderer/RenderItem;Lnet/minecraft/client/renderer/block/model/IBakedModel;ILnet/minecraft/item/ItemStack;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void actinium$renderSimpleLitItem(RenderItem renderItem, IBakedModel model, int color, ItemStack stack, CallbackInfo ci) {
        if (!ActiniumRuntimeOptions.useFastLitItemRendering()) {
            return;
        }

        List<BakedQuad> quads = actinium$fastLitItemQuads.get();
        quads.clear();

        for (EnumFacing facing : EnumFacing.VALUES) {
            quads.addAll(model.getQuads(null, facing, 0L));
        }
        quads.addAll(model.getQuads(null, null, 0L));

        if (quads.isEmpty()) {
            ci.cancel();
            return;
        }

        if (!actinium$isSimpleLitItemModel(quads)) {
            quads.clear();
            return;
        }

        try {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
            renderItem.renderQuads(buffer, quads, color, stack);
            tessellator.draw();
            ci.cancel();
        } finally {
            quads.clear();
        }
    }

    @Unique
    private static boolean actinium$isSimpleLitItemModel(List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            if (!quad.shouldApplyDiffuseLighting()) {
                return false;
            }

            if (quad.getFormat() != DefaultVertexFormats.ITEM && quad.getFormat().hasUvOffset(1)) {
                return false;
            }
        }

        return true;
    }
}
