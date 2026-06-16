package me.decce.gnetum;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import java.nio.FloatBuffer;

public class FramebufferManager {
    public static final boolean GL30SUPPORT = GLContext.getCapabilities().OpenGL30;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final FramebufferManager instance = new FramebufferManager();
    private int width;
    private int height;
    private int guiScale;
    private boolean fullscreen;
    private boolean dropCurrentFrame;
    private boolean complete; // whether the frontFramebuffer contains a complete HUD texture
    private final FloatBuffer clearColor;
    private Framebuffer backFramebuffer;
    private Framebuffer frontFramebuffer;

    private FramebufferManager() {
        clearColor = GLAllocation.createDirectFloatBuffer(4);
        clearColor.put(0).put(0).put(0).put(0);
        clearColor.flip();
        this.reset();
    }

    public static FramebufferManager getInstance() {
        return instance;
    }

    public void reset() {
        GnetumDebug.log("fbo-reset-begin oldWidth={} oldHeight={} oldGuiScale={} oldFullscreen={} oldBack={} oldFront={}",
                width,
                height,
                guiScale,
                fullscreen,
                backFramebuffer == null ? -1 : backFramebuffer.framebufferObject,
                frontFramebuffer == null ? -1 : frontFramebuffer.framebufferObject);
        width = mc.displayWidth;
        height = mc.displayHeight;
        guiScale = mc.gameSettings.guiScale;
        fullscreen = mc.gameSettings.fullScreen;
        if (backFramebuffer != null) {
            backFramebuffer.deleteFramebuffer();
        }
        if (frontFramebuffer != null) {
            frontFramebuffer.deleteFramebuffer();
        }
        backFramebuffer = new Framebuffer(width, height, true);
        backFramebuffer.setFramebufferColor(0, 0, 0, 0);
        backFramebuffer.setFramebufferFilter(GL11.GL_NEAREST);
        backFramebuffer.bindFramebuffer(false);
        this.clear();
        this.unbind();
        frontFramebuffer = new Framebuffer(width, height, true);
        frontFramebuffer.setFramebufferColor(0, 0, 0, 0);
        frontFramebuffer.setFramebufferFilter(GL11.GL_NEAREST);
        frontFramebuffer.bindFramebuffer(false);
        this.clear();
        this.unbind();
        this.complete = false;
        Gnetum.passManager.current = 1;
        GnetumDebug.log("fbo-reset-end width={} height={} guiScale={} fullscreen={} back={} backTex={} front={} frontTex={} pass={}",
                width,
                height,
                guiScale,
                fullscreen,
                backFramebuffer.framebufferObject,
                backFramebuffer.framebufferTexture,
                frontFramebuffer.framebufferObject,
                frontFramebuffer.framebufferTexture,
                Gnetum.passManager.current);
    }

    public void ensureSize() {
        if (mc.displayWidth != width ||
                mc.displayHeight != height ||
                mc.gameSettings.guiScale != guiScale ||
                mc.gameSettings.fullScreen != fullscreen) {
            GnetumDebug.log("fbo-ensure-size-reset display={}x{} stored={}x{} guiScale={} storedGuiScale={} fullscreen={} storedFullscreen={}",
                    mc.displayWidth,
                    mc.displayHeight,
                    width,
                    height,
                    mc.gameSettings.guiScale,
                    guiScale,
                    mc.gameSettings.fullScreen,
                    fullscreen);
            this.reset();
        }
        else {
            GnetumDebug.log("fbo-ensure-size-ok display={}x{} guiScale={} fullscreen={} complete={} back={} front={}",
                    width,
                    height,
                    guiScale,
                    fullscreen,
                    complete,
                    backFramebuffer.framebufferObject,
                    frontFramebuffer.framebufferObject);
        }
    }

    private void clear() {
        this.clear(backFramebuffer.framebufferObject);
    }

    private void clear(int fbo) {
        GnetumDebug.log("fbo-clear fbo={} gl30={}", fbo, GL30SUPPORT);
        OpenGlHelper.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GlStateManager.clearDepth(1.0D);
        if (GL30SUPPORT) {
            // Avoid altering clearColor state by using glClearBuffer
            // Fixes https://github.com/decce6/Gnetum/issues/8
            GL30.glClearBufferfv(GL11.GL_COLOR, 0, clearColor);
            GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        }
        else {
            // macOS does not have GL30 in compatibility context
            // Note: We exceptionally use the notorious `glPushAttrib` here, because we need to restore the original
            //  clear color state, yet there is no reliable way for us to know that state. Because we only use raw GL
            //  functions in the Push/PopAttrib pair, we shouldn't break anything.
            GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT); // This saves the clear color
            // Intentionally use glClearColor and not GL state manager methods to avoid breaking it
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glPopAttrib();
        }
    }

    public void bind() {
        this.bind(false);
    }

    public void bind(boolean setViewport) {
        GnetumDebug.log("fbo-bind back={} setViewport={} complete={}", backFramebuffer.framebufferObject, setViewport, complete);
        backFramebuffer.bindFramebuffer(setViewport);
        GnetumDebug.logGlState("after-fbo-bind");
    }

    public void unbind() {
        GnetumDebug.log("fbo-unbind minecraftFbo={}", mc.getFramebuffer().framebufferObject);
        mc.getFramebuffer().bindFramebuffer(false);
    }

    public void blit(double width, double height) {
        mc.profiler.startSection("blit");
        GnetumDebug.log("fbo-blit-begin front={} frontTex={} width={} height={} complete={}",
                frontFramebuffer.framebufferObject,
                frontFramebuffer.framebufferTexture,
                width,
                height,
                complete);

        frontFramebuffer.bindFramebufferTexture();

        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
        GlStateManager.disableDepth();
        GlStateManager.disableAlpha();
        GlStateManager.colorMask(true, true, true, false);
        GlStateManager.depthMask(false);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        builder.pos(0, height, 0).tex(0, 0).endVertex();
        builder.pos(width, height, 0).tex(1, 0).endVertex();
        builder.pos(width, 0, 0).tex(1, 1).endVertex();
        builder.pos(0, 0, 0).tex(0, 1).endVertex();
        tessellator.draw();

        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.depthMask(true);

        frontFramebuffer.unbindFramebufferTexture();
        GnetumDebug.log("fbo-blit-end front={} frontTex={}", frontFramebuffer.framebufferObject, frontFramebuffer.framebufferTexture);

        mc.profiler.endSection();
    }

    public void swapFramebuffers() {
        GnetumDebug.log("fbo-swap-begin dropCurrentFrame={} back={} front={} complete={}",
                dropCurrentFrame,
                backFramebuffer.framebufferObject,
                frontFramebuffer.framebufferObject,
                complete);
        if (!this.dropCurrentFrame) {
            Framebuffer temp = backFramebuffer;
            this.backFramebuffer = this.frontFramebuffer;
            this.frontFramebuffer = temp;
            this.complete = true;
            Gnetum.getFpsCounter().tick();
            GnetumDebug.log("fbo-swap-done back={} front={} complete=true", backFramebuffer.framebufferObject, frontFramebuffer.framebufferObject);
        }
        else {
            GnetumDebug.log("fbo-swap-dropped back={} front={} complete={}", backFramebuffer.framebufferObject, frontFramebuffer.framebufferObject, complete);
        }
        this.clear();
        this.dropCurrentFrame = false;
    }


    public void dropCurrentFrame() {
        this.dropCurrentFrame = true;
        GnetumDebug.log("fbo-drop-current-frame currentElement={} type={} pass={}", Gnetum.currentElement, Gnetum.currentElementType, Gnetum.passManager.current);
    }

    public int id() {
        return backFramebuffer.framebufferObject;
    }

    public boolean isComplete() {
        return this.complete;
    }

    public String describe() {
        return "display=" + width + "x" + height
                + ",guiScale=" + guiScale
                + ",fullscreen=" + fullscreen
                + ",complete=" + complete
                + ",dropCurrentFrame=" + dropCurrentFrame
                + ",back=" + (backFramebuffer == null ? -1 : backFramebuffer.framebufferObject)
                + ",backTex=" + (backFramebuffer == null ? -1 : backFramebuffer.framebufferTexture)
                + ",front=" + (frontFramebuffer == null ? -1 : frontFramebuffer.framebufferObject)
                + ",frontTex=" + (frontFramebuffer == null ? -1 : frontFramebuffer.framebufferTexture);
    }
}
