package net.coderbot.iris.shadows.frustum.advanced;

import net.coderbot.iris.shadows.frustum.BoxCuller;
import net.minecraft.util.math.AxisAlignedBB;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public class SafeZoneCullingFrustum extends AdvancedShadowCullingFrustum {
	private final BoxCuller distanceCuller;

	public SafeZoneCullingFrustum(Matrix4fc playerView, Matrix4fc playerProjection, Vector3f shadowLightVector, BoxCuller voxelCuller, BoxCuller distanceCuller) {
		super();
		init(playerView, playerProjection, shadowLightVector, voxelCuller);
		this.distanceCuller = distanceCuller;
	}

	@Override
	public boolean supportsOcclusionSearch() {
		// The safe-zone culler keeps everything within the voxel distance visible regardless of the light path,
		// so the receiver-driven search would incorrectly drop sections that are inside the safe zone.
		return false;
	}

	@Override
	public void setPosition(double cameraX, double cameraY, double cameraZ) {
		if (this.distanceCuller != null) {
			this.distanceCuller.setPosition(cameraX, cameraY, cameraZ);
		}
		super.setPosition(cameraX, cameraY, cameraZ);
	}

	@Override
	public boolean isBoundingBoxInFrustum(AxisAlignedBB aabb) {
		// Cull if outside the overall distance limit
		if (distanceCuller != null && distanceCuller.isCulled(aabb)) {
			return false;
		}

		// If within the voxel safe zone, always render
		if (boxCuller != null && !boxCuller.isCulled(aabb)) {
			return true;
		}

		// Otherwise fall through to advanced frustum culling
		return isVisible(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
	}

	@Override
	public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		// Cull if outside the overall distance limit
		if (distanceCuller != null && distanceCuller.isCulledViewRelative(minX, minY, minZ, maxX, maxY, maxZ)) {
			return false;
		}

		// If within the voxel safe zone, always render
		if (boxCuller != null && !boxCuller.isCulledViewRelative(minX, minY, minZ, maxX, maxY, maxZ)) {
			return true;
		}

		return checkCornerVisibility(minX, minY, minZ, maxX, maxY, maxZ);
	}

	/** Combines the safe voxel zone, outer distance limit, and advanced clipping planes. */
	@Override
	public int intersectAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		if (distanceCuller != null && distanceCuller.isCulledViewRelative(minX, minY, minZ, maxX, maxY, maxZ)) {
			return OUTSIDE;
		}

		if (boxCuller != null && !boxCuller.isCulledViewRelative(minX, minY, minZ, maxX, maxY, maxZ)) {
			boolean fullyInside = boxCuller.isFullyInsideSodium(minX, minY, minZ, maxX, maxY, maxZ)
					&& (distanceCuller == null || distanceCuller.isFullyInsideSodium(minX, minY, minZ, maxX, maxY, maxZ));
			return fullyInside ? FULLY_INSIDE : PARTIALLY_INSIDE;
		}

		int result = intersectCorners(minX, minY, minZ, maxX, maxY, maxZ);
		if (result == FULLY_INSIDE && distanceCuller != null
				&& !distanceCuller.isFullyInsideSodium(minX, minY, minZ, maxX, maxY, maxZ)) {
			return PARTIALLY_INSIDE;
		}

		return result;
	}
}
