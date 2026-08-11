package net.coderbot.iris.uniforms;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.coderbot.iris.uniforms.transforms.SmoothedFloat;
import org.junit.jupiter.api.Test;

class SmoothedWorldTimeTest {
	@Test
	void worldTimeJumpIsSmoothedAcrossFrames() {
		float[] target = {0.0F};
		FrameUpdateNotifier notifier = new FrameUpdateNotifier();
		SmoothedFloat smoothed = new SmoothedFloat(20, 20, () -> target[0], notifier);

		SystemTimeUniforms.TIMER.reset();
		long now = System.nanoTime();
		SystemTimeUniforms.TIMER.beginFrame(now);
		notifier.onNewFrame();

		target[0] = 24000.0F;
		SystemTimeUniforms.TIMER.beginFrame(now + 16_000_000L);
		notifier.onNewFrame();

		float firstSmoothed = smoothed.getAsFloat();
		assertTrue(firstSmoothed > 0.0F, "smoothed value should start moving toward the new target");
		assertTrue(firstSmoothed < 1000.0F, "first frame should not jump to the full target");

		SystemTimeUniforms.TIMER.beginFrame(now + 2_000_000_000L);
		notifier.onNewFrame();
		float twoSecondsLater = smoothed.getAsFloat();
		assertTrue(twoSecondsLater > firstSmoothed, "smoothed value should continue approaching the target");
		assertTrue(twoSecondsLater < 24000.0F, "smoothing should still be asymptotically approaching");
	}
}
