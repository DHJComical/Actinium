package net.coderbot.iris.pipeline;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * Renders the sky horizon. Vanilla Minecraft simply uses the "clear color" for its horizon, and then draws a plane
 * above the player. This class extends the sky rendering so that an inverted octagonal cone is drawn around the player instead,
 * allowing shaders to perform more advanced sky rendering.
 * <p>
 * However, the horizon rendering is designed so that when sky shaders are not being used, it looks almost exactly the
 * same as vanilla sky rendering, except a few almost entirely imperceptible differences where the walls
 * of the inverted octagonal cone intersect the top plane.
 */
public class HorizonRenderer {
	/**
	 * The Y coordinate of the top skybox plane. Acts as the upper bound for the horizon cone, since the cone lies
	 * between the bottom and top skybox planes.
	 */
	private static final float TOP = 16.0F;

	/**
	 * The Y coordinate of the bottom skybox plane. Acts as the lower bound for the horizon cone, since the cone lies
	 * between the bottom and top skybox planes.
	 */
	private static final float BOTTOM = -16.0F;

	private int currentRenderDistance;

	public HorizonRenderer() {
		currentRenderDistance = Minecraft.getMinecraft().gameSettings.renderDistanceChunks;
	}

	private void rebuildBuffer() {
	}

	private void buildHorizon(int radius, BufferBuilder consumer) {
		if (radius > 256) {
			// Prevent the cone from getting too large, this causes issues on some shader packs that modify the vanilla
			// sky if we don't do this.
			radius = 256;
		}

		consumer.pos(0.0F, BOTTOM, 0.0F).endVertex();

		for (int i = 0; i <= 8; i++) {
			double angle = -i * Math.PI / 4.0;
			double x = radius * Math.cos(angle);
			double z = radius * Math.sin(angle);
			consumer.pos(x, TOP, z).endVertex();
		}
	}

	public void renderHorizon(FloatBuffer matrix) {
		if (currentRenderDistance != Minecraft.getMinecraft().gameSettings.renderDistanceChunks) {
			currentRenderDistance = Minecraft.getMinecraft().gameSettings.renderDistanceChunks;
			rebuildBuffer();
		}

        GLStateManager.glPushMatrix();
        GLStateManager.glLoadIdentity();
        GLStateManager.glMultMatrix(matrix);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		GLStateManager.disableCull();
		buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
		buildHorizon(currentRenderDistance * 16, buffer);
		tessellator.draw();

        GLStateManager.glPopMatrix();
	}

	public void destroy() {
	}
}
