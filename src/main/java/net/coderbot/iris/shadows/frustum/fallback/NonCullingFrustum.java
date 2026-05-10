package net.coderbot.iris.shadows.frustum.fallback;

import com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiShadowCullingFrustum;
import com.seibel.distanthorizons.api.objects.math.DhApiMat4f;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.Optional;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.joml.Vector3d;

@Optional.Interface(modid = "distanthorizons", iface = "com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiShadowCullingFrustum")
public class NonCullingFrustum extends Frustum implements ViewportProvider, org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum, IDhApiShadowCullingFrustum {
	private final Vector3d position = new Vector3d();
	private double x;
	private double y;
	private double z;

	@Override
	public void setPosition(double cameraX, double cameraY, double cameraZ) {
		super.setPosition(cameraX, cameraY, cameraZ);
		this.x = cameraX;
		this.y = cameraY;
		this.z = cameraZ;
	}

	@Override
	public boolean isBoundingBoxInFrustum(AxisAlignedBB aabb) {
		return true;
	}

	@Override
	public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		return true;
	}

	@Override
	public Viewport sodium$createViewport() {
		return new Viewport(this, position.set(x, y, z));
	}

	@Optional.Method(modid = "distanthorizons")
	@Override
	public void update(int worldMinBlockY, int worldMaxBlockY, DhApiMat4f worldViewProjection) {

	}

	@Optional.Method(modid = "distanthorizons")
	@Override
	public boolean intersects(int lodBlockPosMinX, int lodBlockPosMinZ, int lodBlockWidth, int lodDetailLevel) {
		return true;
	}

}
