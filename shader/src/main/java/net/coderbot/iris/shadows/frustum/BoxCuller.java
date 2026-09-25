package net.coderbot.iris.shadows.frustum;


import lombok.Setter;
import net.minecraft.util.math.AxisAlignedBB;

public class BoxCuller {
	@Setter
    private double maxDistance;

	private double minAllowedX;
	private double maxAllowedX;
	private double minAllowedY;
	private double maxAllowedY;
	private double minAllowedZ;
	private double maxAllowedZ;

	public BoxCuller(double maxDistance) {
		this.maxDistance = maxDistance;
	}

    public void setPosition(double cameraX, double cameraY, double cameraZ) {
		this.minAllowedX = cameraX - maxDistance;
		this.maxAllowedX = cameraX + maxDistance;
		this.minAllowedY = cameraY - maxDistance;
		this.maxAllowedY = cameraY + maxDistance;
		this.minAllowedZ = cameraZ - maxDistance;
		this.maxAllowedZ = cameraZ + maxDistance;
	}

	public boolean isCulled(AxisAlignedBB aabb) {
		return isCulled((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ,
				(float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ);
	}

	public boolean isCulled(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		if (maxX < this.minAllowedX || minX > this.maxAllowedX) {
			return true;
		}

		if (maxY < this.minAllowedY || minY > this.maxAllowedY) {
			return true;
		}

		return maxZ < this.minAllowedZ || minZ > this.maxAllowedZ;
	}

	// View-relative coordinates version
	public boolean isCulledViewRelative(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		if (maxX < -this.maxDistance || minX > this.maxDistance) {
			return true;
		}

		if (maxY < -this.maxDistance || minY > this.maxDistance) {
			return true;
		}

		return maxZ < -this.maxDistance || minZ > this.maxDistance;
	}

	/** Returns whether every point of a view-relative box remains within this culler's distance bounds. */
	public boolean isFullyInsideSodium(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return minX >= -this.maxDistance && maxX <= this.maxDistance
				&& minY >= -this.maxDistance && maxY <= this.maxDistance
				&& minZ >= -this.maxDistance && maxZ <= this.maxDistance;
	}
}
