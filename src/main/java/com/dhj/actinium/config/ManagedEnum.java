package com.dhj.actinium.config;

public final class ManagedEnum<T extends Enum<?>> {
    private final T[] values;
    private T value;

    public ManagedEnum(T initialValue) {
        if (initialValue == null) {
            throw new IllegalArgumentException("initialValue");
        }
        this.value = initialValue;
        @SuppressWarnings("unchecked")
        T[] enumValues = (T[]) initialValue.getClass().getEnumConstants();
        this.values = enumValues;
    }

    public boolean is(T value) {
        return this.value == value;
    }

    public T next() {
        this.value = this.values[(this.value.ordinal() + 1) % this.values.length];
        return this.value;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }
}
