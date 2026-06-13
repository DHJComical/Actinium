package me.decce.gnetum.util;

import net.minecraft.client.resources.I18n;

public class TriStateBoolean implements AnyBoolean {
    public AnyBooleanValue value;

    public transient boolean defaultValue;

    public TriStateBoolean() {
        this(AnyBooleanValue.AUTO);
    }

    public TriStateBoolean(AnyBooleanValue value) {
        this(value, true);
    }

    public TriStateBoolean(AnyBooleanValue value, boolean defaultValue) {
        this.value = value;
        this.defaultValue = defaultValue;
    }

    public boolean get() {
        return value == AnyBooleanValue.AUTO ? defaultValue : value == AnyBooleanValue.ON;
    }

    @Override
    public void next() {
        this.value = switch (this.value) {
            case AUTO -> AnyBooleanValue.ON;
            case ON -> AnyBooleanValue.OFF;
            case OFF -> AnyBooleanValue.AUTO;
        };
    }

    @Override
    public String text() {
        return I18n.format((switch (this.value) {
            case AUTO -> "gnetum.config.auto";
            case OFF -> "options.off";
            case ON -> "options.on";
        }));
    }

    public String effectiveText() {
        return I18n.format(this.get() ? "options.on" : "options.off");
    }
}
