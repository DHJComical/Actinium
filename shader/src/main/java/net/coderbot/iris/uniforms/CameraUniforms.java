package net.coderbot.iris.uniforms;

import com.gtnewhorizons.angelica.rendering.RenderingState;
import lombok.Getter;
import net.coderbot.iris.compat.dh.DHCompat;
import net.coderbot.iris.debug.IrisDebugOptions;
import net.coderbot.iris.gl.uniform.UniformHolder;
import net.coderbot.iris.pipeline.SkyRenderDistance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import dhj.embeddedt.embeddium.impl.render.chunk.map.ChunkTrackerHolder;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import static net.coderbot.iris.gl.uniform.UniformUpdateFrequency.ONCE;
import static net.coderbot.iris.gl.uniform.UniformUpdateFrequency.PER_FRAME;

/**
 * @see <a href="https://github.com/IrisShaders/ShaderDoc/blob/master/uniforms.md#camera">Uniforms: Camera</a>
 */
public class CameraUniforms {
	private static final Minecraft client = Minecraft.getMinecraft();
	private static final Vector3f tempVec3f = new Vector3f();
	private static final Vector3i tempVec3i = new Vector3i();

	private CameraUniforms() {
	}

	public static void addCameraUniforms(UniformHolder uniforms, FrameUpdateNotifier notifier) {
		final CameraPositionTracker tracker = new CameraPositionTracker(notifier);

		uniforms
			.uniform1f(ONCE, "near", () -> 0.05)
			.uniform1f(PER_FRAME, "far", CameraUniforms::getRenderDistanceInBlocks)
			.uniform3d(PER_FRAME, "cameraPosition", tracker::getCurrentCameraPosition)
			.uniform1f(PER_FRAME, "eyeAltitude", tracker::getCurrentCameraPositionY)
			.uniform3d(PER_FRAME, "previousCameraPosition", tracker::getPreviousCameraPosition)
			.uniform3i(PER_FRAME, "cameraPositionInt", () -> getCameraPositionInt(getUnshiftedCameraPosition()))
			.uniform3f(PER_FRAME, "cameraPositionFract", () -> getCameraPositionFract(getUnshiftedCameraPosition()))
			.uniform3i(PER_FRAME, "previousCameraPositionInt", () -> getCameraPositionInt(tracker.getPreviousCameraPositionUnshifted()))
			.uniform3f(PER_FRAME, "previousCameraPositionFract", () -> getCameraPositionFract(tracker.getPreviousCameraPositionUnshifted()));
	}

	public static Vector3fc getCameraPositionFract(Vector3d originalPos) {
		return tempVec3f.set(
			(float) (originalPos.x - Math.floor(originalPos.x)),
			(float) (originalPos.y - Math.floor(originalPos.y)),
			(float) (originalPos.z - Math.floor(originalPos.z))
		);
	}

	public static Vector3ic getCameraPositionInt(Vector3d originalPos) {
		return tempVec3i.set(
			(int) Math.floor(originalPos.x),
			(int) Math.floor(originalPos.y),
			(int) Math.floor(originalPos.z)
		);
	}

	private static int getRenderDistanceInBlocks() {
		final int renderDistanceChunks = client.gameSettings.renderDistanceChunks;

		if (!usesDistantHorizonsTerrain()) {
			return SkyRenderDistance.effectiveBlocks(renderDistanceChunks);
		}

		return SkyRenderDistance.farBlocks(renderDistanceChunks, getUnrenderedOuterRings());
	}

	/**
	 * Returns whether Celeritas renders the terrain while Distant Horizons supplies the distant LODs.
	 *
	 * <p>That combination is what makes shader packs place the vanilla-to-LOD transition from {@code far}. Without it
	 * the reported distance keeps the sky rendering floor, which the skybox, cloud, and celestial geometry need at low
	 * render distances.</p>
	 */
	private static boolean usesDistantHorizonsTerrain() {
		return DHCompat.isDistantHorizonsLoaded() && IrisDebugOptions.enableCeleritas();
	}

	/**
	 * Returns how many of the outermost chunk rings inside the configured render distance are loaded but never
	 * rendered.
	 *
	 * <p>Celeritas publishes a chunk as ready only after the chunk rings configured on its neighbor gate are loaded.
	 * The chunks beyond the configured render distance are not loaded, so that gate can never be satisfied on the
	 * outermost rings and nothing is rendered there.</p>
	 */
	private static int getUnrenderedOuterRings() {
		if (!usesDistantHorizonsTerrain()) {
			return 0;
		}

		WorldClient world = client.world;
		if (world == null) {
			return 0;
		}

		return ChunkTrackerHolder.get(world).getRequiredNeighborRadius();
	}

	public static Vector3d getUnshiftedCameraPosition() {
        return RenderingState.INSTANCE.getCameraPosition();
	}

	static class CameraPositionTracker {
		/**
		 * Value range of cameraPosition. We want this to be small enough that precision is maintained when we convert
		 * from a double to a float, but big enough that shifts happen infrequently, since each shift corresponds with
		 * a noticeable change in shader animations and similar. 1000024 is the number used by Optifine for walking (however this is too much, so we choose 30000),
		 * with an extra 1024 check for if the user has teleported between camera positions.
		 */
		private static final double WALK_RANGE = 30000;
		private static final double TP_RANGE = 1000;

		@Getter
		private Vector3d previousCameraPosition = new Vector3d();
		@Getter
		private Vector3d currentCameraPosition = new Vector3d();
		private final Vector3d shift = new Vector3d();
		private Vector3d previousCameraPositionUnshifted = new Vector3d();
		private Vector3d currentCameraPositionUnshifted = new Vector3d();

		CameraPositionTracker(FrameUpdateNotifier notifier) {
			notifier.addListener(this::update);
		}

		private void update() {
			previousCameraPosition.set(currentCameraPosition);
			previousCameraPositionUnshifted.set(currentCameraPositionUnshifted);
			currentCameraPosition.set(getUnshiftedCameraPosition()).add(shift);
			currentCameraPositionUnshifted.set(getUnshiftedCameraPosition());

			updateShift();
		}

		/**
		 * Updates our shift values to try to keep |x| < 30000 and |z| < 30000, to maintain precision with cameraPosition.
		 * Since our actual range is 60000x60000, this means that we won't excessively move back and forth when moving
		 * around a chunk border.
		 */
		private void updateShift() {
			final double dX = getShift(currentCameraPosition.x, previousCameraPosition.x);
			final double dZ = getShift(currentCameraPosition.z, previousCameraPosition.z);

			if (dX != 0.0 || dZ != 0.0) {
				applyShift(dX, dZ);
			}
		}

		private static double getShift(double value, double prevValue) {
			if (Math.abs(value) > WALK_RANGE || Math.abs(value - prevValue) > TP_RANGE) {
				// Only shift by increments of WALK_RANGE - this is required for some packs (like SEUS PTGI) to work properly
				return -(value - (value % WALK_RANGE));
			} else {
				return 0.0;
			}
		}

		/**
		 * Shifts all current and future positions by the given amount. This is done in such a way that the difference
		 * between cameraPosition and previousCameraPosition remains the same, since they are completely arbitrary
		 * to the shader for the most part.
		 */
		private void applyShift(double dX, double dZ) {
			shift.x += dX;
			currentCameraPosition.x += dX;
			previousCameraPosition.x += dX;

			shift.z += dZ;
			currentCameraPosition.z += dZ;
			previousCameraPosition.z += dZ;
		}

		public double getCurrentCameraPositionY() {
			return currentCameraPosition.y;
		}

		public Vector3d getPreviousCameraPositionUnshifted() {
			return previousCameraPositionUnshifted;
		}
	}
}
