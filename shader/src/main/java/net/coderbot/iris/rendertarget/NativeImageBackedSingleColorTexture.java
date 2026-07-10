package net.coderbot.iris.rendertarget;

import com.gtnewhorizons.angelica.compat.mojang.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

public class NativeImageBackedSingleColorTexture extends DynamicTexture {
	public NativeImageBackedSingleColorTexture(int red, int green, int blue, int alpha) {
		super(create(red, green, blue, alpha));
	}

	public NativeImageBackedSingleColorTexture(int rgba) {
		this(rgba >> 24 & 0xFF, rgba >> 16 & 0xFF, rgba >> 8 & 0xFF, rgba & 0xFF);
	}

	static NativeImage create(int red, int green, int blue, int alpha) {
		NativeImage image = new NativeImage(NativeImage.Format.RGBA, 1, 1, false);

		// This compatibility NativeImage is backed by BufferedImage, whose pixel API uses ARGB.
		image.setPixelRGBA(0, 0, NativeImage.combine(alpha, red, green, blue));

		return image;
	}
}
