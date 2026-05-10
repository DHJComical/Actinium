package net.coderbot.iris.gl.framebuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class MinecraftFramebufferHelper {
    private MinecraftFramebufferHelper() {
    }

    public static void bindMainFramebuffer(boolean setViewport) {
        final Framebuffer main = Minecraft.getMinecraft().getFramebuffer();
        main.bindFramebuffer(setViewport);

        restoreMinecraftFramebufferBuffers();
    }

    public static void restoreMainFramebuffer(boolean setViewport) {
        bindMainFramebuffer(setViewport);
        GLStateManager.glUseProgram(0);
        GLStateManager.glBindVertexArray(0);
        GLStateManager.glActiveTexture(GL13.GL_TEXTURE0);
    }

    public static void restoreMinecraftFramebufferBuffers() {
        // Iris uses custom FBOs with explicit COLOR_ATTACHMENT draw/read buffers.
        // Minecraft's own framebuffer is also an FBO on 1.12, so restore its single color attachment
        // whenever Minecraft binds it directly.
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
    }

    public static void restoreDefaultFramebufferBuffers() {
        GL11.glDrawBuffer(GL11.GL_BACK);
        GL11.glReadBuffer(GL11.GL_BACK);
    }

    public static void bindDefaultFramebuffer() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        restoreDefaultFramebufferBuffers();
    }
}
