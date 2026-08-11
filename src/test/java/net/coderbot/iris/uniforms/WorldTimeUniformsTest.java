package net.coderbot.iris.uniforms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorldTimeUniformsTest {
	@Test
	void worldClockPairStaysContinuousAcrossMidnight() {
		assertEquals(23999, WorldTimeUniforms.getWorldDayTime(23999L));
		assertEquals(0, WorldTimeUniforms.getWorldDay(23999L));
		assertEquals(0, WorldTimeUniforms.getWorldDayTime(24000L));
		assertEquals(1, WorldTimeUniforms.getWorldDay(24000L));
		assertEquals(1, WorldTimeUniforms.getWorldDayTime(24001L));
		assertEquals(1, WorldTimeUniforms.getWorldDay(24001L));
	}

	@Test
	void continuousWorldTimeAdvancesOneTickAcrossMidnight() {
		assertEquals(23999.0F, WorldTimeUniforms.getContinuousWorldTime(23999L), 0.001F);
		assertEquals(24000.0F, WorldTimeUniforms.getContinuousWorldTime(24000L), 0.001F);
		assertEquals(24001.0F, WorldTimeUniforms.getContinuousWorldTime(24001L), 0.001F);
	}

	@Test
	void continuousWorldTimeWrapsAtHundredDayBoundaryLikeShader() {
		long lastTickOfCycle = 99L * 24000L + 23999L;
		long firstTickOfNextCycle = 100L * 24000L;

		assertEquals(2399999.0F, WorldTimeUniforms.getContinuousWorldTime(lastTickOfCycle), 0.001F);
		assertEquals(0.0F, WorldTimeUniforms.getContinuousWorldTime(firstTickOfNextCycle), 0.001F);
	}
}
