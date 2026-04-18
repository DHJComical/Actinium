package com.dhj.actinium.shader.uniform;

import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class ActiniumCommonUniforms {
    private ActiniumCommonUniforms() {
    }

    public static int getWorldTime(@Nullable World world) {
        if (world == null) {
            return 0;
        }

        return (int) (world.getWorldTime() % 24000L);
    }

    public static float getDayMoment(int worldTime) {
        return worldTime / 24000.0f;
    }

    public static float getDayMixer(float dayMoment) {
        float moment = dayMoment - 0.25f;
        return clamp(-(moment * moment) * 20.0f + 1.25f, 0.0f, 1.0f);
    }

    public static float getNightMixer(float dayMoment) {
        float moment = dayMoment - 0.75f;
        return clamp(-(moment * moment) * 50.0f + 3.125f, 0.0f, 1.0f);
    }

    public static float getVolumetricDayMixer(float dayMoment) {
        float daySample = dayMoment * 4.0f - 1.0f;
        float nightSample = dayMoment * 4.0f - 3.0f;
        float dayMixer = clamp((-(pow4(daySample)) + 1.0f) * 7.0f + 1.0f, 1.0f, 8.0f);
        float nightMixer = clamp((-(pow4(nightSample)) + 1.0f) * 7.0f + 1.0f, 1.0f, 8.0f);
        return Math.max(dayMixer, nightMixer);
    }

    public static float getDayNightMix(int worldTime) {
        float a = ((worldTime >= 0 && worldTime < 12485) || worldTime >= 23515) ? 1.0f : 0.0f;
        float b = worldTime >= 12485 && worldTime < 13085 ? 1.0f - ((worldTime - 12485) / 600.0f) : 0.0f;
        float d = worldTime >= 22915 && worldTime < 23515 ? (worldTime - 22915) / 600.0f : 0.0f;
        return Math.max(a, Math.max(b, d));
    }

    public static int getMoonPhase(@Nullable World world) {
        return world != null ? world.getMoonPhase() : 0;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float pow4(float value) {
        float squared = value * value;
        return squared * squared;
    }
}
