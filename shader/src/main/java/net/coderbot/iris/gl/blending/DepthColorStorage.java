package net.coderbot.iris.gl.blending;

import com.gtnewhorizons.angelica.glsm.states.DepthState;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.Getter;

public class DepthColorStorage {
	private static boolean originalDepthEnable;
	private static ColorMask originalColor;
	@Getter
    private static boolean depthColorLocked;

	private static final IntOpenHashSet ownedPrograms = new IntOpenHashSet();

	public static void registerOwnedProgram(int programId) {
		ownedPrograms.add(programId);
	}

	public static void unregisterOwnedProgram(int programId) {
		ownedPrograms.remove(programId);
	}

	public static boolean isOwnedProgram(int programId) {
		return ownedPrograms.contains(programId);
	}

    public static void disableDepthColor() {
		if (!depthColorLocked) {
			// Only save the previous state if the depth and color mask wasn't already locked
			final var colorMask = GLStateManager.getColorMask();
			final DepthState depthState = GLStateManager.getDepthState();

            originalDepthEnable = depthState.isMaskEnabled();
			originalColor = new ColorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha);
		}

		depthColorLocked = false;

		GLStateManager.glDepthMask(false);
        GLStateManager.glColorMask(false, false, false, false);

		depthColorLocked = true;
	}

	public static void deferDepthEnable(boolean enabled) {
		originalDepthEnable = enabled;
	}

	public static void deferColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		originalColor = new ColorMask(red, green, blue, alpha);
	}

	public static void unlockDepthColor() {
		if (!depthColorLocked) {
			return;
		}

		depthColorLocked = false;

        GLStateManager.glDepthMask(originalDepthEnable);

        GLStateManager.glColorMask(originalColor.isRedMasked(), originalColor.isGreenMasked(), originalColor.isBlueMasked(), originalColor.isAlphaMasked());
	}
}
