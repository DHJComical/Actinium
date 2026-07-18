package net.caffeinemc.mods.sodium.client.gui.widgets;

import java.util.function.IntConsumer;

/** Pure clamped scroll state shared by option, page, and tooltip scrollbars. */
public final class ScrollState {
    private final IntConsumer listener;
    private int visible;
    private int total;
    private int amount;

    public ScrollState(IntConsumer listener) {
        this.listener = listener;
    }

    public void setContext(int visible, int total) {
        if (visible < 0 || total < 0) {
            throw new IllegalArgumentException("Scroll dimensions must not be negative");
        }
        this.visible = visible;
        this.total = total;
        this.scrollTo(this.amount);
    }

    public void scroll(int delta) {
        this.scrollTo(this.amount + delta);
    }

    public void scrollTo(int target) {
        int clamped = Math.max(0, Math.min(Math.max(0, this.total - this.visible), target));
        if (clamped != this.amount) {
            this.amount = clamped;
            if (this.listener != null) {
                this.listener.accept(clamped);
            }
        }
    }

    public boolean canScroll() {
        return this.total > this.visible;
    }

    public int amount() {
        return this.amount;
    }

    public int visible() {
        return this.visible;
    }

    public int total() {
        return this.total;
    }

    public int thumbStart(int trackLength) {
        return this.total == 0 ? 0 : (int) Math.round((double) this.amount / this.total * trackLength);
    }

    public int thumbLength(int trackLength) {
        return this.total == 0 ? trackLength
                : Math.max(4, (int) Math.round((double) this.visible / this.total * trackLength));
    }
}
