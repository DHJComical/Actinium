package com.dhj.actinium.shader.pack;

import java.util.Objects;

public final class ActiniumNamespacedId {
    private final String namespace;
    private final String name;

    public ActiniumNamespacedId(String combined) {
        int separator = combined.indexOf(':');
        if (separator < 0) {
            this.namespace = "minecraft";
            this.name = combined;
        } else {
            this.namespace = combined.substring(0, separator);
            this.name = combined.substring(separator + 1);
        }
    }

    public ActiniumNamespacedId(String namespace, String name) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.name = Objects.requireNonNull(name, "name");
    }

    public String namespace() {
        return this.namespace;
    }

    public String name() {
        return this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ActiniumNamespacedId other)) {
            return false;
        }

        return this.namespace.equals(other.namespace) && this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.namespace, this.name);
    }
}
