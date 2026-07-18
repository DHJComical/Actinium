package net.caffeinemc.mods.sodium.client.gui.options.control;

/** Pure keyboard and pointer value mapping shared by stateful controls. */
public final class ControlMath {
    private ControlMath() {
    }

    public static int sliderValue(int min, int max, int step, int pointerOffset, int trackWidth) {
        if (max < min || step <= 0 || (max - min) % step != 0 || trackWidth <= 0) {
            throw new IllegalArgumentException("Invalid stepped slider geometry");
        }
        double progress = Math.max(0.0D, Math.min(1.0D, (double) pointerOffset / trackWidth));
        int steps = (max - min) / step;
        return min + (int) Math.round(progress * steps) * step;
    }

    public static int cycleIndex(int size, int current, int direction) {
        if (size <= 0 || current < 0 || current >= size) {
            throw new IllegalArgumentException("Invalid cycling control state");
        }
        return Math.floorMod(current + direction, size);
    }
}
