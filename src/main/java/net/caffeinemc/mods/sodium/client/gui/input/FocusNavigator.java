package net.caffeinemc.mods.sodium.client.gui.input;

import java.util.List;
import java.util.Objects;

/** Pure single-focus navigation across dynamically rebuilt GUI targets. */
public final class FocusNavigator {
    private List<? extends FocusTarget> targets = List.of();
    private FocusTarget focused;

    public void replaceTargets(List<? extends FocusTarget> targets) {
        if (targets == null || targets.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Focus targets must not contain null");
        }
        if (this.focused != null && (!targets.contains(this.focused) || !this.focused.isFocusable())) {
            this.focused.setFocused(false);
            this.focused = null;
        }
        this.targets = List.copyOf(targets);
        this.enforceSingleFocus();
    }

    public boolean focus(FocusTarget target) {
        if (target != null && (!this.targets.contains(target) || !target.isFocusable())) {
            return false;
        }
        if (this.focused != null && this.focused != target) {
            this.focused.setFocused(false);
        }
        this.focused = target;
        if (target != null) {
            target.setFocused(true);
        }
        this.enforceSingleFocus();
        return target != null;
    }

    public FocusTarget move(int direction) {
        List<? extends FocusTarget> focusable = this.targets.stream().filter(FocusTarget::isFocusable).toList();
        if (focusable.isEmpty()) {
            this.focus(null);
            return null;
        }
        int current = focusable.indexOf(this.focused);
        int next = current < 0 ? (direction < 0 ? focusable.size() - 1 : 0)
                : Math.floorMod(current + Integer.signum(direction), focusable.size());
        FocusTarget target = focusable.get(next);
        this.focus(target);
        return target;
    }

    public FocusTarget focused() {
        return this.focused;
    }

    private void enforceSingleFocus() {
        for (FocusTarget target : this.targets) {
            target.setFocused(target == this.focused);
        }
    }
}
