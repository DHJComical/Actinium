package com.dhj.actinium.mixin.features.iris;

import com.dhj.actinium.config.ActiniumRuntimeOptions;
import com.dhj.actinium.render.FastLitItemDisplayListCache;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.ForgeHooksClient;
import com.dhj.actinium.render.terrain.sprite.SpriteUtil;
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
            FastLitItemDisplayListCache.CachedDisplayList cached = ActiniumRuntimeOptions.useFastLitItemDisplayLists()
                    ? FastLitItemDisplayListCache.getOrCompile(renderItem, model, quads, color, stack)
                    : null;
            if (cached != null) {
                cached.render();
            } else if (actinium$canAppendRawItemQuads(quads, color)) {
                BufferBuilder buffer = tessellator.getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
                actinium$appendRawItemQuads(buffer, quads);
                tessellator.draw();
            } else {
                BufferBuilder buffer = tessellator.getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
                renderItem.renderQuads(buffer, quads, color, stack);
                tessellator.draw();
            }
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

    @Unique
    private static boolean actinium$canAppendRawItemQuads(List<BakedQuad> quads, int color) {
        if (color != -1) {
            return false;
        }

        for (BakedQuad quad : quads) {
            if (!DefaultVertexFormats.ITEM.equals(quad.getFormat()) || quad.hasTintIndex()) {
                return false;
            }
        }

        return true;
    }

    @Unique
    private static void actinium$appendRawItemQuads(BufferBuilder buffer, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.getSprite();
            if (sprite != null) {
                SpriteUtil.markSpriteActive(sprite);
            }
            buffer.addVertexData(quad.getVertexData());
        }
    }
}

