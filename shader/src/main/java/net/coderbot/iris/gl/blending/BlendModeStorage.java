package net.coderbot.iris.gl.blending;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.RenderSystem;
import com.gtnewhorizons.angelica.glsm.states.BlendState;
import lombok.Getter;

import java.util.Objects;

public class BlendModeStorage {
	private static boolean originalBlendEnable;
	private static final BlendState originalBlend = new BlendState();
	@Getter private static boolean blendLocked;
	@Getter private static boolean hasDeferredChanges;
	private static final BlendStateAccess DEFAULT_STATE_ACCESS = new BlendStateAccessImpl();
	private static BlendStateAccess stateAccess = DEFAULT_STATE_ACCESS;

    public static void overrideBlend(BlendState override) {
		if (!blendLocked) {
			// A pending unlocked change already describes the state that must be restored.
			if (!hasDeferredChanges) {
				saveCurrentBlend();
			}
		}

		blendLocked = false;

		if (override == null) {
			stateAccess.setBlendEnabled(false);
		} else {
			stateAccess.setBlendEnabled(true);
			stateAccess.setBlendFunction(override);
		}

		blendLocked = true;
	}

	public static void overrideBufferBlend(int index, BlendState override) {
		if (!blendLocked) {
			if (!hasDeferredChanges) {
				saveCurrentBlend();
			}
		}

		if (override == null) {
			stateAccess.setBufferBlendEnabled(index, false);
		} else {
			stateAccess.setBufferBlendEnabled(index, true);
			stateAccess.setBufferBlendFunction(index, override);
		}

		blendLocked = true;
	}

	public static void deferBlendModeToggle(boolean enabled) {
		prepareDeferredState();
		originalBlendEnable = enabled;
		hasDeferredChanges = true;
	}

	public static void deferBlendFunc(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
		prepareDeferredState();
        originalBlend.setAll(srcRgb, dstRgb, srcAlpha, dstAlpha);
		hasDeferredChanges = true;
	}

	public static void flushDeferredBlend() {
		if (!hasDeferredChanges || blendLocked) {
			return;
		}

		applyOriginalBlend();
		hasDeferredChanges = false;
	}

	public static void restoreBlend() {
		if (!blendLocked) {
			return;
		}

		blendLocked = false;
		applyOriginalBlend();
		hasDeferredChanges = false;
	}

	private static void prepareDeferredState() {
		if (!blendLocked && !hasDeferredChanges) {
			saveCurrentBlend();
		}
	}

	private static void saveCurrentBlend() {
		originalBlendEnable = stateAccess.isBlendEnabled();
		stateAccess.copyBlendState(originalBlend);
	}

	private static void applyOriginalBlend() {
		stateAccess.setBlendEnabled(originalBlendEnable);
		stateAccess.setBlendFunction(originalBlend);
	}

	static void setBlendStateAccessForTesting(BlendStateAccess access) {
		stateAccess = Objects.requireNonNull(access, "access");
		resetStorageState();
		saveCurrentBlend();
	}

	static void restoreDefaultBlendStateAccessForTesting() {
		stateAccess = DEFAULT_STATE_ACCESS;
		resetStorageState();
	}

	private static void resetStorageState() {
		blendLocked = false;
		hasDeferredChanges = false;
	}

	/**
	 * Provides the concrete blend state operations used by the override state machine.
	 */
	interface BlendStateAccess {
		/** Returns the actual global blend enable state. */
		boolean isBlendEnabled();

		/** Copies the actual global blend function into {@code destination}. */
		void copyBlendState(BlendState destination);

		/** Applies the actual global blend enable state. */
		void setBlendEnabled(boolean enabled);

		/** Applies the actual global blend function. */
		void setBlendFunction(BlendState state);

		/** Applies an indexed blend enable override. */
		void setBufferBlendEnabled(int index, boolean enabled);

		/** Applies an indexed blend function override. */
		void setBufferBlendFunction(int index, BlendState state);
	}

	private static final class BlendStateAccessImpl implements BlendStateAccess {
		@Override
		public boolean isBlendEnabled() {
			return GLStateManager.getBlendMode().isEnabled();
		}

		@Override
		public void copyBlendState(BlendState destination) {
			destination.set(GLStateManager.getBlendState());
		}

		@Override
		public void setBlendEnabled(boolean enabled) {
			if (enabled) {
				GLStateManager.enableBlend();
			} else {
				GLStateManager.disableBlend();
			}
		}

		@Override
		public void setBlendFunction(BlendState state) {
			GLStateManager.tryBlendFuncSeparate(
				state.getSrcRgb(), state.getDstRgb(), state.getSrcAlpha(), state.getDstAlpha()
			);
		}

		@Override
		public void setBufferBlendEnabled(int index, boolean enabled) {
			if (enabled) {
				RenderSystem.enableBufferBlend(index);
			} else {
				RenderSystem.disableBufferBlend(index);
			}
		}

		@Override
		public void setBufferBlendFunction(int index, BlendState state) {
			RenderSystem.blendFuncSeparatei(
				index, state.getSrcRgb(), state.getDstRgb(), state.getSrcAlpha(), state.getDstAlpha()
			);
		}
	}
}
