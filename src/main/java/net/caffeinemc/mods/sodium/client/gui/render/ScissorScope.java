package net.caffeinemc.mods.sodium.client.gui.render;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.IntBuffer;
import java.util.Arrays;

/** Saves and restores the complete OpenGL scissor state around clipped GUI rendering. */
public final class ScissorScope implements AutoCloseable {
    private static final Backend LWJGL_BACKEND = new LwjglBackend();
    private final Backend backend;
    private final boolean previouslyEnabled;
    private final int[] previousBox;
    private boolean closed;

    private ScissorScope(Backend backend, int[] box) {
        this.backend = backend;
        this.previouslyEnabled = backend.isEnabled();
        this.previousBox = backend.getBox();
        try {
            backend.enable();
            backend.setBox(box);
        } catch (RuntimeException exception) {
            try {
                backend.setBox(this.previousBox);
                if (this.previouslyEnabled) backend.enable(); else backend.disable();
            } catch (RuntimeException restoreException) {
                exception.addSuppressed(restoreException);
            }
            throw exception;
        }
    }

    public static ScissorScope open(GuiRect bounds, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getMinecraft();
        double scaleX = (double) minecraft.displayWidth / screenWidth;
        double scaleY = (double) minecraft.displayHeight / screenHeight;
        int[] box = {
                (int) Math.floor(bounds.x() * scaleX),
                (int) Math.floor((screenHeight - bounds.bottom()) * scaleY),
                (int) Math.ceil(bounds.width() * scaleX),
                (int) Math.ceil(bounds.height() * scaleY)
        };
        return new ScissorScope(LWJGL_BACKEND, box);
    }

    static ScissorScope open(Backend backend, int[] box) {
        if (backend == null || box == null || box.length != 4) {
            throw new IllegalArgumentException("Scissor backend and four-value box are required");
        }
        return new ScissorScope(backend, Arrays.copyOf(box, box.length));
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.backend.setBox(this.previousBox);
        if (this.previouslyEnabled) {
            this.backend.enable();
        } else {
            this.backend.disable();
        }
        this.closed = true;
    }

    interface Backend {
        boolean isEnabled();

        int[] getBox();

        void setBox(int[] box);

        void enable();

        void disable();
    }

    private static final class LwjglBackend implements Backend {
        @Override
        public boolean isEnabled() {
            return GLStateManager.glIsEnabled(GL11.GL_SCISSOR_TEST);
        }

        @Override
        public int[] getBox() {
            IntBuffer buffer = BufferUtils.createIntBuffer(4);
            GLStateManager.glGetInteger(GL11.GL_SCISSOR_BOX, buffer);
            return new int[]{buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3)};
        }

        @Override
        public void setBox(int[] box) {
            GLStateManager.glScissor(box[0], box[1], box[2], box[3]);
        }

        @Override
        public void enable() {
            GLStateManager.enableScissorTest();
        }

        @Override
        public void disable() {
            GLStateManager.disableScissorTest();
        }
    }
}
