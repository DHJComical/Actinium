package me.decce.gnetum.hud;

import me.decce.gnetum.FastResourceLocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.util.function.Supplier;

public class Hud {
    private final ResourceLocation id;
    private final boolean alpha;
    private final boolean blend;
    private final boolean defaultBlendFunc;
    private final boolean depth;

    private final boolean dummy;
    private final Supplier<Boolean> condition;
    private final Runnable runnable;

    private Hud(ResourceLocation id, boolean alpha, boolean blend, boolean defaultBlendFunc, boolean depth, boolean dummy, Supplier<Boolean> condition, Runnable runnable) {
        this.id = id;
        this.alpha = alpha;
        this.blend = blend;
        this.defaultBlendFunc = defaultBlendFunc;
        this.depth = depth;
        this.dummy = dummy;
        this.condition = condition;
        this.runnable = runnable;
        HudManager.register(this);
    }

    public boolean isDummy() {
        return dummy;
    }

    public void render() {
        if (!condition.get()) return;
        if (!dummy) {
            if (alpha) GlStateManager.enableAlpha();
            else GlStateManager.disableAlpha();
            if (blend) GlStateManager.enableBlend();
            else GlStateManager.disableBlend();
            if (defaultBlendFunc) SharedValues.defaultBlendFunc();
            if (depth) GlStateManager.enableDepth();
            else GlStateManager.disableDepth();
        }
        runnable.run();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public ResourceLocation id() {
        return id;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ResourceLocation id;
        private boolean alpha = true;
        private boolean blend;
        private boolean defaultBlendFunc;
        private boolean depth;
        private boolean dummy;
        private Supplier<Boolean> condition;
        private Runnable runnable;

        public Builder id(String id) {
            this.id = new FastResourceLocation("minecraft", id);
            return this;
        }

        public Builder id(String namespace, String id) {
            this.id = new FastResourceLocation(namespace, id);
            return this;
        }

        public Builder id(ResourceLocation id) {
            this.id = id;
            return this;
        }

        public Builder alpha(boolean alpha) {
            this.alpha = alpha;
            return this;
        }

        public Builder blend(boolean blend) {
            this.blend = blend;
            return this;
        }

        public Builder defaultBlendFunc() {
            this.blend = true;
            this.defaultBlendFunc = true;
            return this;
        }

        public Builder depth(boolean depth) {
            this.depth = depth;
            return this;
        }

        public Builder dummy() {
            this.dummy = true;
            return this.id("dummy");
        }

        public Builder condition(Supplier<Boolean> condition) {
            this.condition = condition;
            return this;
        }

        public Builder onRender(Runnable runnable) {
            this.runnable = runnable;
            return this;
        }

        public Hud build() {
            if (condition == null) condition = () -> true;
            if (runnable == null) runnable = () -> {};
            return new Hud(id, alpha, blend, defaultBlendFunc, depth, dummy, condition, runnable);
        }
    }
}
