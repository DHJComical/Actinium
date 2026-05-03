package com.dhj.actinium.shader.uniform;

import com.dhj.actinium.shader.pack.ActiniumIdMap;
import com.dhj.actinium.shader.pack.ActiniumNamespacedId;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.taumc.celeritas.impl.extensions.TextureMapExtension;

import java.lang.reflect.Method;
import java.nio.FloatBuffer;

public final class ActiniumOptiFineUniforms {
    private static final FloatBuffer CURRENT_COLOR_BUFFER = BufferUtils.createFloatBuffer(4);
    private static final @Nullable Method GL_GET_FLOAT_BUFFER_METHOD = findMethod(GL11.class, "glGetFloat", int.class, FloatBuffer.class);
    private static final Method PLAYER_GET_CURRENT_MOOD = findMethod(EntityPlayer.class, "getCurrentMood");
    private static final Method PLAYER_GET_MOOD_STRENGTH = findMethod(EntityPlayer.class, "getMoodStrength");
    private static final Method ITEM_GET_LIGHT_VALUE_STACK = findMethod(Item.class, "getLightValue", ItemStack.class);
    private static final Method ITEM_GET_LIGHT_VALUE = findMethod(Item.class, "getLightValue");

    private ActiniumOptiFineUniforms() {
    }

    public static float getAspectRatio(int width, int height) {
        return width / (float) Math.max(1, height);
    }

    public static int getWorldDay(@Nullable World world) {
        if (world == null) {
            return 0;
        }

        return (int) (world.getWorldTime() / 24000L);
    }

    public static float getSunAngle(@Nullable World world, float partialTicks) {
        if (world == null) {
            return 0.0f;
        }

        float skyAngle = world.getCelestialAngle(partialTicks);
        return skyAngle < 0.75F ? skyAngle + 0.25F : skyAngle - 0.75F;
    }

    public static float getShadowAngle(@Nullable World world, float partialTicks) {
        float sunAngle = getSunAngle(world, partialTicks);
        return sunAngle > 0.5f ? sunAngle - 0.5f : sunAngle;
    }

    public static float getEyeAltitude(@Nullable Entity entity) {
        if (entity == null) {
            return 0.0f;
        }

        return (float) (entity.posY + entity.getEyeHeight());
    }

    public static float getScreenBrightness() {
        return Minecraft.getMinecraft().gameSettings.gammaSetting;
    }

    public static int isHideGui() {
        return Minecraft.getMinecraft().gameSettings.hideGUI ? 1 : 0;
    }

    public static float getWetness(@Nullable World world) {
        if (world == null) {
            return 0.0f;
        }

        return MathHelper.clamp(world.getRainStrength(Minecraft.getMinecraft().getRenderPartialTicks()), 0.0f, 1.0f);
    }

    public static float getPlayerMood(@Nullable Entity entity) {
        if (!(entity instanceof EntityPlayer player)) {
            return 0.0f;
        }

        Float value = invokeFloat(player, PLAYER_GET_CURRENT_MOOD);
        if (value == null) {
            value = invokeFloat(player, PLAYER_GET_MOOD_STRENGTH);
        }

        return value != null ? MathHelper.clamp(value, 0.0f, 1.0f) : 0.0f;
    }

    public static int getFogMode() {
        if (!GlStateManager.fogState.fog.currentState) {
            return 0;
        }

        return GlStateManager.fogState.mode;
    }

    public static int getFogShape() {
        return GlStateManager.fogState.fog.currentState ? 0 : -1;
    }

    public static float getFogStart() {
        return GlStateManager.fogState.start;
    }

    public static float getFogEnd() {
        return GlStateManager.fogState.end;
    }

    public static float getFogDensity() {
        return Math.max(0.0f, GlStateManager.fogState.density);
    }

    public static int getHeldItemId(@Nullable Entity entity, boolean offHand) {
        if (!(entity instanceof EntityPlayer player)) {
            return -1;
        }

        ItemStack stack = player.getHeldItem(offHand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
        if (stack == null || stack.isEmpty()) {
            return -1;
        }

        Item item = stack.getItem();
        if (item == null) {
            return -1;
        }

        ActiniumIdMap idMap = ActiniumShaderPackManager.getActiveIdMap();
        var itemIds = idMap.getItemIds();
        if (itemIds != null && item.getRegistryName() != null) {
            ActiniumNamespacedId id = new ActiniumNamespacedId(item.getRegistryName().toString());
            int mapped = itemIds.getOrDefault(id, Integer.MIN_VALUE);
            if (mapped != Integer.MIN_VALUE) {
                return mapped;
            }
        }

        return Item.getIdFromItem(item);
    }

    public static int getHeldBlockLightValue(@Nullable Entity entity, boolean offHand) {
        if (!(entity instanceof EntityPlayer player)) {
            return 0;
        }

        ItemStack stack = player.getHeldItem(offHand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        Item item = stack.getItem();
        if (item == null) {
            return 0;
        }

        Integer reflected = invokeLightValue(item, stack);
        return reflected != null ? reflected : 0;
    }

    public static Vector4f getCurrentColorModulator() {
        CURRENT_COLOR_BUFFER.clear();
        invokeGlGetFloat(GL11.GL_CURRENT_COLOR, CURRENT_COLOR_BUFFER);
        return new Vector4f(
                CURRENT_COLOR_BUFFER.get(0),
                CURRENT_COLOR_BUFFER.get(1),
                CURRENT_COLOR_BUFFER.get(2),
                CURRENT_COLOR_BUFFER.get(3)
        );
    }

    public static int getBlendSrcRgb() {
        return GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    }

    public static int getBlendDstRgb() {
        return GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    }

    public static int getBlendSrcAlpha() {
        return GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    }

    public static int getBlendDstAlpha() {
        return GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    }

    public static int getAtlasWidth() {
        TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();
        if (textureMap instanceof TextureMapExtension extension) {
            return Math.max(1, extension.celeritas$getAtlasWidth());
        }

        return 0;
    }

    public static int getAtlasHeight() {
        TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();
        if (textureMap instanceof TextureMapExtension extension) {
            return Math.max(1, extension.celeritas$getAtlasHeight());
        }

        return 0;
    }

    public static int getTerrainIconSize() {
        return 16;
    }

    public static int getBlockEntityId() {
        return ActiniumCapturedRenderingState.getCurrentRenderedBlockEntity();
    }

    private static @Nullable Float invokeFloat(Object target, @Nullable Method method) {
        if (target == null || method == null) {
            return null;
        }

        try {
            Object value = method.invoke(target);
            if (value instanceof Number number) {
                return number.floatValue();
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    private static @Nullable Integer invokeLightValue(Item item, ItemStack stack) {
        if (ITEM_GET_LIGHT_VALUE_STACK != null) {
            try {
                Object value = ITEM_GET_LIGHT_VALUE_STACK.invoke(item, stack);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        if (ITEM_GET_LIGHT_VALUE != null) {
            try {
                Object value = ITEM_GET_LIGHT_VALUE.invoke(item);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        return null;
    }

    private static void invokeGlGetFloat(int parameter, FloatBuffer buffer) {
        if (GL_GET_FLOAT_BUFFER_METHOD == null) {
            throw new IllegalStateException("LWJGL GL11.glGetFloat(int, FloatBuffer) is unavailable");
        }

        try {
            GL_GET_FLOAT_BUFFER_METHOD.invoke(null, parameter, buffer);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to query OpenGL float state", e);
        }
    }

    private static @Nullable Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
