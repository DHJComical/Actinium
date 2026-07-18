package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.gui.GuiRect;

import java.util.ArrayList;
import java.util.List;

/** Frame-local hit regions which deliberately forget controls no longer visible after scrolling. */
public final class VisibleHitMap<T> {
    private final List<Entry<T>> entries = new ArrayList<>();

    public void beginFrame() {
        this.entries.clear();
    }

    public void add(T target, GuiRect bounds) {
        if (target == null || bounds == null || bounds.width() == 0 || bounds.height() == 0) {
            throw new IllegalArgumentException("Visible hit region must have a target and positive bounds");
        }
        this.entries.add(new Entry<>(target, bounds));
    }

    public T find(int mouseX, int mouseY) {
        for (Entry<T> entry : this.entries) {
            if (entry.bounds.contains(mouseX, mouseY)) {
                return entry.target;
            }
        }
        return null;
    }

    public List<T> targets() {
        return this.entries.stream().map(Entry::target).toList();
    }

    private record Entry<T>(T target, GuiRect bounds) {
    }
}
